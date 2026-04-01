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

package uk.ac.lancs.fastcgi.path;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Refers to a resource within the script.
 * 
 * @author simpsons
 */
public final class PathReference {
    private final List<String> ref;

    private final List<String> base;

    private final List<String> script;

    private final URI server;

    /**
     * Create a path reference.
     * 
     * @param ref the path elements navigating from the script to the
     * referred internal resource, with the first element being the last
     * element of the service instance URI
     * 
     * @param base the path elements navigating from the script to the
     * referring internal resource, with the first element being the
     * last element of the service instance URI, or {@code null} if
     * there is no such element
     * 
     * @param script the path elements leading up to the service
     * instance, possibly empty
     * 
     * @param server the server URI of the service instance, against
     * which only absolute paths will be resolved
     */
    PathReference(List<String> ref, List<String> base, List<String> script,
                  URI server) {
        this.ref = ref;
        this.base = base;
        this.script = script;
        this.server = server;
    }

    /**
     * Holds the fragment identifier to be added when generating the
     * URI, or {@code null} if no fragment is to be added.
     */
    private String fragment = null;

    /**
     * Holds the query parameters to be added when generating the URI,
     * or {@code null} if no query is to be added.
     */
    private Collection<Map.Entry<? extends String, ? extends String>> query =
        null;

    /**
     * Set the fragment identifier.
     * 
     * @param value the new fragment identifier; or {@code null} to
     * exclude the fragment identifier
     * 
     * @return this object
     */
    public PathReference fragment(String fragment) {
        this.fragment = fragment;
        return this;
    }

    /**
     * Set the query parameters. Previously set parameters are
     * discarded. Parameters are added in iteration order. Iteration
     * only occurs when building the URI with {@link #relative()},
     * {@link #local()} or {@link #absolute()}.
     * 
     * @param query the new query parameters, which are copied
     * 
     * @return this object
     */
    public PathReference
        query(Collection<? extends Map.Entry<? extends String,
                                             ? extends String>> query) {
        this.query =
            new ArrayList<>(query.stream().map(Map.Entry::copyOf).toList());
        return this;
    }

    /**
     * Append a query parameter.
     * 
     * @param name the parameter name
     * 
     * @param value the parameter value, which is converted with
     * {@link Object#toString()} first
     * 
     * @return this object
     */
    public PathReference parameter(String name, Object value) {
        if (query == null) query = new ArrayList<>();
        query.add(Map.entry(name, value.toString()));
        return this;
    }

    /**
     * Clear the query parameters. In this state, no <samp>?</samp> will
     * be appended.
     * 
     * @return this object
     */
    public PathReference noQuery() {
        query = null;
        return this;
    }

    /**
     * Append the encoded query string and fragment identifier if
     * present.
     * 
     * @param result the destination for added components
     * 
     * @return the input builder
     */
    private StringBuilder appendExtras(StringBuilder result) {
        if (query != null) {
            char sep = '?';
            for (var kv : query) {
                var k = kv.getKey();
                var v = kv.getValue();
                result.append(sep);
                sep = '&';
                Utils.escapeParam(result, k);
                result.append('=');
                Utils.escapeParam(result, v);
            }
        }

        if (fragment != null) {
            result.append('#');
            Utils.escapeFragment(result, fragment);
        }

        return result;
    }

    /**
     * Generate the relative URI for the identified resource.
     * 
     * @return the relative URI to the resource
     */
    public URI relative() {
        /* Eliminate identical initial elements of our two lists. */
        List<String> base = this.base;
        List<String> ref = this.ref;
        while (!base.isEmpty() && ref.size() > 1 &&
            base.get(0).equals(ref.get(0))) {
            base = base.subList(1, base.size());
            ref = ref.subList(1, ref.size());
        }

        /* For each element of the base still remaining, prefix "../",
         * then append the remaining elements separated by "/". */
        StringBuilder result = new StringBuilder("../".repeat(base.size()));
        String sep = "";
        for (String elem : ref) {
            result.append(sep);
            sep = "/";
            Utils.escapePathElement(result, elem);
        }

        if (result.isEmpty()) result.append("./");

        appendExtras(result);
        return URI.create(result.toString());
    }

    /**
     * Generate the absolute URI for the identified resource.
     * 
     * @return the absolute URI to the resource
     */
    public URI absolute() {
        return server.resolve(local());
    }

    /**
     * Generate the local URI for the identified resource.
     * 
     * @return the local URI to the resource
     */
    public URI local() {
        StringBuilder result = new StringBuilder();
        for (var e : script)
            Utils.escapePathElement(result.append('/'), e);
        for (var e : ref.subList(1, ref.size()))
            Utils.escapePathElement(result.append('/'), e);
        appendExtras(result);
        return URI.create(result.toString());
    }

    /**
     * Get a string representation of this reference. This
     * implementation calls {@link #relative()}, and then applies
     * {@link URI#toASCIIString()} to the result.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        return relative().toASCIIString();
    }

    private static final Logger logger =
        Logger.getLogger(PathReference.class.getPackageName());
}
