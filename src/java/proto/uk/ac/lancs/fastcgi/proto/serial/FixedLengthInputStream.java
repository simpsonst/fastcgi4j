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

/**
 * Reads an exact number of bytes from a base stream. Closing this
 * stream does not close the base stream. The intent is to temporarily
 * pass an internal stream to a user (in the form of a new view of the
 * stream), so that it can read an exact number of bytes, before
 * returning control. The user will not be permitted to read more than
 * the specified amount, and the presented stream will appear to have
 * reached end-of-file. If the user closes the presented stream,
 * remaining bytes from those allocated will be skipped, but the
 * internal stream will not be closed. The provider should call
 * {@link #skipRemaining()} when control has returned from the user, to
 * ensure that remaining bytes are consumed, in case the user did not
 * close the presented stream.
 * 
 * <p>
 * On error, the provider should regard its internal stream as corrupt,
 * and use it no further.
 *
 * @author simpsons
 */
class FixedLengthInputStream extends InputStream {
    private final InputStream in;

    private int rem;

    private IOException ex;

    /**
     * Permit an exact number of bytes to be read.
     * 
     * @param rem the number of bytes to be read
     * 
     * @param in the stream to read from
     */
    public FixedLengthInputStream(int rem, InputStream in) {
        this.rem = rem;
        this.in = in;
    }

    /**
     * Read at most one byte.
     * 
     * @return the byte as an unsigned value; or {@code -1} on
     * end-of-file
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        if (ex != null) throw new IOException("already failed", ex);
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

    /**
     * Read bytes into an array.
     * 
     * @param b the array to hold the bytes
     * 
     * @param off the offset of the first byte
     * 
     * @param len the maximum number of bytes to be read
     * 
     * @return the number of bytes read; or {@code -1} on end-of-file
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (ex != null) throw new IOException("already failed", ex);
        try {
            if (rem == 0) return -1;
            if (len > rem) len = rem;
            int got = in.read(b, off, len);
            if (got > 0) rem -= got;
            return got;
        } catch (IOException ex) {
            this.ex = ex;
            throw ex;
        }
    }

    private void clear() throws IOException {
        while (rem > 0) {
            long got = skip(rem);
            rem -= got;
            if (got == 0 && rem > 0) {
                int c = read();
                if (c < 0) break;
                rem--;
            }
        }
    }

    /**
     * Consume remaining bytes.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (ex != null) throw new IOException("already failed", ex);
        try {
            clear();
        } catch (IOException ex) {
            this.ex = ex;
            throw ex;
        }
    }

    /**
     * Clear remaining bytes left unconsumed by the user.
     * 
     * @throws IOException if an I/O error occurs
     */
    public void skipRemaining() throws IOException {
        assert ex == null;
        clear();
    }
}
