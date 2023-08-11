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
 * Converts an incoming stream encoded in Base64 into its original form.
 *
 * @author simpsons
 */
class Base64DecodingInputStream extends FilterInputStream {
    private final boolean closeAfter;

    private final byte[] buf = new byte[1024];

    private int out = 0;

    private int in = 0;

    private boolean receivedEnd = false;

    /**
     * Decode a Base64-encoded input stream, presenting the decoded data
     * as a new stream.
     * 
     * @param in the encoded stream
     * 
     * @param closeAfter {@code true} if the encoded stream is to be
     * closed when this stream is closed; {@code false} if it is to be
     * left open
     */
    public Base64DecodingInputStream(InputStream in, boolean closeAfter) {
        super(in);
        this.closeAfter = closeAfter;
    }

    /**
     * Provide an estimate of the number of bytes available.
     * 
     * @default This implementation simply calls the base stream, and
     * multiplies its estimate by &frac34;, and adds in any already
     * decoded bytes not yet passed to the user.
     * 
     * @return an estimate of the number of bytes available
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int available() throws IOException {
        checkClosed();
        return out - in + super.available() * 3 / 4;
    }

    private boolean populate() throws IOException {
        if (out < in) return true;
        if (receivedEnd) return false;
        assert out == in;
        out = in = 0;
        do {
            int got = super.read(buf, in, buf.length - in);
            if (got < 0) {
                receivedEnd = true;
                return false;
            }
            in += got;
        } while (out == in);
        assert out == 0;
        return true;
    }

    private int value = 0;

    private int width = 0;

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

    /**
     * Read decoded bytes into part of an array.
     * 
     * @param dest the destination array
     * 
     * @param off the offset into the array of the first element ti be
     * written
     * 
     * @param len the maximum number of bytes to read
     * 
     * @return the number of bytes read and placed in the buffer; or
     * <samp>-1</samp> end-of-file is reached
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] dest, int off, int len) throws IOException {
        checkClosed();
        final int lim = off + len;
        final int orig = off;
        while (off < lim && populate()) {
            decode();

            /* Extract and store whole octets from the high end of the
             * int buffer, while available and there's space. */
            while (off < lim && width >= 8) {
                width -= 8;
                dest[off++] = (byte) (value >>> width);
            }
        }
        if (off == orig) return -1;
        return off - orig;
    }

    private void decode() {
        /* Keep going, as long as we have room in our int buffer
         * 'value', and we have encoded hextets to fill it with. */
        while (width < 32 - 6 && out < in) {
            int d = Base64.decode(buf[out++]);
            if (d >= 0) {
                /* The byte corresponds to a legitimate hextet. Push it
                 * in as the lower 6 bits. */
                assert d < 64;
                value <<= 6;
                value |= d;
                width += 6;
                assert width <= 32;
            }
        }
    }

    /**
     * Read a single decoded byte.
     * 
     * @return the decoded byte as an unsigned value; or <samp>-1</samp>
     * if end-of-file is reached
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        checkClosed();
        while (populate()) {
            decode();

            /* Extract a single byte from the high end of the int
             * buffer. */
            if (width >= 8) {
                width -= 8;
                return (value >>> width) & 0xff;
            }
        }
        return -1;
    }

    /**
     * @undocumented
     */
    public static void main(String[] args) throws Exception {
        try (InputStream in = new Base64DecodingInputStream(System.in, false)) {
            in.transferTo(System.out);
        }
    }
}
