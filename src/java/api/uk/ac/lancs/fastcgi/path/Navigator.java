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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Understands the context of the invocation of a service. A navigator
 * has two purposes, given an invocation of a script:
 * 
 * <ol>
 * 
 * <li>
 * <p>
 * It knows which resource within the script is being accessed. This is
 * usually equivalent to the <samp>PATH_INFO</samp> CGI parameter, and
 * can be used to determine what content to provide, and generate
 * relative URIs to other resources within the same script.
 * 
 * <li>
 * <p>
 * It knows the script's external URI, so it can generate absolute URIs,
 * usually required for <samp>Location</samp> header fields.
 * 
 * </ol>
 * 
 * 
 * 
 * @author simpsons
 */
public final class Navigator<I> {
    private final I instance;

    private final URI server;

    private final List<String> resource;

    private final String resourceString;

    private final String leadElem;

    private final List<String> base;

    private final List<String> script;

    /**
     * Create a navigator.
     * 
     * <p>
     * The supplied external server URI has <samp>/</samp> resolved
     * against it, so path information here is discarded.
     * 
     * <p>
     * The resource path elements must begin with an empty element,
     * e.g., <code>Arrays.asList("", "yan", "tan", "")</code> is
     * equivalent to a <samp>PATH_INFO</samp> of <samp>/yan/tan/</samp>.
     * (The trailing empty element <em>after</em> the leading one
     * corresponds to a trailing slash in <samp>PATH_INFO</samp>.)
     * 
     * <p>
     * Copies of the two supplied lists are retained internally.
     * 
     * @param instance the context of the instance being invoked
     * 
     * @param server the external server of the service instance; the
     * path is discarded
     * 
     * @param script the path elements identifying the service instance;
     * may be empty
     * 
     * @param resource the path elements identifying the resource within
     * the service, beginning with an empty element, and optionally
     * ending with an additional empty element to refer to a
     * directory-like path
     */
    Navigator(I instance, URI server, List<? extends String> script,
              List<? extends String> resource) {
        final int rlen = resource.size();
        assert rlen > 0 : "empty resource";
        assert resource.get(0).isEmpty() : "initial resource element non-empty";

        {
            /* Reject a script path that contains empty elements, or
             * elements containing forward slashes. */
            Optional<? extends String> badElement = script.stream()
                .filter(s -> s.isEmpty() || s.indexOf('/') >= 0).findAny();
            assert badElement.isEmpty() :
                "script element " + (badElement.get().isEmpty() ? "empty" :
                    ("with slash: " + badElement.get()));
        }

        {
            /* Reject a resource path that contains elements containing
             * forward slashes. */
            Optional<? extends String> badElement =
                resource.stream().filter(s -> s.indexOf('/') >= 0).findAny();
            assert badElement.isEmpty() :
                "resource element with slash: " + badElement.get();
        }

        if (rlen >= 2) {
            /* Reject a resource path that contains empty elements
             * except at the start or end. */
            Optional<? extends String> badElement =
                resource.subList(1, rlen - 1).stream().filter(String::isEmpty)
                    .findAny();
            assert badElement.isEmpty() : "resource element empty";
        }

        this.instance = instance;

        this.script = List.copyOf(script);
        final int slen = this.script.size();
        this.leadElem = slen > 0 ? this.script.get(slen - 1) : null;

        /* Discard any virtual path, query or fragment identifier of the
         * server. */
        this.server = server.resolve("/");

        this.resource = List.copyOf(resource);
        this.resourceString =
            resource.stream().collect(Collectors.joining("/"));

        /* Create a base element sequence which is the same as the
         * resource, except that the last element is removed, and the
         * first is the last element of the external service prefix. */
        List<String> base = new ArrayList<>(resource.subList(0, rlen - 1));
        if (!base.isEmpty()) base.set(0, this.leadElem);
        this.base = List.copyOf(base);
    }

    /**
     * Refer to an internal resource within the service. An empty string
     * refers to the service root. A leading slash is otherwise assumed.
     * The result can be used to generate a relative URI with a relative
     * or absolute path (useful for generating links to one resource
     * from the content of another), or an absolute URI (essential for
     * redirection).
     * 
     * @param path the path to the resource relative to the service root
     * 
     * @return the requested resource reference
     * 
     * @throws ExternalPathException if an attempt is made to reference
     * an external resource
     */
    public PathReference locate(String path) {
        if (path.isEmpty() && leadElem == null)
            throw new ExternalPathException("");

        List<String> ref = Utils.decomposeInternalPath(path);
        ref.set(0, leadElem);
        return new PathReference(ref, base, script, server);
    }

    /**
     * Get the instance context.
     * 
     * @return the instance context
     */
    public I instance() {
        return instance;
    }

    /**
     * Determine whether the service instance's root path can be
     * referenced. This is not the case if the instance's root is the
     * server root.
     * 
     * @return {@code true} if the root path can be referenced;
     * {@code false} otherwise
     */
    public boolean allowRoot() {
        return leadElem != null;
    }

    /**
     * Identify the resource as a sequence of path elements.
     * 
     * @return an immutable list beginning with an empty element
     */
    public List<String> resourceElements() {
        return resource;
    }

    /**
     * Identify the resource as a slash-separated string. This is either
     * an empty string (meaning the service root) or a sequence of path
     * elements prefixed by forward slashes. The last path element may
     * be empty.
     * 
     * @return the resource identifier
     */
    public String resource() {
        return resourceString;
    }

    /**
     * Identify the invoked script by its external virtual path.
     * 
     * @return the script path
     * 
     * @deprecated The application should be able to do without this. In
     * any case, calling
     * <code>{@linkplain #locate(String)}("").local()</code> should
     * yield the same value.
     */
    @Deprecated
    public String script() {
        return script.stream().map(s -> "/" + s).collect(Collectors.joining());
    }

    /**
     * Match the resource against a regular expression.
     * 
     * @param pattern the regular expression
     * 
     * @return a matcher for the resource against the expression
     */
    public Matcher identify(Pattern pattern) {
        return pattern.matcher(resourceString);
    }

    /**
     * Get the hash code for this object. This is derived from
     * normalized versions of all the provided inputs.
     * 
     * @return the object's hash code
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 13 * hash + Objects.hashCode(this.instance);
        hash = 13 * hash + Objects.hashCode(this.server);
        hash = 13 * hash + Objects.hashCode(this.resource);
        hash = 13 * hash + Objects.hashCode(this.script);
        return hash;
    }

    /**
     * Test whether another object equals this one.
     * 
     * @param obj the object
     * 
     * @return {@code true} if the object is of the same type and is
     * configured with the same normalized inputs; {@code false}
     * otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Navigator other = (Navigator) obj;
        if (!Objects.equals(this.instance, other.instance)) return false;
        if (!Objects.equals(this.server, other.server)) return false;
        if (!Objects.equals(this.resource, other.resource)) return false;
        return Objects.equals(this.script, other.script);
    }

    private static final Logger logger =
        Logger.getLogger(Navigator.class.getPackageName());
}
