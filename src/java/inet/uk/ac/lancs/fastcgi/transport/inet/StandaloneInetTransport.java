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

package uk.ac.lancs.fastcgi.transport.inet;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;
import uk.ac.lancs.fastcgi.transport.SocketTransport;

/**
 * Creates connections by accepting sockets from a server socket, and
 * checking the peer's (Internet) address against a configured set of
 * permitted peers.
 * 
 * @author simpsons
 */
class StandaloneInetTransport extends SocketTransport {
    private final Collection<InetAddress> allowedPeers;

    private final String descrPrefix;

    /**
     * Create an Internet-domain transport based on an existing server
     * socket.
     * 
     * @param descrPrefix the string to prefix each connection's public
     * description with
     * 
     * @param socket the socket to accept connections from
     * 
     * @param allowedPeers the set of Internet addresses to accept
     * connections from
     * 
     * @throws ClassCastException if the socket's address is not
     * {@link InetSocketAddress}
     */
    public StandaloneInetTransport(String descrPrefix, ServerSocket socket,
                         Collection<? extends InetAddress> allowedPeers) {
        super(socket);

        /* Force a class-cast exception if the socket is not bound to
         * the right type. */
        @SuppressWarnings("unused")
        InetSocketAddress localAddr =
            (InetSocketAddress) socket.getLocalSocketAddress();

        this.descrPrefix = descrPrefix;
        this.allowedPeers = Set.copyOf(allowedPeers);
    }

    private static final Logger logger =
        Logger.getLogger(StandaloneInetTransport.class.getPackageName());

    /**
     * {@inheritDoc}
     * 
     * @default If the supplied socket's address (obtained by
     * {@link Socket#getInetAddress()}) is not in the configured set of
     * peer addresses, the event is logged, and {@code null} is
     * returned. Otherwise, a public socket description is formed by
     * concatenating the configured prefix with <samp>#</samp> and the
     * peer's host and port.
     */
    @Override
    protected String describe(Socket sock) {
        InetAddress peer = sock.getInetAddress();
        if (!allowedPeers.contains(peer)) {
            logger.warning(() -> String
                .format("rejected connection from %s to %s", peer,
                        sock.getLocalSocketAddress()));
            return null;
        }
        return descrPrefix + "#" + sock.getRemoteSocketAddress();
    }
}
