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

package uk.ac.lancs.fastcgi.proto;

/**
 * Identifies names of application variables.
 * 
 * @author simpsons
 * 
 * @see <a href=
 * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S4.1">FastCGI
 * Specification &mdash; <samp>FCGI_GET_VALUES</samp>,
 * <samp>FCGI_GET_VALUES_RESULT</samp></a>
 */
public final class ApplicationVariables {
    private ApplicationVariables() {}

    /**
     * Identifies the variable that specifies the maximum number of
     * connections that the application will accept. The value is
     * {@value}.
     */
    public static final String MAX_CONNS = "FCGI_MAX_CONNS";

    /**
     * Identifies the variable that specifies the maximum number of
     * concurrent requests that the application will accept. The value
     * is {@value}.
     */
    public static final String MAX_REQS = "FCGI_MAX_REQS";

    /**
     * Identifies the variable that specifies whether the application
     * multiplexes requests on a single connection. The value is
     * {@value}.
     */
    public static final String MPXS_CONNS = "FCGI_MPXS_CONNS";

    /**
     * Identifies the variable that specifies how the application
     * expects to receive and send protocol fields.
     * 
     * <p>
     * The value must be a comma-separated list of tokens. The following
     * tokens are recognized:
     * 
     * <dl>
     * 
     * <dt>{@value #FIELD_HANDLING_REQUEST_TRAILER}</dt>
     * 
     * <dd>
     * <p>
     * The application will recognize the flag
     * {@link RequestFlags#EXPECT_TRAILER}, allowing it to receive the
     * request trailer as a separate {@link RecordTypes#PARAMS} sequence
     * after {@link RecordTypes#STDIN}. The web server must not set flag
     * if this token is missing.
     * 
     * </dl>
     * 
     * <p>
     * This is an experimental extension to FastCGI/1.0.
     */
    public static final String FIELD_HANDLING = "FCGI_FIELD_HANDLING";

    /**
     * Identifies an application capable of understanding the flag
     * {@link RequestFlags#EXPECT_TRAILER}.
     */
    public static final String FIELD_HANDLING_REQUEST_TRAILER =
        "request-trailer";
}
