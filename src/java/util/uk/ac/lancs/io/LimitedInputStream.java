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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Feigns EOF after a given number of bytes.
 *
 * @author simpsons
 */
public class LimitedInputStream extends FilterInputStream {
    private long remaining;

    private boolean closed = false;

    private IOException fault = null;

    /**
     * Wrap an input stream so that a limited number of bytes may be
     * read.
     * 
     * @param in the base input stream
     * 
     * @param limit the maximum number of bytes to allow to be read
     */
    public LimitedInputStream(InputStream in, long limit) {
        super(in);
        this.remaining = limit;
    }

    private void checkErrors() throws IOException {
        if (closed) throw new IOException("stream closed");
        if (fault != null) throw new IOException("already failed", fault);
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
        checkErrors();
        try {
            int got = super.read(b, off, len);
            if (got > 0) remaining -= got;
            return got;
        } catch (IOException ex) {
            fault = ex;
            throw ex;
        }
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
        checkErrors();
        try {
            int got = super.read();
            if (got >= 0) remaining--;
            return got;
        } catch (IOException ex) {
            fault = ex;
            throw ex;
        }
    }

    /**
     * Estimate the number of bytes that can be read without blocking.
     * 
     * @return the estimated number of bytes
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int available() throws IOException {
        try {
            int base = super.available();
            return remaining < base ? (int) remaining : base;
        } catch (IOException ex) {
            fault = ex;
            throw ex;
        }
    }

    /**
     * Close the stream. If bytes remain on the underlying stream, no
     * attempt is made to read them.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        try {
            super.close();
        } catch (IOException ex) {
            /* Recording this error here has no real effect, as the
             * wrapping stream is now marked closed, but we'll do it
             * anyway. */
            fault = ex;
            throw ex;
        }
    }
}
