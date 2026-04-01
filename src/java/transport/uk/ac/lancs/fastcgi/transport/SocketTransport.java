/*
 * Copyright (c) 2022,2023, Lancaster University
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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.logging.Logger;

/**
 * Creates connections by accepting sockets from a server socket.
 * 
 * @author simpsons
 */
public abstract class SocketTransport implements Transport {
    /**
     * Holds the socket from which connections are accepted.
     */
    protected final ServerSocket socket;

    private final String intDescr;

    /**
     * Create a transport based on a server socket.
     * 
     * @param socket the socket from which connections will be accepted
     */
    public SocketTransport(ServerSocket socket) {
        this.socket = socket;
        this.intDescr = socket.getLocalSocketAddress().toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @default Repeatedly, {@link ServerSocket#accept()} is invoked on
     * the configured socket. The new socket is submitted to
     * {@link #describe(Socket)}. If this returns {@code null}, the
     * socket is closed. Otherwise, the result is used as the public
     * diagnostic description of a new {@link SocketConnection} built
     * from the new socket. Its internal description is the server
     * socket's local address.
     */
    @Override
    public Connection nextConnection() throws IOException {
        do {
            Socket sock = socket.accept();
            SocketAddress peer = sock.getRemoteSocketAddress();
            logger.fine(() -> msg("accepted from %s", peer));
            String descr = describe(sock);
            if (descr == null) {
                sock.close();
                continue;
            }
            return new SocketConnection(sock, descr, intDescr);
        } while (true);
    }

    /**
     * Determine whether to build a connection from a socket, and how to
     * describe it.
     * 
     * @param sock the socket to be tested
     * 
     * @return a public description of the socket; or {@code null} if
     * the connection is to be rejected
     */
    protected abstract String describe(Socket sock);

    private String msg(String fmt, Object... args) {
        return String.format(fmt, args);
    }

    private static final Logger logger =
        Logger.getLogger(SocketTransport.class.getPackageName());
}
