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

package uk.ac.lancs.fastcgi.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.lancs.fastcgi.engine.Attribute;
import uk.ac.lancs.fastcgi.engine.Engine;
import uk.ac.lancs.fastcgi.Authorizer;
import uk.ac.lancs.fastcgi.Filter;
import uk.ac.lancs.fastcgi.Responder;
import uk.ac.lancs.scc.jardeps.Application;
import uk.ac.lancs.fastcgi.transport.Transport;

/**
 * Acts as an entry point for FastCGI applications. Such an application
 * should extend this class, override
 * {@link #init(FastCGIConfiguration, String[])} if it needs to perform
 * any initialization, and await sessions. To indicate the roles it
 * supports, it must either implement them directly, or specifying them
 * during the {@link #init(FastCGIConfiguration, String[])} call.
 * 
 * @author simpsons
 */
@Application
public class FastCGIApplication {
    /**
     * Prepare to handle FastCGI sessions.
     * 
     * @param config allows the application to declare its capabilities
     * 
     * @param args command-line arguments to be interpreted by the
     * application
     * 
     * @default This method does nothing by default.
     */
    public void init(FastCGIConfiguration config, String[] args) {}

    /**
     * Prepare to terminate.
     * 
     * @default This method does nothing by default.
     */
    public void term() {}

    private static final String NCONN_PROP = "uk.ac.lancs.fastcgi.nconn";

    private static final String NSESS_PROP = "uk.ac.lancs.fastcgi.nsess";

    private static final String NSPC_PROP = "uk.ac.lancs.fastcgi.nspc";

    private static final String BUFFER_PROP = "uk.ac.lancs.fastcgi.buffer";

    /**
     * Start and run a FastCGI application, using command-line arguments
     * as configuration.
     * 
     * @param args command-line arguments. These take the form
     * <kbd><var>options</var> <var>class-name</var>
     * <var>app-options</var></kbd>. The class name is loaded and
     * instantiated as a {@link FastCGIApplication} using a
     * zero-argument constructor. Its
     * {@link #init(FastCGIConfiguration, String[])} is the called,
     * passing <var>app-options</var>. Other options affect execution as
     * follows:
     * 
     * <dl>
     * 
     * <dt><kbd>-f <var>file</var></kbd>
     * 
     * <dd>Load a Java properties file, and place it atop a stack used
     * to define engine attributes. The previous stack element is used
     * for defaults. Subsequent arguments modify properties at the top
     * of the stack. The default stack consists of an empty element atop
     * the system properties.
     * 
     * <dt><kbd>+f</kbd>
     * 
     * <dd>Push an empty set of properties onto the stack.
     * 
     * <dt><kbd>-c <var>num</var></kbd>
     * <dt><kbd>+c</kbd> (to reset)
     * 
     * <dd>Limit the number of concurrent transport connections. The
     * equivalent property is <samp>uk.ac.lancs.fastcgi.nconn</samp>.
     * 
     * <dt><kbd>-s <var>num</var></kbd>
     * <dt><kbd>+s</kbd> (to reset)
     * 
     * <dd>Limit the number of concurrent sessions. The equivalent
     * property is <samp>uk.ac.lancs.fastcgi.nsess</samp>.
     * 
     * <dt><kbd>-p <var>num</var></kbd>
     * <dt><kbd>+p</kbd> (to reset)
     * 
     * <dd>Limit the number of concurrent sessions per transport
     * connection. The equivalent property is
     * <samp>uk.ac.lancs.fastcgi.nspc</samp>.
     * 
     * <dt><kbd>-b <var>cap</var></kbd>
     * <dt><kbd>+b</kbd> (to reset)
     * 
     * <dd>Set the default standard output buffer capacity. This must be
     * an integer followed by an optional scale, one of
     * <samp>kKmMgG</samp>, e.g., <samp>100k</samp>. The equivalent
     * property is <samp>uk.ac.lancs.fastcgi.buffer</samp>.
     * 
     * <dt><kbd>-D<var>name</var>=<var>value</var></kbd>
     * 
     * <dd>Override the configuration property <var>name</var> to have
     * the value <var>value</var> at the top of the stack.
     * 
     * <dt><kbd>-U<var>name</var></kbd>
     * 
     * <dd>Remove the configuration property <var>name</var> from the
     * top of the stack.
     * 
     * <dt><kbd>--seek</kbd>
     * 
     * <dd>Do not look for <var>class-name</var>. Instead, seek the
     * first service of type {@link FastCGIApplication} as per
     * {@link ServiceLoader}. Abort if more than one service is found,
     * or no service is found.
     * 
     * </dl>
     * 
     * @throws Exception if an error occurs, duh
     */
    public static void main(String[] args) throws Exception {
        class MyConfig implements FastCGIConfiguration {
            Properties props = new Properties(System.getProperties());

            boolean seek = false;

            Responder responder;

            Authorizer authorizer;

            Filter filter;

            @Override
            public void setResponder(Responder app) {
                this.responder = app;
            }

            @Override
            public void setAuthorizer(Authorizer app) {
                this.authorizer = app;
            }

            @Override
            public void setFilter(Filter app) {
                this.filter = app;
            }

            Engine.Builder applyHandlers(Engine.Builder builder) {
                if (responder != null)
                    builder = builder.with(Attribute.RESPONDER, responder);
                if (filter != null)
                    builder = builder.with(Attribute.FILTER, filter);
                if (authorizer != null)
                    builder = builder.with(Attribute.AUTHORIZER, authorizer);
                return builder;
            }

            void run()
                throws InstantiationException,
                    IllegalAccessException,
                    InvocationTargetException,
                    ClassNotFoundException,
                    NoSuchMethodException,
                    IOException {
                /* Process arguments using these defaults. */
                String className = null;
                List<String> appArgs = new ArrayList<>();
                for (int i = 0; i < args.length; i++) {
                    final String arg = args[i];

                    if ("--seek".equals(arg)) {
                        seek = true;
                        continue;
                    }

                    if ("+f".equals(arg)) {
                        props = new Properties(props);
                        continue;
                    }

                    if ("-f".equals(arg)) {
                        File f = new File(args[++i]);
                        props = new Properties(props);
                        try (InputStream in = new FileInputStream(f)) {
                            props.load(in);
                        }
                        continue;
                    }

                    {
                        Matcher m = DEF_PATTERN.matcher(arg);
                        if (m.matches()) {
                            props.setProperty(m.group(1), m.group(2));
                            continue;
                        }
                    }

                    {
                        Matcher m = UNDEF_PATTERN.matcher(arg);
                        if (m.matches()) {
                            props.remove(m.group(1));
                            continue;
                        }
                    }

                    if ("-c".equals(arg)) {
                        props.setProperty(NCONN_PROP, args[++i]);
                        continue;
                    }

                    if ("+c".equals(arg)) {
                        props.remove(NCONN_PROP);
                        continue;
                    }

                    if ("-s".equals(arg)) {
                        props.setProperty(NSESS_PROP, args[++i]);
                        continue;
                    }

                    if ("+s".equals(arg)) {
                        props.remove(NSESS_PROP);
                        continue;
                    }

                    if ("-p".equals(arg)) {
                        props.setProperty(NSPC_PROP, args[++i]);
                        continue;
                    }

                    if ("+p".equals(arg)) {
                        props.remove(NSPC_PROP);
                        continue;
                    }

                    if ("-b".equals(arg)) {
                        props.setProperty(BUFFER_PROP, args[++i]);
                        continue;
                    }

                    if ("+b".equals(arg)) {
                        props.remove(BUFFER_PROP);
                        continue;
                    }

                    if (arg.startsWith("-") || arg.startsWith("+")) {
                        System.err.printf("unknown switch: %s%n", arg);
                        System.exit(1);
                    }

                    if (seek) {
                        appArgs = Arrays.asList(args).subList(i, args.length);
                        break;
                    }

                    if (className == null) {
                        className = arg;
                        appArgs =
                            Arrays.asList(args).subList(i + 1, args.length);
                        break;
                    }
                }

                /* Instantiate and initialize the main class. */
                FastCGIApplication app = null;
                if (seek) {
                    for (FastCGIApplication cand : ServiceLoader
                        .load(FastCGIApplication.class)) {
                        if (app != null)
                            throw new IllegalArgumentException("multiple"
                                + " applications found");
                        app = cand;
                    }
                    if (app == null) throw new IllegalArgumentException("no"
                        + " application found");
                } else {
                    Class<?> clazz = Class.forName(className);
                    app = clazz.asSubclass(FastCGIApplication.class)
                        .getConstructor().newInstance();
                }
                assert app != null;
                app.init(this, appArgs.toArray(n -> new String[n]));

                /* Allow the application to implement roles directly. */
                if (responder == null && app instanceof Responder)
                    responder = (Responder) app;
                if (filter == null && app instanceof Filter)
                    filter = (Filter) app;
                if (authorizer == null && app instanceof Authorizer)
                    authorizer = (Authorizer) app;

                /* Prepare to receive connections, and build the
                 * engine. */
                Transport conns = Transport.get();
                var builder = Engine.start();

                /* Indicate which roles the application supports. */
                if (responder != null)
                    builder = builder.with(Attribute.RESPONDER, responder);
                if (filter != null)
                    builder = builder.with(Attribute.FILTER, filter);
                if (authorizer != null)
                    builder = builder.with(Attribute.AUTHORIZER, authorizer);

                /* Apply configuration from command-line arguments. */
                builder = builder.using(props)
                    .withProperty(Attribute.MAX_CONN, NCONN_PROP)
                    .withProperty(Attribute.MAX_SESS, NSESS_PROP)
                    .withProperty(Attribute.MAX_SESS_PER_CONN, NSPC_PROP)
                    .tryingProperty(Attribute.BUFFER_SIZE, BUFFER_PROP);

                /* Build the engine and start it. */
                Engine engine = builder.build().apply(conns);
                try {
                    while (engine.process())
                        ;
                } finally {
                    app.term();
                }
            }
        }

        MyConfig mc = new MyConfig();
        mc.run();
    }

    private static final Pattern DEF_PATTERN =
        Pattern.compile("^-D([^=]+)=(.*)$");

    private static final Pattern UNDEF_PATTERN = Pattern.compile("^-U([^=]+)$");
}
