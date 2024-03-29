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
 * Holds constants for distinct record types.
 * 
 * @see <a href=
 * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S8">FastCGI
 * Specification &mdash; Types and Constants</a> of the FastCGI
 * specification
 * 
 * @author simpsons
 */
public final class RecordTypes {
    private RecordTypes() {}

    /**
     * Indicates that a request is being opened by the server. The value
     * is {@value}, corresponding to <code>FCGI_BEGIN_REQUEST</code>.
     * 
     * @see <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S5.1">FastCGI
     * Specification &mdash; <code>FCGI_BEGIN_REQUEST</code></a>
     */
    public static final byte BEGIN_REQUEST = 1;

    /**
     * Indicates that a request is being aborted by the server. The
     * value is {@value}, corresponding to
     * <code>FCGI_ABORT_REQUEST</code>.
     * 
     * @see <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S5.4">FastCGI
     * Specification &mdash; <code>FCGI_ABORT_REQUEST</code></a>
     */
    public static final byte ABORT_REQUEST = 2;

    /**
     * Indicates that the application is terminating a request. The
     * value is {@value}, corresponding to
     * <code>FCGI_END_REQUEST</code>. Values in {@link ProtocolStatuses}
     * indicate the reason.
     * 
     * @see <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S5.5">FastCGI
     * Specification &mdash; <code>FCGI_END_REQUEST</code></a>
     */
    public static final byte END_REQUEST = 3;

    /**
     * Indicates that the content is part of the server-to-application
     * parameters stream. The value is {@value}, corresponding to
     * <code>FCGI_PARAMS</code>.
     * 
     * @see <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S5.2">FastCGI
     * Specification &mdash; <code>FCGI_PARAMS</code></a>
     */
    public static final byte PARAMS = 4;

    /**
     * Indicates that the content is part of the server-to-application
     * standard input. The value is {@value}, corresponding to
     * <code>FCGI_STDIN</code>.
     * 
     * @see <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S5.3">FastCGI
     * Specification &mdash; <code>FCGI_STDIN</code></a>
     */
    public static final byte STDIN = 5;

    /**
     * Indicates that the content is part of the application-to-server
     * standard output. The value is {@value}, corresponding to
     * <code>FCGI_STDOUT</code>.
     * 
     * @see <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S5.3">FastCGI
     * Specification &mdash; <code>FCGI_STDOUT</code></a>
     */
    public static final byte STDOUT = 6;

    /**
     * Indicates that the content is part of the application-to-server
     * standard error output. The value is {@value}, corresponding to
     * <code>FCGI_STDERR</code>.
     * 
     * @see <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S5.3">FastCGI
     * Specification &mdash; <code>FCGI_STDERR</code></a>
     */
    public static final byte STDERR = 7;

    /**
     * Indicates that the content is part of the server-to-application
     * extra data. The value is {@value}, corresponding to
     * <code>FCGI_DATA</code>.
     * 
     * @see <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S5.3">FastCGI
     * Specification &mdash; <code>FCGI_DATA</code></a>
     */
    public static final byte DATA = 8;

    /**
     * Indicates that the content is a list of application variable
     * names requested by the server. The value is
     * {@value}, corresponding to <code>FCGI_GET_VALUES</code>. See
     * {@link ApplicationVariables} for standard variable names.
     * 
     * @see <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S4.1">FastCGI
     * Specification &mdash; <code>FCGI_GET_VALUES</code></a>
     */
    public static final byte GET_VALUES = 9;

    /**
     * Indicates that the content is a list of application variables and
     * values. The value is {@value}, corresponding to
     * <code>FCGI_GET_VALUES_RESULT</code>. See
     * {@link ApplicationVariables} for standard variable names.
     * 
     * @see <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S4.1">FastCGI
     * Specification &mdash; <code>FCGI_GET_VALUES_RESULT</code></a>
     */
    public static final byte GET_VALUES_RESULT = 10;

    /**
     * Indicates that a record type from the server was not understood
     * by the application. The value is {@value}, corresponding to
     * <code>FCGI_UNKNOWN_TYPE</code>.
     * 
     * @see <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S4.2">FastCGI
     * Specification &mdash; <code>FCGI_UNKNOWN_TYPE</code></a>
     */
    public static final byte UNKNOWN_TYPE = 11;

    /**
     * Get a string representation of a record type.
     * 
     * @param type the record type
     * 
     * @return the string representation, including the numeric value if
     * unknown
     */
    public static String toString(int type) {
        return switch (type) {
        case ABORT_REQUEST -> "ABORT_REQUEST";
        case BEGIN_REQUEST -> "BEGIN_REQUEST";
        case DATA -> "DATA";
        case END_REQUEST -> "END_REQUEST";
        case GET_VALUES -> "GET_VALUES";
        case GET_VALUES_RESULT -> "GET_VALUES_RESULT";
        case PARAMS -> "PARAMS";
        case STDERR -> "STDERR";
        case STDIN -> "STDIN";
        case STDOUT -> "STDOUT";
        case UNKNOWN_TYPE -> "UNKNOWN_TYPE";
        default -> "RECORD_TYPE_" + type;
        };
    }
}
