/*
 * Copyright (c) 2022, Regents of the University of Lancaster
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

import java.io.IOException;
import java.io.InputStream;

/**
 * Holds a portion of a stream.
 * 
 * @author simpsons
 */
interface Chunk {
    /**
     * Append bytes to the chunk.
     * 
     * @param buf the array containing the bytes
     * 
     * @param off the offset to the first byte in the array to be added
     * 
     * @param len the maximum number of bytes to be added
     * 
     * @return the number of bytes to be added
     * 
     * @throws IOException if an I/O error occurs
     */
    int write(byte[] buf, int off, int len) throws IOException;

    /**
     * Indicate that no more bytes will be added to the chunk.
     * 
     * @throws IOException if an I/O error occurs
     */
    void complete() throws IOException;

    /**
     * Cause subsequent reads of the content of this chunk to fail with
     * an exception.
     * 
     * @param reason the cause of I/O errors when reading from this
     * chunk
     */
    void abort(Throwable reason);

    /**
     * Get the input stream of the contents of this chunk. All calls of
     * this method on the same object yield the same stream. Reads from
     * this stream block while the chunk is incomplete and all previous
     * contents have already been read. A call to
     * {@link #write(byte[], int, int)} or {@link #complete()} unblock.
     * A call to {@link #abort(Throwable)} causes such reads to throw an
     * exception whose cause is the supplied argument.
     * 
     * @return the input stream
     */
    InputStream getStream();
}
