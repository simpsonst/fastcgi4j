// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2023, Lancaster University
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
 *  Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.fastcgi.mime;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Converts an incoming stream encoded as Quoted-Printable into its
 * original form.
 * 
 * @author simpsons
 */
class QuotedPrintableDecodingInputStream extends FilterInputStream {
    private final boolean closeAfter;

    /**
     * Prepare to decode a stream.
     * 
     * @param in the stream to be decoded
     * 
     * @param closeAfter {@code true} if the encoded stream is to be
     * closed when this stream is closed; {@code false} if it is to be
     * left open
     */
    public QuotedPrintableDecodingInputStream(InputStream in,
                                              boolean closeAfter) {
        super(in);
        this.closeAfter = closeAfter;
    }

    /**
     * Provide an estimate of the number of bytes available.
     * 
     * @default This implementation simply calls the base stream,
     * multiplies by a large fraction, and then adds in any already
     * decoded bytes not yet passed to the user.
     * 
     * @return an estimate of the number of bytes available
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int available() throws IOException {
        checkClosed();
        return clear - out + (super.available() + (in - clear)) * 65 / 72;
    }

    private boolean closed = false;

    private void checkClosed() throws IOException {
        if (closed) throw new IOException("closed");
    }

    /**
     * Close the stream. The encoded stream is also closed if specified
     * in the constructor. Calling this method a second time has no
     * effect.
     * 
     * @throws IOException if an I/O error occurs in closing the encoded
     * stream
     */
    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        if (closeAfter) super.close();
    }

    private final byte[] buf = new byte[1024];

    /**
     * Holds the index of the first decoded byte in the buffer available
     * to read by the user. The exception is when it equals
     * {@link #clear}, which is never less than it.
     */
    private int out = 0;

    /**
     * Holds the index of the first byte yet to be decoded in the
     * buffer. The exception is when it equals {@link #in}, which is
     * never less than it.
     */
    private int clear = 0;

    /**
     * Holds the index of the first free byte after any being processed.
     * The exception is when it equals
     * <code>{@linkplain #buf}.length</code>, which it is never more
     * than.
     */
    private int in = 0;

    /**
     * Records when the base stream as returned EOF.
     */
    private boolean receivedEnd = false;

    private void advanceToEquals() {
        while (clear < in && buf[clear] != ASCII.EQUALS)
            clear++;
    }

    private void compact() {
        System.arraycopy(buf, out, buf, 0, in - out);
        in -= out;
        out = clear = 0;
    }

    /**
     * Ensure that there are decoded bytes in the buffer.
     * 
     * @return {@code true} if there are decoded bytes available to the
     * user; {@code false} otherwise
     * 
     * @throws IOException if an I/O error occurs
     */
    private boolean populate() throws IOException {
        check_cleared: while (out == clear) {
            /* There are no bytes we haven't ascertained to be literal.
             * Can we ascertain more? Be sure we know where the next =
             * is. */
            advanceToEquals();
            if (clear > out) return true;

            assert out == clear;
            assert clear == in || buf[clear] == ASCII.EQUALS;
            /* We have a potential encoded sequence. Can we decode
             * it? */

            if (clear + 3 >= in) {
                /* Each encoded sequence is 3 bytes long. We don't have
                 * enough. Are there any more? */
                if (receivedEnd) {
                    /* There are no more bytes. We'll have to treat
                     * these last few as literal. */
                    if (clear == in) {
                        assert out == clear;
                        return false;
                    }
                    clear = in;
                    continue;
                }
                /* There are thought to be some bytes. Do we have enough
                 * space? */
                if (buf.length - clear < 3) {
                    /* Try to compact the buffer. */
                    assert out == clear;
                    assert out > 0;
                    compact();
                }
                final int rem = buf.length - in;
                assert rem > 0;
                final int got = super.read(buf, in, rem);
                if (got < 0) {
                    receivedEnd = true;
                } else {
                    in += got;
                }
                continue;
            }

            /* We've got three bytes beginning with ASCII =. Is it a
             * proper sequence? */
            assert clear + 3 < in;
            assert in <= buf.length;
            assert clear + 1 < buf.length;
            assert clear + 2 < buf.length;
            int n1, n2;
            if (buf[clear + 1] == ASCII.CR && buf[clear + 2] == ASCII.LF) {
                /* It's a soft line wrap. Delete the entire sequence. */
                assert out == clear;
                out = clear += 3;
            } else if ((n1 = QuotedPrintable.hexval(buf[clear + 1])) >= 0 &&
                (n2 = QuotedPrintable.hexval(buf[clear + 2])) >= 0) {
                /* It's an encoded byte. Work it out, and replace the
                 * 3-byte encoded sequence with this byte. */
                buf[clear + 2] = (byte) ((n1 << 4) | n2);
                out += 2;
                clear += 3;
            } else {
                /* It's not proper QP, but we'll be tolerant, and skip
                 * over it. */
                clear++;
            }
        }
        return true;
    }

    /**
     * Attempt to read several decoded bytes into a buffer.
     * 
     * @param b the buffer to read bytes into
     * 
     * @param off the offset into the buffer of the first byte to
     * overwrite
     * 
     * @param len the maximum number of bytes to overwrite
     * 
     * @return the number of decoded bytes read and written to the
     * buffer
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkClosed();
        if (populate()) {
            int min = Math.min(len, clear - out);
            System.arraycopy(buf, out, b, off, min);
            out += min;
            return min;
        }
        return -1;
    }

    /**
     * Read a single decoded byte.
     * 
     * @return the decoded byte as an unsigned integer; or
     * <samp>-1</samp> if end-of-file is reached
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        checkClosed();
        if (populate()) {
            assert out < clear;
            return buf[out++] & 0xff;
        }
        return -1;
    }

    /**
     * @undocumented
     */
    public static void main(String[] args) throws Exception {
        try (InputStream in =
            new QuotedPrintableDecodingInputStream(System.in, false)) {
            in.transferTo(System.out);
        }
    }
}
