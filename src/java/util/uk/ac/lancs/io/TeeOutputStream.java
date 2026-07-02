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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Duplicates bytes to another stream.
 *
 * @author simpsons
 */
public class TeeOutputStream extends FilterOutputStream {
    /**
     * Identifies the secondary stream to be written to.
     */
    protected final OutputStream other;

    /**
     * Create a stream that writes to two streams.
     * 
     * @param out the main stream
     * 
     * @param other the auxiliary stream
     */
    public TeeOutputStream(OutputStream out, OutputStream other) {
        super(out);
        this.other = other;
    }

    /**
     * Close both underlying stream.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        try {
            other.close();
        } finally {
            out.close();
        }
    }

    /**
     * Flush both underlying stream.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
        try {
            other.flush();
        } finally {
            out.flush();
        }
    }

    /**
     * Write a portion of an array to both underlying streams.
     * 
     * @param b the array containing the bytes to write
     * 
     * @param off the index into the array of the first byte to write
     * 
     * @param len the number of bytes to write
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            other.write(b, off, len);
        } finally {
            /* Don't call super.write(...), as it just makes multiple
             * calls to the single-byte method. */
            out.write(b, off, len);
        }
    }

    /**
     * Write a byte to both underlying streams.
     * 
     * @param b the byte to write
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(int b) throws IOException {
        try {
            other.write(b);
        } finally {
            out.write(b);
        }
    }
}
