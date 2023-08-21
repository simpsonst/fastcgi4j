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

package uk.ac.lancs.fastcgi.transport.inherit;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.logging.Logger;
import jdk.net.ExtendedSocketOptions;
import jdk.net.UnixDomainPrincipal;
import uk.ac.lancs.fastcgi.proto.InvocationVariables;
import uk.ac.lancs.fastcgi.transport.SocketChannelTransport;
import uk.ac.lancs.fastcgi.transport.Transport;
import uk.ac.lancs.fastcgi.transport.TransportConfigurationException;
import uk.ac.lancs.fastcgi.transport.TransportFactory;
import uk.ac.lancs.scc.jardeps.Service;

/**
 * Builds a transport from the channel inherited from the entity that
 * started the JVM. The channel must be based on a server socket, or the
 * factory will ignore it. Each connection's description is
 * {@value #UNIX_DESCRIPTION} if a Unix-domain socket is obtained, or
 * {@value #INET_DESCRIPTION_PREFIX} plus the peer address for an
 * Internet-domain socket.
 * 
 * <p>
 * In the Internet domain, {@value InvocationVariables#WEB_SERVER_ADDRS}
 * is consulted via {@link InvocationVariables#getAuthorizedInetPeers()}
 * to get a list of IP addresses that a client may connect from.
 * 
 * <p>
 * In the Unix domain, {@value InvocationVariables#LEGIT_PEERS} is
 * consulted via
 * {@link InvocationVariables#getAuthorizedStandalonePrincipals()} to
 * get a list of Unix-domain principals that may connect as clients.
 * This non-standard variable is consulted, as there shouldn't be a peer
 * list for Unix-domain sockets. However, this allows for a stand-alone
 * application to inherit from an initiating process which creates the
 * socket itself, and fails early if the bind address is already in use.
 * 
 * @author simpsons
 */
@Service(TransportFactory.class)
public class InheritedChannelTransportFactory implements TransportFactory {
    @Override
    public Transport getTransport() {
        try {
            Channel ic = System.inheritedChannel();
            if (ic == null) return null;
            if (ic instanceof ServerSocketChannel ssc) {
                SocketAddress addr = ssc.getLocalAddress();
                if (addr instanceof UnixDomainSocketAddress) {
                    final var permittedCallers =
                        InvocationVariables.getAuthorizedStandalonePrincipals();
                    Predicate<UnixDomainPrincipal> peerOkay =
                        permittedCallers == null ? principal -> true :
                            principal -> {
                                return permittedCallers.stream()
                                    .anyMatch(t -> t.test(principal));
                            };
                    return new SocketChannelTransport(ssc) {
                        @Override
                        protected String describe(SocketChannel sock)
                            throws IOException {
                            UnixDomainPrincipal principal = sock
                                .getOption(ExtendedSocketOptions.SO_PEERCRED);
                            if (!peerOkay.test(principal)) {
                                logger.warning(() -> String
                                    .format("rejected connection from %s",
                                            principal));
                                return null;
                            }
                            String result = UNIX_DESCRIPTION;
                            logger.info(() -> String
                                .format("accepted connection to %s as %s", ssc,
                                        result));
                            return result;
                        }
                    };
                } else if (addr instanceof InetSocketAddress) {
                    final Collection<InetAddress> permittedCallers =
                        InvocationVariables.getAuthorizedInetPeers();
                    return new SocketChannelTransport(ssc) {
                        @Override
                        protected String describe(SocketChannel sock)
                            throws IOException {
                            InetSocketAddress peer =
                                (InetSocketAddress) sock.getRemoteAddress();
                            if (!permittedCallers.contains(peer.getAddress()))
                                return null;
                            String result = INET_DESCRIPTION_PREFIX + peer;
                            logger.info(() -> String
                                .format("accepted connection to %s as %s", ssc,
                                        result));
                            return result;
                        }
                    };
                }
            }
            return null;
        } catch (IOException ex) {
            throw new TransportConfigurationException("inherited channel", ex);
        }
    }

    private static final String UNIX_DESCRIPTION = "inherited-unix";

    private static final String INET_DESCRIPTION_PREFIX = "inherited-inet-";

    private static final Logger logger = Logger
        .getLogger(InheritedChannelTransportFactory.class.getPackageName());
}
