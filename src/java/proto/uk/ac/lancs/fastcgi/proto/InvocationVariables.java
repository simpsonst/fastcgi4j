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

package uk.ac.lancs.fastcgi.proto;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
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
     * Holds the string identifying IP addresses of legitimate peers.
     * The value is {@value}.
     */
    public static final String WEB_SERVER_ADDRS = "FCGI_WEB_SERVER_ADDRS";

    private static final Pattern COMMA = Pattern.compile(",");

    private static final class LazyAuthorizedInetPeers {
        static final Collection<InetAddress> CACHE;

        static final UnknownHostException EX;

        static final Error ERROR;

        static final RuntimeException RTE;

        private static Collection<InetAddress> get()
            throws UnknownHostException {
            String text = System.getenv(WEB_SERVER_ADDRS);
            if (text == null) return null;
            Collection<InetAddress> result = new HashSet<>();
            for (String item : COMMA.split(text)) {
                InetAddress addr = InetAddress.getByName(item);
                result.add(addr);
            }
            return Set.copyOf(result);
        }

        static {
            Collection<InetAddress> computed = null;
            UnknownHostException ex = null;
            Error error = null;
            RuntimeException rte = null;
            try {
                computed = get();
            } catch (UnknownHostException e) {
                ex = e;
                Logger.getLogger(InvocationVariables.class.getName())
                    .log(Level.SEVERE, "parsing " + WEB_SERVER_ADDRS, e);
            } catch (Error e) {
                error = e;
                Logger.getLogger(InvocationVariables.class.getName())
                    .log(Level.SEVERE, "parsing " + WEB_SERVER_ADDRS, e);
            } catch (RuntimeException e) {
                rte = e;
                Logger.getLogger(InvocationVariables.class.getName())
                    .log(Level.SEVERE, "parsing " + WEB_SERVER_ADDRS, e);
            }
            EX = ex;
            RTE = rte;
            ERROR = error;
            CACHE = computed;
        }
    }

    /**
     * Get the set of IP addresses of legitimate peers as a set of
     * structured data. This reads from the environment variable
     * {@value WEB_SERVER_ADDRS}. The result is cached, so only the
     * first call will actually do anything. An exception in the first
     * call is preserved for other calls, so its stack trace will not be
     * correct.
     * 
     * @return an unmodifiable set of IP addresses; or {@code null} if
     * the variable is not set
     * 
     * @throws UnknownHostException if an element of the variable's
     * value could not be parsed
     */
    public static Collection<InetAddress> getAuthorizedInetPeers()
        throws UnknownHostException {
        if (LazyAuthorizedInetPeers.EX != null)
            throw LazyAuthorizedInetPeers.EX;
        if (LazyAuthorizedInetPeers.ERROR != null)
            throw LazyAuthorizedInetPeers.ERROR;
        if (LazyAuthorizedInetPeers.RTE != null)
            throw LazyAuthorizedInetPeers.RTE;
        return LazyAuthorizedInetPeers.CACHE;
    }
}
