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
import java.io.OutputStream;

/**
 * Ignores byte data.
 * 
 * @author simpsons
 */
public class NullOutputStream extends OutputStream {
    private boolean closed = false;

    private void checkClosed() throws IOException {
        if (closed) throw new IOException("stream closed");
    }

    /**
     * Ignore a byte.
     * 
     * @param i the byte to discard
     * 
     * @throws IOException if the stream has been closed
     */
    @Override
    public void write(int i) throws IOException {
        checkClosed();
    }

    /**
     * Ignore bytes from an array.
     * 
     * @param b the array containing the bytes
     * 
     * @param off the index into the array of the first byte to ignore
     * 
     * @param len the number of bytes to ignore
     * 
     * @throws IOException if the stream has been closed
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkClosed();
    }

    /**
     * Do nothing.
     * 
     * @throws IOException if the stream has been closed
     */
    @Override
    public void flush() throws IOException {
        checkClosed();
    }

    /**
     * Close the stream. This method can be called multiple times
     * without error.
     */
    @Override
    public void close() {
        closed = true;
    }
}
