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

package uk.ac.lancs.fastcgi;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Presents the context of a FastCGI session to an application in a role
 * which receives a request body.
 *
 * @author simpsons
 */
public interface RequestableSession extends Session {
    /**
     * Get the stream for reading the request body.
     * 
     * @return the input stream providing the request body
     */
    InputStream in();

    /**
     * Get the request trailer.
     * 
     * <p>
     * This can only be received after EOF has been delivered on the
     * request body stream to the application, as it arrives after
     * closing the request body. An implementation could block if called
     * too early, but then it would have to buffer the remainder of the
     * stream to avoid deadlock. For these reasons, implementations of
     * this method have the option of throwing a run-time exception if
     * called too early, rather than blocking. The application is
     * therefore responsible for avoiding this condition by not
     * attempting the call until it has received the body itself.
     * 
     * <p>
     * Request trailer field names are not encoded like the main request
     * parameters. There is no upper-casing of the names, nor any
     * translation from dashes to underscores.
     * 
     * <p>
     * This is an experimental extension to FastCGI/1.0. It will only be
     * enabled if the server requests an application value
     * {@value uk.ac.lancs.fastcgi.proto.ApplicationVariables#FIELD_HANDLING},
     * and recognizes the token
     * {@value uk.ac.lancs.fastcgi.proto.ApplicationVariables#FIELD_HANDLING_REQUEST_TRAILER}
     * in response.
     * 
     * @return an immutable set of trailer fields
     * 
     * @throws IllegalStateException if called before EOF has been
     * delivered on {@link RequestableSession#in()}, when a trailer is
     * expected
     * 
     * @throws InterruptedException if interrupted while waiting for the
     * trailer to be received
     * 
     * @default By default, this method returns an empty map.
     */
    default Map<String, List<String>> requestTrailer()
        throws InterruptedException {
        return Collections.emptyMap();
    }
}
