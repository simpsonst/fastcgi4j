// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2026, Lancaster University
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
 *  Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.cgi;

/**
 * Holds symbolic constants for the names of standard CGI parameters.
 *
 * @author simpsons
 */
public final class CGIParameters {
    private CGIParameters() {}

    /**
     * The name of the CGI parameter indicating the request method,
     * namely {@value}
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875/#section-4.1.12">RFC3875
     * &sect;4.1.12</a>
     */
    public static final String REQUEST_METHOD_PARAM = "REQUEST_METHOD";

    /**
     * The name of the CGI parameter giving the request body's content
     * type, namely {@value}
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875/#section-4.1.3">RFC3875
     * &sect;4.1.3</a>
     */
    public static final String REQUEST_TYPE_PARAM = "CONTENT_TYPE";

    /**
     * The name of the CGI parameter giving the length of the request
     * body, namely {@value}
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875/#section-4.1.2">RFC3875
     * &sect;4.1.2</a>
     */
    public static final String REQUEST_LENGTH_PARAM = "CONTENT_LENGTH";

    /**
     * The name of the CGI parameter containing any sub-path beyond that
     * which identifies the script to be invoked, namely {@value}
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875/#section-4.1.5">RFC3875
     * &sect;4.1.5</a>
     */
    public static final String PATH_INFO_PARAM = "PATH_INFO";

    /**
     * The name of the CGI parameter identifying the script, namely
     * {@value}
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875/#section-4.1.13">RFC3875
     * &sect;4.1.13</a>
     */
    public static final String SCRIPT_NAME_PARAM = "SCRIPT_NAME";
}
