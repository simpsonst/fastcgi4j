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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Understands how to navigate based on future invocations of a service.
 * 
 * @author simpsons
 * 
 * @param <I> the instance type
 */
public final class PathConfiguration<I> {
    private static class Instance<I> {
        public final URI server;

        public final List<String> prefix;

        public final I context;

        public Instance(URI server, List<String> prefix, I context) {
            this.server = server;
            this.prefix = prefix;
            this.context = context;
        }
    }

    /**
     * Creates navigation in stages.
     * 
     * @param <I> the instance type
     */
    public static final class Builder<I> {
        private Function<? super Map<? super String, ? extends String>,
                         ? extends String> scriptFilename =
                             m -> m.get("SCRIPT_FILENAME");

        private Function<? super Map<? super String, ? extends String>,
                         ? extends String> pathInfo = m -> m.get("PATH_INFO");

        private Function<? super Map<? super String, ? extends String>,
                         ? extends String> scriptName =
                             m -> m.get("SCRIPT_NAME");

        private final Map<URI, Map<List<String>, Instance<I>>> instances =
            new HashMap<>();

        Builder() {}

        /**
         * Specify how to obtain the script filename.
         * 
         * @param func a function taking CGI parameters and returning
         * the script filename
         * 
         * @return this builder
         */
        public Builder<I>
            scriptFilename(Function<? super Map<? super String,
                                                ? extends String>,
                                    ? extends String> func) {
            this.scriptFilename = Objects.requireNonNull(func, "func");
            return this;
        }

        /**
         * Specify how to obtain the script name.
         * 
         * @param func a function taking CGI parameters and returning
         * the script name
         * 
         * @return this builder
         * 
         * @see <a href=
         * "https://datatracker.ietf.org/doc/html/rfc3875#section-4.1.13">RFC3875
         * Section 4.1.13</a>
         */
        public Builder<I>
            scriptName(Function<? super Map<? super String, ? extends String>,
                                ? extends String> func) {
            this.scriptName = Objects.requireNonNull(func, "func");
            return this;
        }

        /**
         * Specify how to obtain the path information.
         * 
         * @param func a function taking CGI parameters and returning
         * the path information, or {@code null} if not available
         * 
         * @return this builder
         * 
         * @see <a href=
         * "https://datatracker.ietf.org/doc/html/rfc3875section-4.1.5">RFC3875
         * Section 4.1.5</a>
         */
        public Builder<I>
            pathInfo(Function<? super Map<? super String, ? extends String>,
                              ? extends String> func) {
            this.pathInfo = Objects.requireNonNull(func, "func");
            return this;
        }

        /**
         * Specify an instance of the service.
         * 
         * @param instance the instance context
         * 
         * @param externalService the external service URI prefix
         * 
         * @param internalService the internal service URI prefix
         * 
         * @return this object
         */
        public Builder<I> instance(I instance, String externalService,
                                   String internalService) {
            /* Parse the internal service URI prefix as a URI, separate
             * the path elements from the server, and index on server
             * and path elements to yield the instance details. */
            URI intSrv = URI.create(internalService);
            List<String> intPfx = Utils.decomposePathPrefix(intSrv.getPath());
            intSrv = intSrv.resolve("/");

            URI extSrv = URI.create(externalService);
            List<String> extPfx = Utils.decomposePathPrefix(extSrv.getPath());
            extSrv = extSrv.resolve("/");

            instances.computeIfAbsent(intSrv, k -> new HashMap<>())
                .put(intPfx, new Instance<>(extSrv, extPfx, instance));
            return this;
        }

        /**
         * Specify multiple instances of the service from a subset of
         * properties. Every property with a name matching
         * <samp><var>prefix</var><var>instance</var>.external</samp>
         * specifies an external URI prefix for <var>instance</var>. A
         * corresponding property called
         * <samp><var>prefix</var><var>instance</var>.external</samp>
         * may be set to specify an internal URI prefix for the
         * instance, but it is assumed to be the same as the external
         * URI prefix. The <var>instance</var> string is mapped to the
         * user-specified instance type.
         * 
         * @param props container of the properties
         * 
         * @param prefix prefix of property names to match
         * 
         * @param instanceMap a mapping from extracted instance name to
         * context
         * 
         * @return this object
         */
        public Builder<I> instances(Properties props, String prefix,
                                    Function<? super String, I> instanceMap) {
            Pattern pat = Pattern
                .compile("^" + Pattern.quote(prefix) + "(.*)" + "\\.external$");
            for (var exk : props.stringPropertyNames()) {
                Matcher m = pat.matcher(exk);
                if (!m.matches()) continue;
                String name = m.group(1);
                var ink = prefix + name + ".internal";
                String exv = props.getProperty(exk);
                String inv = props.getProperty(ink, exv);
                instance(instanceMap.apply(name), exv, inv);
            }
            return this;
        }

        /**
         * Create the navigation using the current configuration.
         * 
         * @return the new navigation
         * 
         * @constructor
         */
        public PathConfiguration<I> create() {
            return new PathConfiguration<>(scriptFilename, pathInfo, scriptName,
                                           Map.copyOf(instances));
        }
    }

    /**
     * Convert a string to a URI prefix. The input is first treated as a
     * URI, then its path is extracted, split on forward slashes, then
     * empty elements are removed, and then each remaining element is
     * prefixed with a forward slash, and they are concatenated, and
     * resolved against the original URI. For example,
     * <samp>http://example.com/foo/bar/baz/</samp> becomes
     * <samp>http://example.com/foo/bar/baz</samp>, but that itself
     * would be returned unchanged.
     * 
     * @param input the raw URI prefix input
     * 
     * @return the input as a URI prefix
     */
    private static URI getPrefix(String input) {
        URI raw = URI.create(input).normalize();
        return raw.resolve(Utils.decomposePathPrefix(raw.getPath()).stream()
            .map(s -> "/" + s).collect(Collectors.joining()));
    }

    /**
     * Start building navigation.
     * 
     * @param <I> the instance type
     * 
     * @return a fresh builder
     * 
     * @constructor
     */
    public static <I> Builder<I> start() {
        return new Builder<>();
    }

    PathConfiguration(Function<? super Map<? super String, ? extends String>,
                               ? extends String> scriptFilename,
                      Function<? super Map<? super String, ? extends String>,
                               ? extends String> pathInfo,
                      Function<? super Map<? super String, ? extends String>,
                               ? extends String> scriptName,
                      Map<URI, Map<List<String>, Instance<I>>> instances) {
        this.scriptFilename = scriptFilename;
        this.pathInfo = pathInfo;
        this.scriptName = scriptName;
        this.instances = instances;
    }

    private final Function<? super Map<? super String, ? extends String>,
                           ? extends String> scriptFilename;

    private final Function<? super Map<? super String, ? extends String>,
                           ? extends String> pathInfo;

    private final Function<? super Map<? super String, ? extends String>,
                           ? extends String> scriptName;

    private final Map<URI, Map<List<String>, Instance<I>>> instances;

    /**
     * Get a navigator for a CGI context.
     * 
     * @param params the CGI parameters defining the context
     * 
     * @return the requested navigator
     */
    public PathContext<I>
        recognize(Map<? super String, ? extends String> params) {
        /* Determine from the local environment the correct internal
         * script name and path info. */
        final URI scriptFilename =
            URI.create(this.scriptFilename.apply(params));
        String pathInfo = this.pathInfo.apply(params);
        String scriptName = this.scriptName.apply(params);
        do {
            if (pathInfo != null) {
                /* scriptName and pathInfo are just fine and dandy. */
                break;
            }

            if ("proxy".equalsIgnoreCase(scriptFilename.getScheme())) {
                final URI ssp =
                    URI.create(scriptFilename.getRawSchemeSpecificPart());
                final String virtualPath = ssp.getPath();
                if (scriptName.endsWith(virtualPath)) {
                    scriptName = scriptName
                        .substring(0,
                                   scriptName.length() - virtualPath.length());
                    pathInfo = virtualPath;
                    break;
                }
            }
            pathInfo = "";
        } while (false);

        /* Get the local server identity. See if we have any entries for
         * it. */
        final URI internalServer = Utils.getInternalServer(params).resolve("/");
        var sm = instances.get(internalServer);

        /* Combine the script name with the local server details,
         * gradually shortening the path until we get a match. */
        final List<String> scriptElems = Utils.decomposePathPrefix(scriptName);
        Instance<I> instance = null;
        List<String> remainder = null;
        List<String> prior = null;
        if (sm != null) {
            final int scriptLen = scriptElems.size();
            for (int i = 0; i <= scriptLen && instance == null; i++) {
                prior = scriptElems.subList(0, scriptLen - i);
                remainder = scriptElems.subList(scriptLen - i, scriptLen);
                instance = sm.get(prior);
            }
        }

        final I ctxt;
        final URI server;
        final List<String> script;
        if (instance == null) {
            ctxt = null;
            server = internalServer;
            script = scriptElems;
        } else {
            assert remainder != null;
            ctxt = instance.context;
            server = instance.server;
            script = Stream.concat(instance.prefix.stream(), remainder.stream())
                .toList();
        }

        List<String> resource = Utils.decomposeInternalPath(pathInfo);
        return new PathContext<>(ctxt, server, script, resource);
    }

    private static final Logger logger =
        Logger.getLogger(PathConfiguration.class.getPackageName());

    /**
     * @undocumented
     */
    public static void main(String[] args) throws Exception {
        /* Initialization stage */
        Properties props = new Properties();
        props.setProperty("main.external", "https://example.com/zarquon");
        props.setProperty("main.internal", "http://backend.local:3000/foo/bar");
        PathConfiguration<String> pathConfig = PathConfiguration.<String>start()
            .instances(props, "", s -> s).create();

        /* Invocation stage */
        Map<String, String> params = new HashMap<>();
        params.put("REQUEST_SCHEME", "http");
        params.put("SCRIPT_NAME", "/foo/bar/baz");
        params.put("SCRIPT_FILENAME", "/var/www/html/foo/bar/baz");
        params.put("PATH_INFO", "/a/b/c");
        params.put("SERVER_PROTOCOL", "HTTP/1.1");
        params.put("SERVER_NAME", "backend.local");
        params.put("SERVER_PORT", "3000");
        params.put("HTTP_HOST", "backend.local:3000");
        params.put("GATEWAY_INTERFACE", "CGI/1.1");
        PathContext<String> pathCtxt = pathConfig.recognize(params);
        Navigator navigator = pathCtxt.navigator();
        System.out.printf("Instance: %s%n", pathCtxt.instance());
        System.out.printf("Resource: [%s] or %s%n", navigator.resource(),
                          navigator.resourceElements());

        /* Referencing */
        String[] refs = { "", "/", "/my/old/man", "my/old/man", "/a/b/man",
            "a/b/man", "/a/b/", "a/b/", "/a/b", "a/b", "/a/", "a/" };
        for (String ref : refs) {
            try {
                PathReference pr = navigator.locate(ref);
                System.out.printf("[%s] -> %s %s %s%n", ref, pr.relative(),
                                  pr.local(), pr.absolute());
            } catch (ExternalPathException ex) {
                System.out.printf("[%s] is external%n", ref);
            }
        }
    }
}
