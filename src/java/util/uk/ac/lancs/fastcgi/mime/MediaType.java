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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Expresses an immutable MIME media type with parameters.
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
     * Records modifications to the parameters of a MIME media type
     * before applying them.
     */
    public final class Modification {
        Collection<String> removals = new HashSet<>();

        Map<String, ParameterValue> replacements = new HashMap<>();

        Modification() {}

        private ParameterValue modified(String key) {
            if (removals.contains(key)) return null;
            ParameterValue v = replacements.get(key);
            if (v != null) return v;
            return params.get(key);
        }

        /**
         * Apply the modifications.
         * 
         * @return a new media type with the same major and minor type,
         * and with identical parameters except for the modifications
         */
        public MediaType apply() {
            Collection<String> keys = Stream
                .concat(params.keySet().stream(),
                        replacements.keySet().stream())
                .collect(Collectors.toSet());
            Map<String, ParameterValue> newParams = new HashMap<>();
            for (var k : keys) {
                var v = modified(k);
                if (v == null) continue;
                newParams.put(k, v);
            }
            return new MediaType(major, minor, Map.copyOf(newParams));
        }

        /**
         * Remove a parameter.
         * 
         * @param name the parameter name
         * 
         * @return this object
         */
        public Modification remove(String name) {
            removals.add(name);
            replacements.remove(name);
            return this;
        }

        /**
         * Add or set a parameter.
         * 
         * @param name the parameter name
         * 
         * @param value the parameter's new value
         * 
         * @return this object
         */
        public Modification set(String name, ParameterValue value) {
            removals.remove(name);
            replacements.put(name, value);
            return this;
        }

        /**
         * Add or set a parameter.
         * 
         * @param name the parameter name
         * 
         * @param value the plain string value of the parameter
         * 
         * @return this object
         */
        public Modification set(String name, String value) {
            return set(name, ParameterValue.of(value));
        }
    }

    /**
     * Prepare to modify the parameters.
     * 
     * @return a fresh modification
     * 
     * @constructor
     */
    public Modification modify() {
        return new Modification();
    }

    /**
     * Check whether the media type is a specific type.
     * 
     * @param major the expected major type
     * 
     * @param minor the expected minor type
     * 
     * @return {@code true} if the major and minor types match;
     * {@code false} otherwise
     */
    public boolean is(String major, String minor) {
        return this.major.equals(major) && this.minor.equals(minor);
    }

    /**
     * Parse a MIME media type from a string.
     * 
     * @param text the string to parse
     * 
     * @return the MIME type; or {@code null} if the input is
     * {@code null}
     * 
     * @throws IllegalArgumentException if the text is not a MIME media
     * type
     * 
     * @constructor
     */
    public static MediaType fromString(CharSequence text) {
        if (text == null) return null;
        Tokenizer tok = new Tokenizer(text);
        MediaType res = tok.mediaType();
        if (res == null)
            throw new IllegalArgumentException("not MIME type: " + text);
        return res;
    }

    /**
     * @undocumented
     */
    public static void main(String[] args) throws Exception {
        MediaType t = MediaType.fromString(args[0]);
        System.err.printf("major: %s  minor: %s%n", t.major(), t.minor());
        System.err.printf("boundary: [%s]%n", t.parameter("boundary"));
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

    /**
     * Get a string representation of the media type. This should be
     * largely how it appears in a header field.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(major).append('/').append(minor);
        for (var e : params.entrySet()) {
            /* TODO: Check elsewhere that the major and minor types are
             * tokens. */
            /* TODO: Properly encode the value, including the
             * language. */
            result.append("; ").append(e.getKey()).append('=')
                .append(Tokenizer.quoteOptionally(e.getValue().text));
        }
        return result.toString();
    }

    private static MediaType decode(List<? extends CharSequence> seq) {
        if (seq.isEmpty()) return null;
        CharSequence last = seq.get(seq.size() - 1);
        return fromString(last);
    }

    private static List<String> encode(MediaType obj) {
        if (obj == null) return Collections.emptyList();
        return Collections.singletonList(obj.toString());
    }

    /**
     * Converts between internal and external representations of MIME
     * types. The encoder translates {@code null} into an empty
     * sequence, and other values into a singleton. The decoder
     * translates an empty sequence into {@code null}, and yields only
     * the last element of non-empty sequences.Coalescing ignores the
     * former value.
     */
    public static final Format<MediaType> FORMAT =
        Format.of("media-type", MediaType.class, MediaType::decode,
                  MediaType::encode, (a, b) -> b);
}
