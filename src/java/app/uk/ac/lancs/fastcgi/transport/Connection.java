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

package uk.ac.lancs.fastcgi.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides bidirectional byte-stream communication with the server. The
 * streams returned by {@link #input()} and {@link #output()} do not
 * have to be individually thread-safe, but it must be possible to use
 * both simultaneously on separate threads.
 * 
 * @author simpsons
 */
public interface Connection {
    /**
     * Get the stream of bytes from the server.
     * 
     * @return the stream of bytes from the server
     * 
     * @throws IOException if an I/O error occurs
     */
    InputStream input() throws IOException;

    /**
     * Get the stream of bytes to the server.
     * 
     * @return the stream of bytes to the server
     * 
     * @throws IOException if an I/O error occurs
     */
    OutputStream output() throws IOException;

    /**
     * Close the connection.
     * 
     * @throws IOException if an I/O error occurs
     */
    void close() throws IOException;

    /**
     * Get a diagnostic description of this connection.
     * 
     * @return the connection description
     */
    String description();

    /**
     * Get the sensitive parts of a diagnostic description of this
     * connection. This should not repeat parts mentioned by
     * {@link #description()}.
     * 
     * @return the sensitive connection description
     */
    String internalDescription();

    /**
     * Identify the implementation of this connection as an overarching
     * package.
     * 
     * @return the package identifying the implementation
     * 
     * @default By default, this method calls {@link Object#getClass()}
     * on its receiver, and then {@link Class#getPackage()} on the
     * class, yielding the result.
     */
    default Package implementation() {
        return this.getClass().getPackage();
    }
}
