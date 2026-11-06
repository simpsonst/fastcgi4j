// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2022,2023,2026, Lancaster University
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

/**
 * Allows an application to set raw response fields. All pure FastCGI
 * sessions support application-defined fields.
 * 
 * <p>
 * An application may modify header fields up until it calls
 * {@link Session#out()}, which is when the fields are committed. The
 * meaning of these fields is largely protocol-dependent, except that
 * the field <samp>{@value "%s" #STATUS_FIELD}</samp> may not be set.
 * 
 * <p>
 * An application may modify trailer fields up until it calls
 * {@link java.io.OutputStream#close()} on the result of
 * {@link Session#out()}. As an extension to FastCGI/1.0, this feature
 * is subject to availability of supporting servers.
 * 
 * @author simpsons
 */
public interface FieldSession extends Session {
    /**
     * Get the response field header. This forbids the name
     * <code>{@value #STATUS_FIELD}</code>, which is used by (Fast)CGI,
     * throwing {@link IllegalArgumentException}. If {@link #out()} has
     * been invoked, the methods of the returned object throw
     * {@link IllegalStateException}.
     * 
     * @return the response header
     */
    ResponseFieldSet responseHeader();

    /**
     * Get the request trailer. All fields must be set before closing
     * {@link #out()}.
     * 
     * <p>
     * Some server protocols may impose other requirements. For example,
     * HTTP usually requires trailer field names to be listed in the
     * <samp>{@value "%s" uk.ac.lancs.http.FieldNames#TRAILER}</samp>
     * header field.
     * 
     * <p>
     * This is an experimental extension to FastCGI/1.0. It will only be
     * enabled if the server initiates the session with the
     * {@link uk.ac.lancs.fastcgi.proto.RequestFlags#SUPPLY_TRAILER}
     * flag set.
     * 
     * @return a mutable field set; or {@code null} if a response
     * trailer is not permitted
     * 
     * @implNote By default, this method returns {@code null}, denoting
     * that the response trailer cannot be set.
     */
    ResponseFieldSet responseTrailer();

    /**
     * Specifies the header field name used to set the response status
     * code. The value is <code>{@value}</code>.
     */
    String STATUS_FIELD = "Status";
}
