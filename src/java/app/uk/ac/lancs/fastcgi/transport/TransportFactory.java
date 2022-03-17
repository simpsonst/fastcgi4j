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

package uk.ac.lancs.fastcgi.transport;

/**
 * Determines whether FastCGI connections are arriving over a specific
 * mechanism, and presents them to the application. Implementations
 * should read FastCGI-defined environment variables or look at file
 * descriptor 0 to determine how to receive FastCGI connections. An
 * implementation should need only test for one mechanism, returning
 * {@code null} if not recognized. Implementations should be declared as
 * services for this interface in line with
 * {@link java.util.ServiceLoader}, so they can be enabled simply by
 * adding to the class path.
 * 
 * @see <a href=
 * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S2">FastCGI
 * Specification &mdash; Initial Process State</a>
 * 
 * @author simpsons
 */
public interface TransportFactory {
    /**
     * Get a supply of connections that a FastCGI engine can use.
     * 
     * @return a supply of connections; or {@code null} if none can be
     * provided by the implementation
     * 
     * @throws TransportConfigurationException if a supported transport
     * was positively recognized but cannot be implemented because
     * configuration and environment are incompatible
     */
    Transport getTransport();
}
