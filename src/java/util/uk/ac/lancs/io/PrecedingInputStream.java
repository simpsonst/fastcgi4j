// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2022,2023,2026, Lancaster University
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
 * Splits a stream into a head and a tail. This object is the head, and
 * {@link #tail()} yields the tail. Most operations on the tail fail
 * with {@link IllegalStateException} if the head stream has not been
 * closed. Closing the head stream does not close the underlying stream,
 * but closing the tail does.
 *
 * @author simpsons
 */
public class PrecedingInputStream extends FilterInputStream {
    /**
     * Create a stream which will be the head of a base stream.
     * 
     * @param in the base stream
     */
    public PrecedingInputStream(InputStream in) {
        super(in);
        this.tail = new FilterInputStream(in) {
            private void check() {
                if (!closed)
                    throw new IllegalStateException("prior stream not closed");
            }

            @Override
            public void reset() throws IOException {
                check();
                super.reset();
            }

            @Override
            public void mark(int readlimit) {
                check();
                super.mark(readlimit);
            }

            @Override
            public void close() throws IOException {
                check();
                super.close();
            }

            @Override
            public int available() throws IOException {
                check();
                return super.available();
            }

            @Override
            public long skip(long n) throws IOException {
                check();
                return super.skip(n);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                check();
                return super.read(b, off, len);
            }

            @Override
            public int read() throws IOException {
                return super.read();
            }
        };
    }

    private final InputStream tail;

    private boolean closed = false;

    /**
     * Get the tail stream.
     * 
     * @return the tail stream
     */
    public InputStream tail() {
        return tail;
    }

    /**
     * Close the head stream. The underlying stream is not closed.
     * Operations on the tail stream no longer fail with
     * {@link IllegalStateException}. Subsequent calls to this method
     * have no effect.
     */
    @Override
    public void close() {
        if (closed) return;
        closed = true;
    }
}
