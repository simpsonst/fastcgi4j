/*
 * Copyright (c) 2022, Regents of the University of Lancaster
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

package uk.ac.lancs.fastcgi.proto.app;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import uk.ac.lancs.fastcgi.proto.RecordTypes;

/**
 * Deserializes FastCGI records.
 *
 * @author simpsons
 */
public class RecordReader {
    private final InputStream in;

    private final Charset charset;

    private final RecordHandler handler;

    /**
     * Prepare to read records from a stream.
     * 
     * @param in the stream of serialized records
     * 
     * @param charset the encoding to expect for name/value pairs
     * 
     * @param handler a destination for deserialized records
     */
    public RecordReader(InputStream in, Charset charset,
                        RecordHandler handler) {
        this.in = in;
        this.charset = charset;
        this.handler = handler;
    }

    private byte[] buf = new byte[8];

    private boolean require(int exp) throws IOException {
        if (exp > buf.length) buf = new byte[exp];
        int len = 0;
        while (len < exp) {
            int got = in.read(buf, len, exp - len);
            if (got < 0) return false;
            len += got;
        }
        return true;
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
        if (!require(8)) return false;
        final int rver = unser(buf, 0, 1);
        final int rtype = unser(buf, 1, 1);
        final int rid = unser(buf, 2, 2);
        int clen = unser(buf, 4, 2);
        int plen = unser(buf, 6, 1);
        int reasons = 0;
        switch (rtype) {
        case RecordTypes.ABORT_REQUEST:
            if (rver < 1) reasons |= RecordHandler.BAD_VERSION;
            if (clen != 0) reasons |= RecordHandler.BAD_LENGTH;
            if (rid == 0) reasons |= RecordHandler.BAD_REQ_ID;
            if (reasons != 0) {
                handler.bad(reasons, rver, rtype, clen, rid);
                break;
            }
            handler.abortRequest(rid);
            break;

        case RecordTypes.BEGIN_REQUEST:
            if (rver < 1) reasons |= RecordHandler.BAD_VERSION;
            if (clen != 8) reasons |= RecordHandler.BAD_LENGTH;
            if (rid == 0) reasons |= RecordHandler.BAD_REQ_ID;
            if (reasons != 0) {
                handler.bad(reasons, rver, rtype, clen, rid);
                break;
            }
            if (!require(clen)) return false;
            int role = unser(buf, 0, 2);
            int flags = unser(buf, 2, 1);
            handler.beginRequest(rid, role, flags);
            break;

        case RecordTypes.GET_VALUES:
            if (rver < 1) reasons |= RecordHandler.BAD_VERSION;
            if (rid == 0) reasons |= RecordHandler.BAD_REQ_ID;
            if (reasons != 0) {
                handler.bad(reasons, rver, rtype, clen, rid);
                break;
            }
            Map<String, String> vars = new HashMap<>();
            while (clen > 0) {
                if (!require(1)) return false;
                int nlen = unser(buf, 0, 1);
                clen--;
                if (nlen > 127) {
                    if (clen < 3) break;
                    if (!require(3)) return false;
                    clen -= 3;
                    nlen = unser(nlen & 0x7f, buf, 0, 3);
                }

                if (clen < 1) break;
                if (!require(1)) return false;
                int vlen = unser(buf, 0, 1);
                clen--;
                if (vlen > 127) {
                    if (clen < 3) break;
                    if (!require(3)) return false;
                    clen -= 3;
                    vlen = unser(vlen & 0x7f, buf, 0, 3);
                }

                if (clen < nlen) break;
                if (!require(nlen)) return false;
                clen -= nlen;
                String name = new String(buf, 0, nlen, charset);

                if (clen < vlen) break;
                if (!require(vlen)) return false;
                clen -= vlen;
                String value = new String(buf, 0, vlen, charset);
                vars.put(name, value);
            }
            plen += clen;
            handler.getValues(vars.keySet());
            break;

        case RecordTypes.PARAMS: {
            if (rver < 1) reasons |= RecordHandler.BAD_VERSION;
            if (rid == 0) reasons |= RecordHandler.BAD_REQ_ID;
            if (reasons != 0) {
                handler.bad(reasons, rver, rtype, clen, rid);
                break;
            }
            if (clen == 0) {
                handler.paramsEnd(rid);
                break;
            }
            Slice out = new Slice(clen);
            handler.params(rid, clen, out);
            if (out.check() > 0) return false;
            break;
        }

        case RecordTypes.STDIN: {
            if (rver < 1) reasons |= RecordHandler.BAD_VERSION;
            if (rid == 0) reasons |= RecordHandler.BAD_REQ_ID;
            if (reasons != 0) {
                handler.bad(reasons, rver, rtype, clen, rid);
                break;
            }
            if (clen == 0) {
                handler.stdinEnd(rid);
                break;
            }
            Slice out = new Slice(clen);
            handler.stdin(rid, clen, out);
            if (out.check() > 0) return false;
            break;
        }

        case RecordTypes.DATA: {
            if (rver < 1) reasons |= RecordHandler.BAD_VERSION;
            if (rid == 0) reasons |= RecordHandler.BAD_REQ_ID;
            if (reasons != 0) {
                handler.bad(reasons, rver, rtype, clen, rid);
                break;
            }
            if (clen == 0) {
                handler.dataEnd(rid);
                break;
            }
            Slice out = new Slice(clen);
            handler.data(rid, clen, out);
            if (out.check() > 0) return false;
            break;
        }

        default:
            if (!require(clen)) return false;
            handler.bad(RecordHandler.UNKNOWN_TYPE, rver, rtype, clen, rid);
            break;
        }

        /* Skip over trailing padding. */
        while (plen > 0) {
            long got = in.skip(plen);
            plen -= got;
            if (got == 0 && plen > 0) {
                int c = in.read();
                if (c < 0) return false;
                plen--;
            }
        }

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

    private class Slice extends InputStream {
        private int rem;

        private IOException ex;

        public Slice(int rem) {
            this.rem = rem;
        }

        @Override
        public int read() throws IOException {
            if (ex != null) throw ex;
            try {
                if (rem == 0) return -1;
                int c = in.read();
                if (c >= 0) rem--;
                return c;
            } catch (IOException ex) {
                this.ex = ex;
                throw ex;
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (ex != null) throw ex;
            try {
                if (len > rem) len = rem;
                int got = in.read(b, off, len);
                if (got > 0) rem -= got;
                return got;
            } catch (IOException ex) {
                this.ex = ex;
                throw ex;
            }
        }

        @Override
        public void close() throws IOException {
            if (ex == null) try {
                while (rem > 0) {
                    long got = skip(rem);
                    rem -= got;
                    if (got == 0 && rem > 0) {
                        int c = read();
                        if (c < 0) break;
                        rem--;
                    }
                }
            } catch (IOException ex) {
                this.ex = ex;
                throw ex;
            }
        }

        public int check() throws IOException {
            if (ex != null) throw ex;
            close();
            return rem;
        }
    }
}
