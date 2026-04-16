// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

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

package uk.ac.lancs.fastcgi.context;

import java.util.Collections;
import java.util.Map;

/**
 * Presents the context of a FastCGI session to an application in the
 * Responder role.
 * 
 * @author simpsons
 * 
 * @see <a href=
 * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S6.2">FastCGI
 * Specification &mdash; Responder</a>
 */
public interface ResponderSession extends RequestableSession {
    /**
     * Get the trailing parameters. These can only be received after EOF
     * has been delivered on the request body stream to the application,
     * as it arrives after closing the request body. An implementation
     * could block if called too early, but then it would have to buffer
     * the remainder of the stream to avoid deadlock. For these reasons,
     * implementations of this method have the option of throwing a
     * run-time exception if called too early, rather than blocking. The
     * application is therefore responsible for avoiding this condition
     * by not attempting the call until it has received the body itself.
     * 
     * <p>
     * This is considered an experimental extension to the FastCGI
     * specification. Because CGI (and therefore FastCGI) requires that
     * the server present the <samp>CONTENT_LENGTH</samp> parameter to
     * the application, the server is forced to buffer the entire
     * request body if the client sends it over HTTP/1.1 with
     * <samp>chunked</samp> transfer encoding, which precludes the
     * sending of a <samp>Content-Length</samp> header field from which
     * that parameter is derived. However, in HTTP/2, chunking is
     * obviated as a side-effect of its stream multiplexing, so a client
     * can effectively chunk the request, send a trailer (only possible
     * in HTTP/1.1 with chunking), and provide a
     * <samp>Content-Length</samp>. This should allow a server to invoke
     * the application before it has received the entire request, and
     * the FastCGI specification could be extended by defining a flag in
     * the <samp>FCGI_BEGIN_REQUEST</samp> record indicating that a
     * second <samp>FCGI_PARAM</samp> cluster will follow the
     * termination of the <samp>FCGI_STDIN</samp> stream. There should
     * also be a capability test performed by the server on the
     * application before setting the bit, so that it can fall back to
     * receiving the entire request and merging the trailer into the
     * initial parameters when dealing with applications and libraries
     * unaware of this facility.
     * 
     * @return an immutable set of parameters
     * 
     * @throws IllegalStateException if called before EOF has been
     * delivered on {@link RequestableSession#in()}, when a trailer is
     * expected
     * 
     * @default By default, this method returns an empty map.
     */
    default Map<String, String> trailingParameters() {
        return Collections.emptyMap();
    }
}
