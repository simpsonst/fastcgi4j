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
 * Holds codes for indicating why an application is terminating a
 * session.
 * 
 * @see <a href=
 * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S5.5">FastCGI
 * Specification &mdash; <code>FCGI_END_REQUEST</code></a>
 * 
 * @author simpsons
 */
public final class ProtocolStatuses {
    private ProtocolStatuses() {}

    /**
     * Indicates that the request is complete. The value is
     * {@value}, corresponding to <code>FCGI_REQUEST_COMPLETE</code>.
     */
    public static final byte REQUEST_COMPLETE = 0;

    /**
     * Indicates that another request is open on this connection, and
     * the application does not multiplex requests. The value is
     * {@value}, corresponding to <code>FCGI_CANT_MPX_CONN</code>.
     */
    public static final byte CANT_MPX_CONN = 1;

    /**
     * Indicates that the application currently can't handle another
     * request. The value is {@value}, corresponding to
     * <code>FCGI_OVERLOADED</code>.
     */
    public static final byte OVERLOADED = 2;

    /**
     * Indicates that the request's role cannot be fulfilled by the
     * application. The value is {@value}, corresponding to
     * <code>FCGI_UNKNOWN_ROLE</code>.
     */
    public static final byte UNKNOWN_ROLE = 3;

    /**
     * Get a string representation of a protocol status.
     * 
     * @param status the status
     * 
     * @return the string representation, including the numeric value if
     * unknown
     */
    public static String toString(int status) {
        return switch (status) {
        case REQUEST_COMPLETE -> "REQUEST_COMPLETE";
        case CANT_MPX_CONN -> "CANT_MPX_CONN";
        case OVERLOADED -> "OVERLOADED";
        case UNKNOWN_ROLE -> "UNKNOWN_ROLE";
        default -> "PROTOCOL_STATUS_" + status;
        };
    }
}
