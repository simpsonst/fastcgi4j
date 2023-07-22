// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2023, Lancaster University
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

package uk.ac.lancs.fastcgi.mime;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Expresses a MIME media type with parameters.
 *
 * @author simpsons
 */
public final class MediaType {
    private final String major;

    private final String minor;

    private final Map<String, ParameterValue> params;

    MediaType(String major, String minor, Map<String, ParameterValue> params) {
        this.major = major;
        this.minor = minor;
        this.params = params;
    }

    /**
     * Parse a MIME media type from a string.
     * 
     * @param text
     * 
     * @return the MIME type
     * 
     * @throws IllegalArgumentException if the text is not a MIME media
     * type
     * 
     * @constructor
     */
    public static MediaType fromString(String text) {
        throw new UnsupportedOperationException("unimplemented"); // TODO
    }

    /**
     * Create a MIME media type from separate major and minor
     * components.
     * 
     * @param major the major (or top-level) type
     * 
     * @param minor the minor type (or subtype)
     * 
     * @return the requested media type
     * 
     * @throws NullPointerException if an argument is {@code null}
     */
    public static MediaType of(String major, String minor) {
        return new MediaType(Objects.requireNonNull(major, "major"),
                             Objects.requireNonNull(minor, "minor"),
                             Collections.emptyMap());
    }

    /**
     * Add parameters to a MIME media type. A new media type is created.
     * The supplied parameters override existing parameters.
     * 
     * @param params additional parameters
     * 
     * @return the requested media type
     * 
     * @constructor
     */
    public MediaType
        qualify(Map<? extends String, ? extends ParameterValue> params) {
        var repl = new HashMap<>(this.params);
        repl.putAll(params);
        return new MediaType(major, minor, Map.copyOf(repl));
    }

    /**
     * Check whether the media type is a multipart type.
     * 
     * @return {@code true} if the major type is <samp>multipart</samp>;
     * {@code false} otherwise
     */
    public boolean isMultipart() {
        return major.equals("multipart");
    }

    /**
     * Check whether the media type is a text type.
     * 
     * @return {@code true} if the major type is <samp>text</samp>;
     * {@code false} otherwise
     */
    public boolean isText() {
        return major.equals("text");
    }

    /**
     * Remove specific parameters from the media type.
     * 
     * @param excl a function identifying names of parameters to be
     * removed
     * 
     * @return the requested media type
     * 
     * @constructor
     */
    public MediaType dequalify(Predicate<? super String> excl) {
        var alt = params.entrySet().stream().filter(p -> !excl.test(p.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new MediaType(major, minor, alt);
    }

    /**
     * Remove specific parameters from the media type.
     * 
     * @param excl a set of names of parameters to be removed
     * 
     * @return the requested media type
     * 
     * @constructor
     */
    public MediaType dequalify(Collection<? extends String> excl) {
        return dequalify(excl::contains);
    }

    /**
     * Remove specific parameters from the media type.
     * 
     * @param excl names of parameters to be removed
     * 
     * @return the requested media type
     * 
     * @constructor
     */
    public MediaType dequalify(String... excl) {
        Collection<String> names = new HashSet<>(Arrays.asList(excl));
        return dequalify(names);
    }

    /**
     * Strip the media type of parameters.
     * 
     * @return the media type without parameters
     * 
     * @constructor
     */
    public MediaType dequalify() {
        return new MediaType(major, minor, Collections.emptyMap());
    }

    /**
     * Get the major or top-level type.
     * 
     * @return the top-level type
     */
    public String major() {
        return major;
    }

    /**
     * Get the minor type or subtype.
     * 
     * @return the subtype
     */
    public String minor() {
        return minor;
    }

    /**
     * Get a named parameter.
     * 
     * @param name the parameter name
     * 
     * @return the value of the parameter; or {@code null} if not set
     */
    public String parameter(String name) {
        ParameterValue pv = params.get(name);
        if (pv == null) return null;
        return pv.text;
    }

    /**
     * Get the locale of a named parameter.
     * 
     * @param name the parameter name
     * 
     * @return the locale of the parameter; or {@code null} if either
     * the parameter or its locale is not set
     */
    public Locale locale(String name) {
        ParameterValue pv = params.get(name);
        if (pv == null) return null;
        return pv.locale;
    }
}
