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

package uk.ac.lancs.fastcgi.transport.unix;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import jdk.net.ExtendedSocketOptions;
import jdk.net.UnixDomainPrincipal;
import uk.ac.lancs.fastcgi.proto.InvocationVariables;
import uk.ac.lancs.fastcgi.proto.PrincipalRequirement;
import uk.ac.lancs.fastcgi.transport.SocketChannelTransport;
import uk.ac.lancs.fastcgi.transport.Transport;
import uk.ac.lancs.fastcgi.transport.TransportConfigurationException;
import uk.ac.lancs.fastcgi.transport.TransportFactory;
import uk.ac.lancs.scc.jardeps.Service;

/**
 * Recognizes stand-alone Unix-domain transports. The environment
 * variable {@value InvocationVariables#UNIX_BIND_ADDR} must be set,
 * specifying the file of the rendezvous point. If the environment
 * variable {@value InvocationVariables#LEGIT_PEERS} is set, it must be
 * a comma-separated list of legitimate principals, each of the form
 * <samp><var>user</var></samp>, <samp>&#64;<var>group</var></samp> or
 * <samp><var>user</var>&#64;<var>group</var></samp>, and only peers
 * calling with at least one of those identities will be accepted.
 * 
 * <p>
 * Each connection's description is {@value #STANDALONE_DESCRIPTION}.
 * 
 * @author simpsons
 */
@Service(TransportFactory.class)
public class StandaloneUnixTransportFactory implements TransportFactory {

    @Override
    public Transport getTransport() {
        try {
            String pathText = System.getenv(InvocationVariables.UNIX_BIND_ADDR);
            if (pathText == null) return null;

            /* Determine who is allowed to connect. */
            Collection<PrincipalRequirement> allowedPeers =
                InvocationVariables.getAuthorizedStandalonePrincipals();
            Predicate<UnixDomainPrincipal> peerOkay =
                allowedPeers == null ? principal -> true : principal -> {
                    return allowedPeers.stream()
                        .anyMatch(t -> t.test(principal));
                };

            /* Bind to the configured path. */
            Path path = Paths.get(pathText);
            UnixDomainSocketAddress addr = UnixDomainSocketAddress.of(path);
            final ServerSocketChannel ssc =
                ServerSocketChannel.open(StandardProtocolFamily.UNIX);
            ssc.bind(addr);

            /* Ensure the rendezvous point is accessible by all (if we
             * have a set of allowed peers), and delete it on exit. */
            if (allowedPeers != null) {
                Set<PosixFilePermission> perms =
                    PosixFilePermissions.fromString("rwxrwxrwx");
                Files.setPosixFilePermissions(path, perms);
            }
            path.toFile().deleteOnExit();

            /* Build a transport out of the server socket that checks
             * and names each accepted connection. */
            return new SocketChannelTransport(ssc) {
                @Override
                protected String describe(SocketChannel channel)
                    throws IOException {
                    UnixDomainPrincipal principal =
                        channel.getOption(ExtendedSocketOptions.SO_PEERCRED);
                    if (!peerOkay.test(principal)) {
                        logger.warning(() -> String
                            .format("rejected connection from %s to %s",
                                    principal, path));
                        return null;
                    }
                    String result = STANDALONE_DESCRIPTION;
                    logger.info(() -> String
                        .format("accepted connection from %s to %s as %s",
                                principal, path, result));
                    return result;
                }
            };
        } catch (IOException ex) {
            throw new TransportConfigurationException(ex);
        }
    }

    private static final String STANDALONE_DESCRIPTION = "unix-standalone";

    private static final Logger logger =
        Logger.getLogger(StandaloneUnixTransportFactory.class.getPackageName());
}
