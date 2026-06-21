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

package uk.ac.lancs.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads to the end of its base stream when closed.
 *
 * @author simpsons
 */
public class CompletingInputStream extends FilterInputStream {
    /**
     * Ensure a stream is fully read before closing.
     * 
     * @param in the base stream
     */
    public CompletingInputStream(InputStream in) {
        super(in);
    }

    private boolean closed = false;

    private void checkClosed() throws IOException {
        if (closed) throw new IOException("closed");
    }

    /**
     * Reset the stream to its last marked position.
     * 
     * @throws IOException if the stream has not been marked or the mark
     * has been invalidated
     */
    @Override
    public void reset() throws IOException {
        checkClosed();
        in.reset();
    }

    /**
     * Get an estimate of bytes immediately available.
     * 
     * @return the requested estimate
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int available() throws IOException {
        checkClosed();
        return in.available();
    }

    /**
     * Skip bytes.
     * 
     * @param n the maximum number of bytes to skip
     * 
     * @return the number of bytes skipped
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long skip(long n) throws IOException {
        checkClosed();
        return in.skip(n);
    }

    /**
     * Read bytes into a portion of an array.
     * 
     * @param b the array to read into
     * 
     * @param off the index of the first byte to overwrite
     * 
     * @param len the maximum number of bytes to read
     * 
     * @return the number of bytes read; or {@code -1} on EOF
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkClosed();
        return in.read(b, off, len);
    }

    /**
     * Read a single byte.
     * 
     * @return the next byte; or {@code -1} on EOF
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        checkClosed();
        return in.read();
    }

    /**
     * Discard all remaining bytes of the base stream, and then close
     * it. A second call to this method has no effect.
     * 
     * @throws IOException if an I/O error occurs on clearing or closing
     * the base stream
     */
    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        for (;;) {
            long amount = in.skip(65536);
            if (amount > 0) continue;
            int c = in.read();
            if (c < 0) break;
        }
        in.close();
    }
}
