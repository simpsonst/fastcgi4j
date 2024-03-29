/*
 * Copyright (c) 2022,2023, Lancaster University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the
 *   distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 *  Author: Steven Simpson <s.simpson@lancaster.ac.uk>
 */

package uk.ac.lancs.fastcgi.proto.serial;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;
import uk.ac.lancs.fastcgi.proto.RecordTypes;
import uk.ac.lancs.fastcgi.proto.RequestFlags;
import uk.ac.lancs.fastcgi.proto.RoleTypes;

/**
 * Deserializes FastCGI records.
 *
 * @author simpsons
 */
public class RecordReader {
    private final InputStream in;

    private final Charset charset;

    private final RecordHandler handler;

    private final String tag;

    /**
     * Prepare to read records from a stream.
     * 
     * @param in the stream of serialized records
     * 
     * @param charset the encoding to expect for name/value pairs
     * 
     * @param handler a destination for deserialized records
     * 
     * @param tag a tag to include in logging messages
     */
    public RecordReader(InputStream in, Charset charset, RecordHandler handler,
                        String tag) {
        this.in = in;
        this.charset = charset;
        this.handler = handler;
        this.tag = tag;
    }

    private byte[] buf = new byte[8];

    /**
     * Expect a specific number of bytes, and load them into the buffer.
     * Wait until all bytes have been loaded, or end-of-file.
     * 
     * @param log a token to appear in logging
     * 
     * @param exp the expected number of bytes
     * 
     * @return {@code true} if the expected number of bytes was read;
     * {@code false} otherwise
     * 
     * @throws IOException if an I/O error occurred
     */
    private boolean require(Supplier<String> log, int exp) throws IOException {
        return require(log, exp, true);
    }

    /**
     * Expect a specific number of bytes, and load them into the buffer.
     * Wait until all bytes have been loaded, or end-of-file.
     * 
     * @param log a token to appear in logging
     * 
     * @param exp the expected number of bytes
     * 
     * @param eofBad {@code true} if EOF occurring after reading at
     * least one byte should be logged
     * 
     * @return {@code true} if the expected number of bytes was read;
     * {@code false} otherwise
     * 
     * @throws IOException if an I/O error occurred
     */
    private boolean require(Supplier<String> log, int exp, boolean eofBad)
        throws IOException {
        if (exp > buf.length) buf = new byte[exp];
        int len = 0;
        while (len < exp) {
            int got = in.read(buf, len, exp - len);
            if (got < 0) {
                if (len > 0 || eofBad) {
                    final int flen = len;
                    logger.warning(() -> msg(
                                             "expected %d bytes for %s;"
                                                 + " got %d before EOF",
                                             exp, log.get(), flen));
                }
                return false;
            }
            len += got;
        }
        return true;
    }

    private void skip(int amount) throws IOException {
        final int oa = amount;
        while (amount > 0) {
            /* We can cast the result to int because we only supply an
             * int. */
            int got = (int) in.skip(amount);
            if (got == 0) {
                /* Detect EOF by reading one byte. */
                assert amount > 0;
                int rc = in.read();
                if (rc < 0) {
                    final int na = amount;
                    logger.warning(() -> msg("EOF skipping last %d of %d", na,
                                             oa));
                    return;
                }
                got = 1;
            }
            amount -= got;
        }
    }

    /**
     * Read and process at most one record. If EOF is encountered before
     * reading a complete record, {@code false} is returned.
     * 
     * @return {@code true} if an attempt should be made to read another
     * record; {@code false} otherwise
     * 
     * @throws IOException if an I/O error occurred
     */
    public boolean processRecord() throws IOException {
        /* Read in the header. */
        logger.fine(() -> msg("awaiting record"));
        if (!require(() -> "hdr", 8, false)) return false;
        final int rver = unser(buf, 0, 1);
        final int rtype = unser(buf, 1, 1);
        final int rid = unser(buf, 2, 2);
        final int iclen = unser(buf, 4, 2);
        final int iplen = unser(buf, 6, 1);
        logger.finer(() -> msg("ver=%d type=%s rid=%d clen=%d plen=%d", rver,
                               RecordTypes.toString(rtype), rid, iclen, iplen));
        int clen = iclen, plen = iplen;
        int reasons = 0;
        switch (rtype) {
        case RecordTypes.ABORT_REQUEST -> {
            if (rver < 1) reasons |= RecordHandler.BAD_VERSION;
            if (clen != 0) reasons |= RecordHandler.BAD_LENGTH;
            if (rid == 0) reasons |= RecordHandler.BAD_REQ_ID;
            if (reasons != 0) {
                skip(clen);
                rejectMessage(rver, rtype, rid, clen, plen, reasons);
                handler.bad(reasons, rver, rtype, clen, rid);
                break;
            }
            logger.fine(() -> msg("ABORT_REQUEST(%d)", rid));
            handler.abortRequest(rid);
        }

        case RecordTypes.BEGIN_REQUEST -> {
            if (rver < 1) reasons |= RecordHandler.BAD_VERSION;
            if (clen != 8) reasons |= RecordHandler.BAD_LENGTH;
            if (rid == 0) reasons |= RecordHandler.BAD_REQ_ID;
            if (reasons != 0) {
                skip(clen);
                rejectMessage(rver, rtype, rid, clen, plen, reasons);
                handler.bad(reasons, rver, rtype, clen, rid);
                break;
            }
            if (!require(() -> msg("begin-request"), clen)) return false;
            int role = unser(buf, 0, 2);
            int flags = unser(buf, 2, 1);
            logger.fine(() -> msg("BEGIN_REQUEST(%d, %s)%s", rid,
                                  RoleTypes.toString(role),
                                  RequestFlags.toString(flags)));
            handler.beginRequest(rid, role, flags);
        }

        case RecordTypes.GET_VALUES -> {
            if (rver < 1) reasons |= RecordHandler.BAD_VERSION;
            if (rid == 0) reasons |= RecordHandler.BAD_REQ_ID;
            if (reasons != 0) {
                skip(clen);
                rejectMessage(rver, rtype, rid, clen, plen, reasons);
                handler.bad(reasons, rver, rtype, clen, rid);
                break;
            }
            Map<String, String> vars = new HashMap<>();
            while (clen > 0) {
                if (!require(() -> "gvs-namelen", 1)) return false;
                int nlen = unser(buf, 0, 1);
                clen--;
                if (nlen > 127) {
                    if (clen < 3) break;
                    if (!require(() -> "gvs-namelen", 3)) return false;
                    clen -= 3;
                    nlen = unser(nlen & 0x7f, buf, 0, 3);
                }
                final int fnlen = nlen;
                logger.finest(() -> msg("namelen=%d", fnlen));

                if (clen < 1) break;
                if (!require(() -> "gvs-valuelen", 1)) return false;
                int vlen = unser(buf, 0, 1);
                clen--;
                if (vlen > 127) {
                    if (clen < 3) break;
                    if (!require(() -> "gvs-valuelen", 3)) return false;
                    clen -= 3;
                    vlen = unser(vlen & 0x7f, buf, 0, 3);
                }
                logger.finest(() -> msg("valuelen=%d", fnlen));

                if (clen < nlen) break;
                if (!require(() -> "gvs-name", nlen)) return false;
                clen -= nlen;
                String name = new String(buf, 0, nlen, charset);
                logger.finest(() -> msg("name=%s", fnlen));

                if (clen < vlen) break;
                if (!require(() -> "gvs-value", vlen)) return false;
                clen -= vlen;
                String value = new String(buf, 0, vlen, charset);
                logger.finest(() -> msg("value=%s", fnlen));

                logger.finer(() -> msg("VALUE[%s]=%s", name, value));
                vars.put(name, value);
            }
            if (clen > 0) {
                final int fclen = clen;
                logger.finest(() -> msg("excess content %d added to padding",
                                        fclen));
                plen += clen;
            }
            logger.fine(() -> msg("GET_VALUES(%s)", vars.keySet()));
            handler.getValues(vars.keySet());
        }

        case RecordTypes.PARAMS -> {
            if (rver < 1) reasons |= RecordHandler.BAD_VERSION;
            if (rid == 0) reasons |= RecordHandler.BAD_REQ_ID;
            if (reasons != 0) {
                skip(clen);
                rejectMessage(rver, rtype, rid, clen, plen, reasons);
                handler.bad(reasons, rver, rtype, clen, rid);
            } else if (clen == 0) {
                logger.fine(() -> msg("PARAMS(%d) end", rid));
                handler.paramsEnd(rid);
            } else {
                FixedLengthInputStream out =
                    new FixedLengthInputStream(clen, in);
                final int fclen = clen;
                logger.fine(() -> msg("PARAMS(%d, %d)", rid, fclen));
                handler.params(rid, clen, out);
                out.skipRemaining();
            }
        }

        case RecordTypes.STDIN -> {
            if (rver < 1) reasons |= RecordHandler.BAD_VERSION;
            if (rid == 0) reasons |= RecordHandler.BAD_REQ_ID;
            if (reasons != 0) {
                skip(clen);
                rejectMessage(rver, rtype, rid, clen, plen, reasons);
                handler.bad(reasons, rver, rtype, clen, rid);
            } else if (clen == 0) {
                logger.fine(() -> msg("STDIN(%d) end", rid));
                handler.stdinEnd(rid);
            } else {
                FixedLengthInputStream out =
                    new FixedLengthInputStream(clen, in);
                final int fclen = clen;
                logger.fine(() -> msg("STDIN(%d, %d)", rid, fclen));
                handler.stdin(rid, clen, out);
                out.skipRemaining();
            }
        }

        case RecordTypes.DATA -> {
            if (rver < 1) reasons |= RecordHandler.BAD_VERSION;
            if (rid == 0) reasons |= RecordHandler.BAD_REQ_ID;
            if (reasons != 0) {
                skip(clen);
                rejectMessage(rver, rtype, rid, clen, plen, reasons);
                handler.bad(reasons, rver, rtype, clen, rid);
            } else if (clen == 0) {
                logger.fine(() -> msg("DATA(%d) end", rid));
                handler.dataEnd(rid);
            } else {
                FixedLengthInputStream out =
                    new FixedLengthInputStream(clen, in);
                final int fclen = clen;
                logger.fine(() -> msg("DATA(%d, %d)", rid, fclen));
                handler.data(rid, clen, out);
                out.skipRemaining();
            }
        }

        default -> {
            if (!require(() -> msg("unknown-%d", rtype), clen)) return false;
            rejectMessage(rver, rtype, rid, clen, plen, reasons);
            handler.bad(RecordHandler.UNKNOWN_TYPE, rver, rtype, clen, rid);
        }
        }

        /* Skip over trailing padding. */
        skip(plen);

        return true;
    }

    private static int unser(byte[] buf, int off, int len) {
        return unser(0, buf, off, len);
    }

    private static int unser(int r, byte[] buf, int off, int len) {
        while (len-- > 0) {
            r <<= 8;
            r |= buf[off++] & 0xff;
        }
        return r;
    }

    private String msg(String fmt, Object... args) {
        return tag + ":in:" + String.format(fmt, args);
    }

    private void rejectMessage(int rver, int rtype, int rid, int iclen,
                               int iplen, int fr) {
        logger.warning(() -> msg(
                                 "rejected %s ver=%d"
                                     + " rid=%d clen=%d plen=%d%s%s%s",
                                 RecordTypes.toString(rtype), rver, rid, iclen,
                                 iplen,
                                 (fr & RecordHandler.BAD_VERSION) != 0 ?
                                     " bad-version" : "",
                                 (fr & RecordHandler.BAD_LENGTH) != 0 ?
                                     " bad-length" : "",
                                 (fr & RecordHandler.BAD_REQ_ID) != 0 ?
                                     " bad-req-id" : ""));

    }

    private static final Logger logger =
        Logger.getLogger(RecordReader.class.getPackageName());
}
