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

package uk.ac.lancs.fastcgi.proto;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Defines constants for names of environment variables pertaining to
 * the connection between server and application.
 * 
 * @author simpsons
 * 
 * @see <a href=
 * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S2.3">FastCGI
 * Specification &mdash; Environment variables</a>
 */
public final class InvocationVariables {
    private InvocationVariables() {}

    /**
     * Specifies the name of the environment variable identifying IP
     * addresses of legitimate peers. The variable is called {@value}.
     */
    public static final String WEB_SERVER_ADDRS = "FCGI_WEB_SERVER_ADDRS";

    private static final Pattern COMMA = Pattern.compile(",");

    /**
     * Holds lazily computed sets of Unix-domain principals, indexed by
     * the variable name from which they are derived. Use only with
     * {@link Map#computeIfAbsent(Object, java.util.function.Function)}
     * to ensure consistency.
     */
    private static final Map<String,
                             Supplier<Collection<PrincipalRequirement>>> permittedPrincipals =
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
        getAuthorizedPrincipals(String varName) {
        Supplier<Collection<PrincipalRequirement>> result = permittedPrincipals
            .computeIfAbsent(varName,
                             InvocationVariables::parsePrincipalsEnvironmentResult);
        return result.get();
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

    /**
     * Get the set of legitimate principals from an environment
     * variable, and package the result or an exception.
     *
     * @param varName the variable name
     *
     * @return the packaged result or exception
     */
    private static Supplier<Collection<PrincipalRequirement>>
        parsePrincipalsEnvironmentResult(String varName) {
        try {
            Collection<PrincipalRequirement> value =
                parsePrincipalsEnvironment(varName);
            logger.info(() -> String.format("UNIX peers for %s detected as %s",
                                            varName, value));
            return () -> value;
        } catch (IllegalArgumentException ex) {
            return () -> {
                throw ex;
            };
        }
    }

    /**
     * Get the set of legitimate principals. This reads from the
     * environment variable {@value #LEGIT_PEERS}. The result is cached,
     * so only the first call will actually do anything.
     *
     * @return an unmodifiable set of legitimate principals; or
     * {@code null} if the variable is not set
     *
     * @throws IllegalArgumentException if an element of the variable's
     * value could not be parsed
     */
    public static Collection<PrincipalRequirement>
        getAuthorizedStandalonePrincipals() {
        return getAuthorizedPrincipals(LEGIT_PEERS);
    }

    /**
     * Get the set of IP addresses of legitimate peers as a set of
     * structured data. This reads from the environment variable
     * {@value #WEB_SERVER_ADDRS}. The result is cached, so only the
     * first call will actually do anything. An exception in the first
     * call is preserved for other calls, so its stack trace will not be
     * correct.
     * 
     * @return an unmodifiable set of IP addresses; or {@code null} if
     * the variable is not set
     * 
     * @throws UnknownHostException if an element of the variable's
     * value could not be parsed
     * 
     * @see #getAuthorizedStandaloneInetPeers()
     */
    public static Collection<InetAddress> getAuthorizedInetPeers()
        throws UnknownHostException {
        return getAuthorizedInetPeersFromVariable(WEB_SERVER_ADDRS);
    }

    /**
     * Get the set of IP addresses of legitimate peers as a set of
     * structured data. This reads from the environment variable
     * {@value #LEGIT_PEERS}, which is only to be used for the Internet
     * domain when operating in stand-alone mode. (Use
     * {@link #getAuthorizedInetPeers()} in forked mode.) The result is
     * cached, so only the first call will actually do anything. An
     * exception in the first call is preserved for other calls, so its
     * stack trace will not be correct.
     * 
     * @return an unmodifiable set of IP addresses; or {@code null} if
     * the variable is not set
     * 
     * @throws UnknownHostException if an element of the variable's
     * value could not be parsed
     * 
     * @see #getAuthorizedInetPeers()
     */
    public static Collection<InetAddress> getAuthorizedStandaloneInetPeers()
        throws UnknownHostException {
        return getAuthorizedInetPeersFromVariable(LEGIT_PEERS);
    }

    /**
     * Convert a comma-separated string into a set of Internet
     * addresses. Each item is converted with
     * {@link InetAddress#getByName(String)}.
     * 
     * @param text the text to be converted
     * 
     * @return the set of Internet addresses
     * 
     * @throws UnknownHostException if an item did not parse as an IP
     * address, nor resolve as a host name
     */
    private static Collection<InetAddress> parsePeers(String text)
        throws UnknownHostException {
        Collection<InetAddress> result = new HashSet<>();
        for (String item : COMMA.split(text)) {
            InetAddress addr = InetAddress.getByName(item);
            result.add(addr);
        }
        return Set.copyOf(result);
    }

    /**
     * Read an environment variable, and parse its value as a
     * comma-separated string of Internet addresses.
     * 
     * @param varName the variable name
     * 
     * @return the set of Internet addresses in the variable's value
     * 
     * @throws UnknownHostException if an item did not parse as an IP
     * address, nor resolve as a host name
     */
    private static Collection<InetAddress> parsePeersEnvironment(String varName)
        throws UnknownHostException {
        String text = System.getenv(varName);
        return parsePeers(text);
    }

    /**
     * Get the set of Internet addresses from an environment variable,
     * and package the result or an exception.
     * 
     * @param varName the variable name
     * 
     * @return the packaged result or exception
     */
    private static PeersResult parsePeersEnvironmentResult(String varName) {
        try {
            Collection<InetAddress> value = parsePeersEnvironment(varName);
            logger.info(() -> String.format("INET peers for %s detected as %s",
                                            varName, value));
            return () -> value;
        } catch (UnknownHostException ex) {
            return () -> {
                throw ex;
            };
        }
    }

    /**
     * Holds a set of Internet addresses, or the exception thrown when
     * attempting to obtain them.
     */
    private interface PeersResult {
        /**
         * Get the set of addresses.
         * 
         * @return the set of addresses
         * 
         * @throws UnknownHostException if an item from which the
         * addresses were to be generated did not parse as an IP
         * address, nor resolve as a host name
         */
        Collection<InetAddress> get() throws UnknownHostException;
    }

    /**
     * Holds lazily computed sets of Internet addresses, indexed by the
     * variable name from which they are derived. Use only with
     * {@link Map#computeIfAbsent(Object, java.util.function.Function)}
     * to ensure consistency.
     */
    private static final Map<String, PeersResult> permittedPeers =
        new ConcurrentHashMap<>();

    /**
     * Get a set of Internet addresses parsed from the comma-separated
     * value of an environment variable, caching the value.
     * 
     * @param varName the variable name
     * 
     * @return the set of Internet addresses
     * 
     * @throws UnknownHostException if an item from which the addresses
     * were to be generated did not parse as an IP address, nor resolve
     * as a host name
     */
    private static Collection<InetAddress>
        getAuthorizedInetPeersFromVariable(String varName)
            throws UnknownHostException {
        PeersResult result = permittedPeers
            .computeIfAbsent(varName,
                             InvocationVariables::parsePeersEnvironmentResult);
        return result.get();
    }

    /**
     * Specifies the name of the environment variable identifying IP
     * addresses of legitimate peers (Internet-domain, when running in
     * stand-alone mode) or names of legitimate users/groups
     * (Unix-domain). The variable is called {@value}.
     * 
     * <p>
     * The value of the variable is a comma-separated list. For the
     * Internet domain, each item is either an IP address or a hostname
     * to be resolved as such. For the Unix domain, each item is of the
     * form accepted by {@link PrincipalRequirement#of(String)}, i.e.,
     * <samp><var>user</var></samp>, <samp>@<var>group</var></samp> or
     * <samp><var>user</var>@<var>group</var></samp>.
     */
    public static final String LEGIT_PEERS = "FASTCGI4J_WEB_SERVER_ADDRS";

    /**
     * Specifies the name of the environment variable instructing the
     * application process to bind to an Internet-domain socket address.
     * The variable is called {@value}.
     */
    public static final String INET_BIND_ADDR = "FASTCGI4J_INET_BIND";

    /**
     * Specifies the name of the environment variable instructing the
     * application process to bind to a Unix-domain socket address. The
     * variable is called {@value}.
     */
    public static final String UNIX_BIND_ADDR = "FASTCGI4J_UNIX_BIND";

    /**
     * Get the address that a stand-alone application process should
     * bind to.
     * 
     * @return the address to bind to; or {@code null} if this process
     * should not be running stand-alone
     * 
     * @throws IllegalArgumentException if the bind address does not end
     * with a colon-delimited port number
     * 
     * @throws NumberFormatException if the text after the last colon is
     * not a decimal integer
     * 
     * @throws UnknownHostException if the text before the last colon
     * cannot be parsed as a host name or IP address
     */
    public static InetSocketAddress getInetBindAddress()
        throws UnknownHostException {
        String value = System.getenv(INET_BIND_ADDR);
        if (value == null) return null;
        int colon = value.lastIndexOf(':');
        if (colon < 0)
            throw new IllegalArgumentException("no port in " + value);

        String portText = value.substring(colon + 1);
        String hostText = value.substring(0, colon);
        int port = Integer.parseInt(portText);
        InetAddress host = InetAddress.getByName(hostText);
        var result = new InetSocketAddress(host, port);
        logger.info(() -> String.format("stand-alone INET bind detected as %s",
                                        result));
        return result;
    }

    private static final Logger logger =
        Logger.getLogger(InvocationVariables.class.getPackageName());
}
