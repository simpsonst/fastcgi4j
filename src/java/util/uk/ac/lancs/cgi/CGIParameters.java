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
     * Names the CGI parameter stating the scheme of the URI. The value
     * is {@value}.
     * 
     * <p>
     * This parameter doesn't seem to have any formal definition, but
     * might be defined in response to the inadequacies of formally
     * defined parameters allowing the full URI of the interaction (the
     * <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875#section-3.3" title
     * ="RFC3875 &sect;3.3">Script-URI</a>) to be formed:
     * 
     * <blockquote>
     * <p>
     * CGI/1.1 provides no generic means for the script to reconstruct
     * [the scheme]&hellip;
     * </p>
     * </blockquote>
     */
    public static final String SCHEME_PARAM = "REQUEST_SCHEME";

    /**
     * Names the CGI parameter providing the query string. The value is
     * {@value}.
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875#section-4.1.7">RFC3875
     * &sect;4.1.7</a>
     */
    public static final String QUERY_STRING_PARAM = "QUERY_STRING";

    /**
     * Names the CGI parameter stating the protocol used between server
     * and client. The value is {@value}.
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875#section-4.1.16">RFC3875
     * &sect;4.1.16</a>
     */
    public static final String SERVER_PROTOCOL_PARAM = "SERVER_PROTOCOL";

    /**
     * Names the CGI parameter stating the name of the server's host
     * through which the client is accessing it. The value is {@value}.
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875#section-4.1.14">RFC3875
     * &sect;4.1.14</a>
     */
    public static final String SERVER_NAME_PARAM = "SERVER_NAME";

    /**
     * Names the CGI parameter stating the port of the server's host
     * through which the client is accessing it. The value is {@value}.
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875#section-4.1.15">RFC3875
     * &sect;4.1.15</a>
     */
    public static final String SERVER_PORT_PARAM = "SERVER_PORT";

    /**
     * Names the CGI parameter indicating the request method. The value
     * is {@value}.
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875/#section-4.1.12">RFC3875
     * &sect;4.1.12</a>
     */
    public static final String METHOD_PARAM = "REQUEST_METHOD";

    /**
     * Names the CGI parameter giving the request body's content type.
     * The value is {@value}.
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875/#section-4.1.3">RFC3875
     * &sect;4.1.3</a>
     */
    public static final String REQUEST_TYPE_PARAM = "CONTENT_TYPE";

    /**
     * Names the CGI parameter giving the length of the request body.
     * The value is {@value}.
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875/#section-4.1.2">RFC3875
     * &sect;4.1.2</a>
     */
    public static final String REQUEST_LENGTH_PARAM = "CONTENT_LENGTH";

    /**
     * Names the CGI parameter containing any sub-path beyond that which
     * identifies the script to be invoked. The value is {@value}.
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875/#section-4.1.5">RFC3875
     * &sect;4.1.5</a>
     */
    public static final String PATH_INFO_PARAM = "PATH_INFO";

    /**
     * Names the CGI parameter identifying the script. The value is
     * {@value}.
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc3875/#section-4.1.13">RFC3875
     * &sect;4.1.13</a>
     */
    public static final String SCRIPT_NAME_PARAM = "SCRIPT_NAME";
}
