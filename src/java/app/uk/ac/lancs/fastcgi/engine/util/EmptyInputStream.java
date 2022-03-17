/*
 * Copyright (c) 2022, Lancaster University
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

package uk.ac.lancs.fastcgi.engine.util;

import java.io.InputStream;

/**
 * Yields exactly zero bytes.
 * 
 * @author simpsons
 */
class EmptyInputStream extends InputStream {
    /**
     * Close the stream. In fact, this method has nothing to do.
     */
    @Override
    public void close() {}

    /**
     * Get the number of bytes available on this stream.
     * 
     * @return the number of available bytes, which is zero
     */
    @Override
    public int available() {
        return 0;
    }

    /**
     * Skip bytes on this stream. As there are none, none are skipped.
     * 
     * @param n the maximum number of bytes to skip
     * 
     * @return the number of bytes skipped, which is zero
     */
    @Override
    public long skip(long n) {
        return 0;
    }

    /**
     * Read bytes from this stream into an array.
     * 
     * @param b the array to write bytes into
     * 
     * @param off the index of the first byte to overwrite
     * 
     * @param len the maximum number of bytes to read
     * 
     * @return {@code -1}, indicating that there are no more bytes to
     * read
     */
    @Override
    public int read(byte[] b, int off, int len) {
        return -1;
    }

    /**
     * Read a byte from this stream.
     * 
     * @return {@code -1}, indicating that there are no more bytes to
     * read
     */
    @Override
    public int read() {
        return -1;
    }
}
