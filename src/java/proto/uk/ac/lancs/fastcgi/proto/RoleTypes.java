/*
 * Copyright (c) 2022, Lancaster University
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
 * Indicates the role that the application is expected to play in
 * responding to a request.
 * 
 * @see <a href=
 * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S6">Roles</a>
 * from the FastCGI specification
 * 
 * @author simpsons
 */
public final class RoleTypes {
    private RoleTypes() {}

    /**
     * Indicates that the application is expected to respond to the
     * request as a responder. The value is {@value}.
     * 
     * @see <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S6.2">FastCGI
     * Specification &mdash; Responder</a> from the FastCGI
     * specification
     */
    public static final int RESPONDER = 1;

    /**
     * Indicates that the application is expected to respond to the
     * request as an authorizer. The value is {@value}.
     * 
     * @see <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S6.3">FastCGI
     * Specification &mdash; Authorizer</a> from the FastCGI
     * specification
     */
    public static final int AUTHORIZER = 2;

    /**
     * Indicates that the application is expected to respond to the
     * request as a filter. The value is {@value}.
     * 
     * @see <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S6.4">FastCGI
     * Specification &mdash; Filter</a> from the FastCGI specification
     */
    public static final int FILTER = 3;
}
