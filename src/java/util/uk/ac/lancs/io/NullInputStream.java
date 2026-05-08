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
import java.util.Arrays;

/**
 * Generates an infinite stream of null bytes.
 *
 * @author simpsons
 */
public class NullInputStream extends InputStream {
    private boolean closed = false;

    /**
     * Close the stream. Attempting to read or skip further bytes throws
     * an exception.
     */
    @Override
    public void close() {
        closed = true;
    }

    /**
     * Estimate the number of bytes that can be read without blocking.
     * As the stream is infinite, {@value Integer#MAX_VALUE} is
     * returned.
     * 
     * @return the estimated number of bytes available
     * 
     * @throws IOException if the stream has been closed
     */
    @Override
    public int available() throws IOException {
        if (closed) throw new IOException("closed");
        return Integer.MAX_VALUE;
    }

    /**
     * Skip a number of bytes.
     * 
     * @param n the maximum number of bytes to skip
     * 
     * @return the number of bytes skipped
     * 
     * @throws IOException if the stream has been closed
     */
    @Override
    public long skip(long n) throws IOException {
        if (closed) throw new IOException("closed");
        return n;
    }

    /**
     * Read bytes into part of an array.
     * 
     * @param b the array
     * 
     * @param off the index into the array of the first element to be
     * overwritten
     * 
     * @param len the maximum number of bytes to read
     * 
     * @return the number of bytes read
     * 
     * @throws IOException if the stream has been closed
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) throw new IOException("closed");
        Arrays.fill(b, off, off + len, (byte) 0);
        return len;
    }

    /**
     * Read one byte.
     * 
     * @return {@code 0} always
     * 
     * @throws IOException if the stream has been closed
     */
    @Override
    public int read() throws IOException {
        if (closed) throw new IOException("closed");
        return 0;
    }
}
