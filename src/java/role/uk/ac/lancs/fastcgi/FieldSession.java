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

import java.util.List;
import java.util.Map;

/**
 * Allows an application to set raw response fields. All pure FastCGI
 * sessions support application-defined fields.
 * 
 * <p>
 * An application may set, modify and clear fields up until it calls
 * {@link Session#out()}, which is when the fields are committed. The
 * meaning of these fields is largely protocol-dependent, except that
 * the field <samp>{@value "%s" #STATUS_FIELD}</samp> may not be set.
 * 
 * @author simpsons
 */
public interface FieldSession extends Session {
    /**
     * Set a response header field, replacing any existing values.
     * Leading and trailing spaces are trimmed from the name.
     * 
     * @param name the header field name
     * 
     * @param value the new value
     * 
     * @throws IllegalArgumentException if the field name is
     * <code>{@value #STATUS_FIELD}</code>
     * 
     * @throws IllegalStateException if the response output has been
     * started (with {@link #out()})
     */
    void setField(String name, String value);

    /**
     * Specifies the header field name used to set the response status
     * code. The value is <code>{@value}</code>.
     */
    String STATUS_FIELD = "Status";

    /**
     * Add a response header field, retaining earlier values as distinct
     * fields. Leading and trailing spaces are trimmed from the name.
     * 
     * @param name the header field name
     * 
     * @param value the additional value
     * 
     * @throws IllegalArgumentException if the field name is
     * <code>{@value #STATUS_FIELD}</code>
     * 
     * @throws IllegalStateException if the response output has been
     * started (with {@link #out()})
     */
    void addField(String name, String value);

    /**
     * Remove all instances of a response header field.
     * 
     * @param name the header field name
     * 
     * @throws IllegalArgumentException if the field name is
     * <code>{@value #STATUS_FIELD}</code>
     * 
     * @throws IllegalStateException if the response output has been
     * started (with {@link #out()})
     */
    void clearField(String name);

    /**
     * Get the request trailer. All fields must be set before closing
     * {@link #out()}.
     * 
     * <p>
     * Some server protocols may impose other requirements. For example,
     * HTTP usually requires trailer field names to be listed in the
     * <samp>{@value "%s"
     * uk.ac.lancs.http.field.FieldNames#TRAILER}</samp> header field.
     * 
     * <p>
     * This is an experimental extension to FastCGI/1.0. It will only be
     * enabled if the server initiates the session with the
     * {@link uk.ac.lancs.fastcgi.proto.RequestFlags#SUPPLY_TRAILER}
     * flag set.
     * 
     * @return a mutable set of field names mapping to sequences of
     * values; or {@code null} if a response trailer is not permitted
     * 
     * @implNote By default, this method returns {@code null}, denoting
     * that the response trailer cannot be set.
     */
    default Map<String, List<String>> responseTrailer() {
        return null;
    }
}
