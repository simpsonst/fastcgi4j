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
import java.util.regex.Pattern;
import jdk.net.ExtendedSocketOptions;
import jdk.net.UnixDomainPrincipal;
import uk.ac.lancs.fastcgi.proto.InvocationVariables;
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
    private static final Pattern COMMA = Pattern.compile(",");

    /**
     * Holds a set of principal requirements, or the exception thrown
     * when attempting to obtain them.
     */
    private interface PrincipalsResult {
        /**
         * Get the set of principal requirements.
         * 
         * @return the set of requirements
         * 
         * @throws IllegalArgumentException if an item from which the
         * requirements were to be generated did not parse as a
         * requirement
         */
        Collection<PrincipalRequirement> get();
    }

    /**
     * Holds lazily computed sets of Unix-domain principals, indexed by
     * the variable name from which they are derived. Use only with
     * {@link Map#computeIfAbsent(Object, java.util.function.Function)}
     * to ensure consistency.
     */
    private static final Map<String, PrincipalsResult> permittedPrincipals =
        new ConcurrentHashMap<>();

    /**
     * Get a set of principal requirements parsed from the
     * comma-separated value of an environment variable, caching the
     * value.
     *
     * @param varName the variable name
     *
     * @return the set of principal requirements
     *
     * @throws IllegalArgumentException if an item from which the
     * requirements were to be generated did not parse as a requirement
     */
    private static Collection<PrincipalRequirement>
        getPermittedPrincipals(String varName) {
        PrincipalsResult result = permittedPrincipals
            .computeIfAbsent(varName,
                             StandaloneUnixTransportFactory::parsePrincipalsEnvironmentResult);
        return result.get();
    }

    /**
     * Get the set of legitimate principals. This reads from the
     * environment variable {@value #INET_SERVER_ADDRS}. The result is
     * cached, so only the first call will actually do anything.
     *
     * @return an unmodifiable set of legitimate principals; or
     * {@code null} if the variable is not set
     *
     * @throws IllegalArgumentException if an element of the variable's
     * value could not be parsed
     */
    private static Collection<PrincipalRequirement>
        getAuthorizedStandalonePrincipals() {
        return getPermittedPrincipals(InvocationVariables.LEGIT_PEERS);
    }

    /**
     * Convert a comma-separated string into a set of legitimate
     * principals. Each item is converted by
     * {@link PrincipalRequirement#of(String)}.
     *
     * @param text the text to be converted
     *
     * @return the set of legitimate principals
     *
     * @throws IllegalArgumentException if an item did not parse as a
     * principal requirement
     */
    private static Collection<PrincipalRequirement>
        parsePrincipals(String text) {
        if (text == null) return null;
        Collection<PrincipalRequirement> result = new HashSet<>();
        for (String item : COMMA.split(text)) {
            PrincipalRequirement req = PrincipalRequirement.of(item);
            result.add(req);
        }
        return Set.copyOf(result);
    }

    /**
     * Get the set of legitimate principals from an environment
     * variable, and package the result or an exception.
     *
     * @param varName the variable name
     *
     * @return the packaged result or exception
     */
    private static PrincipalsResult
        parsePrincipalsEnvironmentResult(String varName) {
        try {
            Collection<PrincipalRequirement> value =
                parsePrincipalsEnvironment(varName);
            return () -> value;
        } catch (IllegalArgumentException ex) {
            return () -> {
                throw ex;
            };
        }
    }

    /**
     * Read an environment variable, and parse its value as a
     * comma-separated string of principal requirements.
     *
     * @param varName the variable name
     *
     * @return the set of principal requirements in the variable's value
     *
     * @throws IllegalArgumentException if an item did not parse as a
     * principal requirement
     *
     * @see PrincipalRequirement
     */
    private static Collection<PrincipalRequirement>
        parsePrincipalsEnvironment(String varName) {
        String text = System.getenv(varName);
        return parsePrincipals(text);
    }

    @Override
    public Transport getTransport() {
        try {
            String pathText = System.getenv(InvocationVariables.UNIX_BIND_ADDR);
            if (pathText == null) return null;

            /* Determine who is allowed to connect. */
            Collection<PrincipalRequirement> allowedPeers =
                getAuthorizedStandalonePrincipals();
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
            if (allowedPeers == null) {
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
                    if (!peerOkay.test(principal)) return null;
                    return STANDALONE_DESCRIPTION;
                }
            };
        } catch (IOException ex) {
            throw new TransportConfigurationException(ex);
        }
    }

    private static final String STANDALONE_DESCRIPTION = "unix-standalone";
}
