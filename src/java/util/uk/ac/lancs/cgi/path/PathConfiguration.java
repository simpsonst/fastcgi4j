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
 *  Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.cgi.path;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import uk.ac.lancs.cgi.CGIParameters;

/**
 * Understands how to navigate based on future invocations of a service.
 * Each service has a context of a given application-specific type,
 * which is the type parameter of this class, and could be used (for
 * example) to point each instance at a different database, or root of a
 * directory structure. Each service also has an internal prefix and an
 * external one. A {@link PathConfiguration} can recognize the internal
 * prefix expressed as various CGI parameters, and map them to a
 * {@link PathContext}, which provides both the external prefix and the
 * application-defined context.
 * 
 * <p>
 * For example, if an application is externally accessible through this
 * prefix:
 * 
 * <pre>
 * https://example.org
 * </pre>
 * 
 * <p>
 * &hellip;but that is just a reverse proxy onto this internal prefix:
 * 
 * <pre>
 * http://backend.local:3000/foo
 * </pre>
 * 
 * <p>
 * &hellip;and the application receives a request for:
 * 
 * <pre>
 * http://backend.local:3000/foo/bar/baz
 * </pre>
 * 
 * <p>
 * &hellip;then a {@link PathConfiguration} can be created to map from
 * that to the external URI:
 * 
 * <pre>
 * https://example.org/bar/baz
 * </pre>
 * 
 * <p>
 * &hellip;as well as to any application-specific context that helps the
 * application to fulfil requests under <samp>https://example.org</samp>
 * as opposed to (say) <samp>https://example.com</samp>, which could be
 * forwarded via a distinct reverse proxy. Multiple such mappings can be
 * defined in a single {@link PathConfiguration}.
 * 
 * <p>
 * A context for each instance is defined by a call to
 * {@link Builder#instance(Object, String, String)}, and multiple
 * contexts can be defined from Java properties with
 * {@link Builder#instances(Properties, String, Function)}. Such
 * contexts must be created before the {@link PathConfiguration} is
 * created, so this class is intended for static instances at irregular
 * paths. For something more regular, the application can perform its
 * own context search internally, which makes this class redundant
 * (except perhaps for making a single instance).
 * 
 * @author simpsons
 * 
 * @param <C> the context type
 */
public final class PathConfiguration<C> {
    private static class Instance<C> {
        public final URI server;

        public final List<String> prefix;

        public final C context;

        public Instance(URI server, List<String> prefix, C context) {
            this.server = server;
            this.prefix = prefix;
            this.context = context;
        }
    }

    /**
     * Creates navigation in stages.
     * 
     * @param <C> the context type
     */
    public static final class Builder<C> {
        private Function<? super Map<? super String, ? extends String>,
                         ? extends String> scriptFilename =
                             m -> m.get(CGIParameters.SCRIPT_FILENAME);

        private Function<? super Map<? super String, ? extends String>,
                         ? extends String> pathInfo =
                             m -> m.get(CGIParameters.PATH_INFO);

        private Function<? super Map<? super String, ? extends String>,
                         ? extends String> scriptName =
                             m -> m.get(CGIParameters.SCRIPT_NAME);

        private final Map<URI, Map<List<String>, Instance<C>>> instances =
            new HashMap<>();

        Builder() {}

        /**
         * Specify how to obtain the script filename from CGI
         * parameters.
         * 
         * @param func a function taking CGI parameters and returning
         * the script filename
         * 
         * @return this builder
         */
        public Builder<C>
            scriptFilename(Function<? super Map<? super String,
                                                ? extends String>,
                                    ? extends String> func) {
            this.scriptFilename = Objects.requireNonNull(func, "func");
            return this;
        }

        /**
         * Specify how to obtain the script name from CGI parameters.
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
        public Builder<C>
            scriptName(Function<? super Map<? super String, ? extends String>,
                                ? extends String> func) {
            this.scriptName = Objects.requireNonNull(func, "func");
            return this;
        }

        /**
         * Specify how to obtain the path information from CGI
         * parameters.
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
        public Builder<C>
            pathInfo(Function<? super Map<? super String, ? extends String>,
                              ? extends String> func) {
            this.pathInfo = Objects.requireNonNull(func, "func");
            return this;
        }

        /**
         * Specify an instance of the service.
         * 
         * @param context the instance context
         * 
         * @param externalService the external service URI prefix
         * 
         * @param internalService the internal service URI prefix
         * 
         * @return this object
         */
        public Builder<C> instance(C context, String externalService,
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
                .put(intPfx, new Instance<>(extSrv, extPfx, context));
            return this;
        }

        /**
         * Specify multiple instances of the service from a subset of
         * properties. Every property with a name matching
         * <samp><var>prefix</var><var>instance</var>.external</samp>
         * specifies an external URI prefix for <var>instance</var>. A
         * corresponding property called
         * <samp><var>prefix</var><var>instance</var>.internal</samp>
         * may be set to specify an internal URI prefix for the
         * instance, but it is assumed to be the same as the external
         * URI prefix. The <var>instance</var> string is mapped to the
         * application-specified instance type through
         * <samp>instanceMap</samp>.
         * 
         * <p>
         * For example, with an empty prefix, the following properties
         * will result in invocations to {@code instanceMap} with
         * <samp>apache</samp> and <samp>nginx</samp>, and will result
         * in a {@link PathConfiguration} recognizing invocations under
         * <samp>http://localhost/test</samp> and
         * <samp>http://localhost:8000/test</samp>, yielding
         * {@link Navigator}s within
         * <samp>https://foo.example.com</samp> and
         * <samp>https://bar.example.com</samp> respectively:
         * 
         * <pre class="java-props">
         * apache.internal=http://localhost/test
         * apache.external=https://foo.example.com
         * nginx.internal=http://localhost:8000/test
         * nginx.external=https://bar.example.com
         * </pre>
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
        public Builder<C> instances(Properties props, String prefix,
                                    Function<? super String, C> instanceMap) {
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
        public PathConfiguration<C> create() {
            return new PathConfiguration<>(scriptFilename, pathInfo, scriptName,
                                           Map.copyOf(instances));
        }
    }

    /**
     * Start building navigation.
     * 
     * @param <C> the context type
     * 
     * @return a fresh builder
     * 
     * @constructor
     */
    public static <C> Builder<C> start() {
        return new Builder<>();
    }

    PathConfiguration(Function<? super Map<? super String, ? extends String>,
                               ? extends String> scriptFilename,
                      Function<? super Map<? super String, ? extends String>,
                               ? extends String> pathInfo,
                      Function<? super Map<? super String, ? extends String>,
                               ? extends String> scriptName,
                      Map<URI, Map<List<String>, Instance<C>>> instances) {
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

    private final Map<URI, Map<List<String>, Instance<C>>> instances;

    /**
     * Get a collection of all instances' contexts.
     * 
     * @return a collection of the defined instance contexts
     */
    public Collection<C> getInstances() {
        return instances.values().stream().map(Map::values)
            .flatMap(Collection::stream).map(i -> i.context).toList();
    }

    /**
     * Get the path context for a CGI context. The result can in turn
     * provide a navigator and the application context corresponding to
     * the provided CGI context.
     * 
     * @param params the CGI parameters defining the context
     * 
     * @return the path context corresponding to the CGI context
     */
    public PathContext<C>
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
        Instance<C> instance = null;
        List<String> remainder = null;
        List<String> prior;
        if (sm != null) {
            final int scriptLen = scriptElems.size();
            for (int i = 0; i <= scriptLen && instance == null; i++) {
                prior = scriptElems.subList(0, scriptLen - i);
                remainder = scriptElems.subList(scriptLen - i, scriptLen);
                instance = sm.get(prior);
            }
        }

        final C ctxt;
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

    /**
     * @hidden
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
        params.put(Utils.HOST_PARAM, "backend.local:3000");
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
