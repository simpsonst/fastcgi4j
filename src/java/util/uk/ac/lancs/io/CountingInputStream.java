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
import java.util.function.LongConsumer;

/**
 * Counts bytes read from a a stream.
 *
 * @author simpsons
 */
public class CountingInputStream extends FilterInputStream {
    private final LongConsumer counter;

    /**
     * Create a byte-counting stream wrapped around another stream.
     * 
     * @param in the stream to wrap around
     * 
     * @param counter the counter to increment by the number of bytes
     * read
     */
    public CountingInputStream(InputStream in, LongConsumer counter) {
        super(in);
        this.counter = counter;
    }

    /**
     * Read bytes from the stream into part of an array. If EOF is not
     * reached, and no exception is thrown, the configured counter will
     * be incremented by the number of bytes read.
     * 
     * @param b the array to store the bytes in
     * 
     * @param off the index into the array of the first byte
     * 
     * @param len the maximum number of bytes to read
     * 
     * @return the number of bytes read; or {@code -1} if EOF is reached
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int rc = super.read(b, off, len);
        if (rc > 0) counter.accept(rc);
        return rc;
    }

    /**
     * Read a byte from the stream. If EOF is not reached, and no
     * exception is thrown, the configured counter will be incremented
     * by 1.
     * 
     * @return the byte read as an unsigned value; or {@code -1} if EOF
     * is reached
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        int rc = super.read();
        if (rc >= 0) counter.accept(1);
        return rc;
    }
}
