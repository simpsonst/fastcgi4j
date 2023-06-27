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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Understands the context of the invocation of a service.
 * 
 * @author simpsons
 */
public final class Navigator {
    private final String instance;

    private final URI server;

    private final List<String> resource;

    private final String resourceString;

    private final String leadElem;

    private final List<String> base;

    private final List<String> script;

    /**
     * Create a navigator.
     * 
     * @param instance the name of the instance being invoked
     * 
     * @param service the external URI prefix of the service instance
     * 
     * @param resource the path elements identifying the resource within
     * the service
     */
    Navigator(String instance, URI service, List<String> resource) {
        this.instance = instance;
        this.script =
            Utils.decomposePathPrefix(service.resolve("./").getPath());
        this.server = service.resolve("/");
        final int slen = this.script.size();
        this.leadElem = slen > 0 ? this.script.get(slen - 1) : null;

        final int rlen = resource.size();
        this.resource = resource.subList(1, rlen);
        this.resourceString =
            this.resource.stream().collect(Collectors.joining("/"));
        List<String> base = new ArrayList<>(resource.subList(0, rlen - 1));
        base.set(0, this.leadElem);
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
     * Get the name of this instance.
     * 
     * @return the instance name
     */
    public String instance() {
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

    private static final Logger logger =
        Logger.getLogger(Navigator.class.getPackageName());
}
