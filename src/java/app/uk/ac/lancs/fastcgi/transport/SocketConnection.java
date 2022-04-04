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
import java.net.Socket;

/**
 * Implements a FastCGI connection over a socket.
 * 
 * @author simpsons
 */
public class SocketConnection implements Connection {
    /**
     * Holds the socket over which the FastCGI connection is
     * implemented.
     */
    protected final Socket socket;

    private final String descr;

    private final String intDescr;

    /**
     * Create a FastCGI connection from a socket.
     * 
     * @param socket the socket over which the connection is implemented
     * 
     * @param descr a diagnostic description of this connection,
     * excluding sensitive information
     * 
     * @param intDescr sensitive information describing this connection
     */
    public SocketConnection(Socket socket, String descr, String intDescr) {
        this.socket = socket;
        this.descr = descr;
        this.intDescr = intDescr;
    }

    /**
     * {@inheritDoc}
     * 
     * @default {@link Socket#getInputStream()} is invoked on the
     * supplied socket, and the result is returned.
     */
    @Override
    public InputStream getInput() throws IOException {
        return socket.getInputStream();
    }

    /**
     * {@inheritDoc}
     * 
     * @default {@link Socket#getOutputStream()} is invoked on the
     * supplied socket, and the result returned.
     */
    @Override
    public OutputStream getOutput() throws IOException {
        return socket.getOutputStream();
    }

    /**
     * {@inheritDoc}
     * 
     * @default {@link Socket#close()} is invoked on the supplied
     * socket.
     */
    @Override
    public void close() throws IOException {
        socket.close();
    }

    /**
     * {@inheritDoc}
     * 
     * @default The value returned is the second argument of the
     * constructor.
     */
    @Override
    public String description() {
        return descr;
    }

    /**
     * {@inheritDoc}
     * 
     * @default The value returned is the third argument of the
     * constructor.
     */
    @Override
    public String internalDescription() {
        return intDescr;
    }
}
