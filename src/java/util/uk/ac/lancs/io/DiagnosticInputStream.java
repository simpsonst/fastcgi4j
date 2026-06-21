// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2026, Lancaster University
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

package uk.ac.lancs.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Generates an almost infinite sequence of US-ASCII lines, each
 * beginning with the hex offset from the start of the stream. Lines
 * have the form:
 * 
 * <pre>
 * XXXXXXXXXXXXXXXX 123456789abcdef0123456789abcdef0123456789abcd
 * </pre>
 * 
 * <p>
 * The first 16 characters are replaced with the offset at the start of
 * the line. The 17th character is a space U+0020. An optional carriage
 * return U+000D and a mandatory new line U+000A terminate the line.
 * Remaining characters are the lowest nibble of the column position in
 * hex.
 * 
 * <p>
 * The width of the line can be controlled, but must be a multiple of
 * 16, and at least 32 characters (to fit the offset, a space and CRLF).
 * 
 * @author simpsons
 */
public class DiagnosticInputStream extends InputStream {
    private static final byte[] ASCII_HEX =
        "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    private final int width;

    private final boolean withCR;

    /**
     * Create a stream with lines of a at least a given width. The width
     * is rounded up to a multiple of 16, with a minimum of 32.
     * 
     * @param width the minimum width
     * 
     * @param withCR {@code true} if the penultimate character of each
     * line should be a carriage return U+000D
     */
    private DiagnosticInputStream(int width, boolean withCR) {
        this.width = Integer.max(32, (width + 15) / 16 * 16);
        this.withCR = withCR;
    }

    /**
     * Start building a diagnostic stream.
     * 
     * @return the object used to build the stream
     */
    public static Builder start() {
        return new Builder();
    }

    /**
     * Builds a diagnostic stream from desired characteristics.
     */
    public static class Builder {
        private int width = 64;

        private boolean withCR = true;

        private Builder() {}

        /**
         * Build the stream from current settings.
         * 
         * @return the new stream
         */
        public DiagnosticInputStream build() {
            return new DiagnosticInputStream(width, withCR);
        }

        /**
         * Specify whether carriage returns are to be included in the
         * lines.
         * 
         * @param state {@code true} if the penultimate character of
         * each line shall be a carriage return; {@code false} otherwise
         * 
         * @return this object
         */
        public Builder withCarriageReturns(boolean state) {
            this.withCR = state;
            return this;
        }

        /**
         * Specify the minimum width of each line. The width is rounded
         * up to a multiple of 16, with a minimum of 32.
         * 
         * @param width the minimum width
         * 
         * @return this object
         */
        public Builder width(int width) {
            this.width = width;
            return this;
        }
    }

    private boolean closed = false;

    private long pos = 0;

    /**
     * Estimate the number of bytes that can be read without blocking.
     * This will typically return {@value Integer#MAX_VALUE} until very
     * close to the end.
     * 
     * @return the estimated number of bytes
     * 
     * @throws IOException if the stream has been closed
     */
    @Override
    public int available() throws IOException {
        if (closed) throw new IOException("closed");
        return (int) Long
            .min(pos == 0 ? Long.MAX_VALUE : Long.MAX_VALUE - pos + 1,
                 Integer.MAX_VALUE);
    }

    /**
     * Skip a number of bytes.
     * 
     * @param n the maximum number of bytes to skip
     * 
     * @return the number of bytes skipped
     * 
     * @throws IOException if the stream has been closed
     * 
     * @throws IllegalArgumentException if the requested number of bytes
     * is negative
     */
    @Override
    public long skip(long n) throws IOException {
        if (closed) throw new IOException("closed");
        if (n < 0) throw new IllegalArgumentException("-ve length " + n);
        if (pos < 0) return 0;
        long done =
            Long.min(pos == 0 ? Long.MAX_VALUE : Long.MAX_VALUE - n + 1, n);
        pos += done;
        return done;
    }

    /**
     * Read bytes into part of an array.
     * 
     * @param b the buffer into which the bytes shall be written
     * 
     * @param off the index into the array of the first byte to be
     * overwritten
     * 
     * @param len the number of bytes to read
     * 
     * @return the requested number of bytes
     * 
     * @throws IOException if the stream has been closed
     * 
     * @throws IllegalArgumentException if the requested number of bytes
     * is negative
     * 
     * @throws ArrayIndexOutOfBoundsException if the range of bytes is
     * not within the array
     * 
     * @throws NullPointerException if the buffer is {@code null}s
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) throw new IOException("closed");
        Objects.requireNonNull(b, "b");
        if (len < 0) throw new IllegalArgumentException("-ve length " + len);
        if (off < 0 || off > b.length)
            throw new ArrayIndexOutOfBoundsException(off);
        len = (int) Long
            .min(pos == 0 ? Long.MAX_VALUE : Long.MAX_VALUE - pos + 1, len);
        final int lim = off + len;
        if (lim > b.length) throw new ArrayIndexOutOfBoundsException(lim);
        if (pos < 0) return -1;
        final int orig = off;

        /* Where do we start within the line? */
        int lpos = (int) (pos % width);

        /* What is the counter for this line? */
        long cpos = (pos - pos % width);
        assert cpos % width == 0;
        while (off < lim) {
            try {
                if (lpos < 16) {
                    /* We're in the counter. Which digit of the value
                     * should represented? */
                    int digidx = 15 - lpos;

                    /* What is the value of the digit? */
                    int dig = (int) ((cpos >>> 4 * digidx) & 0xf);

                    /* Extract that digit, and convert to hex. */
                    b[off++] = ASCII_HEX[dig];
                    continue;
                }

                /* We put a space after the counter. */
                if (lpos == 16) {
                    b[off++] = 32;
                    continue;
                }

                /* The last characters of each line are CR and LF. */
                if (withCR && lpos == width - 2) {
                    b[off++] = 13;
                    continue;
                }
                if (lpos == width - 1) {
                    b[off++] = 10;
                    continue;
                }

                /* Other characters are hex digits giving column
                 * position. */
                b[off++] = ASCII_HEX[lpos % 16];
            } finally {
                ++pos;
                if (++lpos == width) {
                    lpos = 0;
                    cpos += width;
                }
            }
        }
        return off - orig;
    }

    /**
     * Read a single byte.
     * 
     * @return the byte as an unsigned value; or {@code -1} on EOF
     * 
     * @throws IOException if the stream has been closed
     */
    @Override
    public int read() throws IOException {
        if (closed) throw new IOException("closed");
        if (pos < 0) return -1;
        try {
            /* Where are we in the line? */
            int lpos = (int) (pos % width);
            if (lpos < 16) {
                /* We're in the counter. What value should be
                 * represented? */
                long cpos = (pos - pos % width);

                /* Which digit of the value should represented? */
                int digidx = 15 - lpos;

                /* What is the value of the digit? */
                int dig = (int) ((cpos >>> 4 * digidx) & 0xf);

                /* Extract that digit, and convert to hex. */
                return ASCII_HEX[dig] & 0xff;
            }

            /* We put a space after the counter. */
            if (lpos == 16) return 32;

            /* The last characters of each line are CR and LF. */
            if (withCR && lpos == width - 2) return 13;
            if (lpos == width - 1) return 10;

            /* Other characters are hex digits giving column
             * position. */
            return ASCII_HEX[lpos % 16] & 0xff;
        } finally {
            pos++;
        }
    }

    /**
     * Close the stream. Subsequent attempts to read or skip bytes will
     * throw an exception.
     */
    @Override
    public void close() {
        closed = true;
    }
}
