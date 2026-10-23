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

package uk.ac.lancs.fastcgi.pattern;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.ac.lancs.fastcgi.SessionException;

/**
 * Dispatches paths to a sequence of candidates, matching on regular
 * expressions and request methods. A dispatcher centres around a
 * user-defined interface type (the <dfn>receiver type</dfn>) that
 * performs different actions in each method, and where method selection
 * is based on some accessed resource's path and request method, as
 * derived from a CGI context. For example, one might define an
 * interface such as this:
 * 
 * <pre class="java">
 * interface Actions {
 *   void action1(Matcher m);
 * 
 *   void action2(Matcher m);
 * }
 * </pre>
 * 
 * <p>
 * <code class="java">action1</code> is to be invoked for paths
 * beginning with <samp>/action</samp>, while
 * <code class="java">action2</code> is is for paths beginning with
 * <samp>/stuff</samp>.
 * 
 * <p>
 * Given this information, a dispatcher can be built statically. During
 * the handling of a specific request, an instance of
 * <code class="java">Actions</code> can be provided:
 * 
 * <pre class="java">
 * static final PatternDispatcher&lt;Actions&gt; dispatcher;
 * 
 * // per request
 * ResponderSession session = ...;
 * Navigator nav = ...;
 * dispatcher.dispatch(session.parameters.get(CGIParameters.REQUEST_METHOD),
 *                     nav.resource(),
 *                     new Actions() {
 *   public void action1(Matcher m) {
 *     ...
 *   }
 * 
 *   public void action2(Matcher m) {
 *     ...
 *   }
 * });
 * </pre>
 * 
 * <p>
 * There are two options for building a dispatcher:
 * 
 * <ul>
 * 
 * <li>
 * <p>
 * A dispatcher can be created with a manual mapping by using
 * {@link #start()}:
 * 
 * <pre class="java">
 * PatternDispatcher.&lt;Actions&gt;start()
 *                  .with(Pattern.compile("/action(/.*)?"),
 *                        method -> true,
 *                        receiver -> receiver::action1)
 *                  .with(Pattern.compile("/stuff(/.*)?"),
 *                        method -> true,
 *                        receiver -> receiver::action2))
 *                  .create();
 * </pre>
 * 
 * <li>
 * <p>
 * A dispatcher can be created from an annotated receiver type with
 * {@link #of(Class)}:
 * 
 * <pre class="java">
 * interface Actions {
 *   &#64;ForPath("/action(/.*)?$")
 *   &#64;Priority(1)
 *   void action1(Matcher m);
 * 
 *   &#64;ForPath("/stuff(/.*)?")
 *   void action2(Matcher m);
 * }
 * PatternDispatcher&lt;Actions&gt; testDisp =
 *       PatternDispatcher.of(Actions.class);
 * </pre>
 * 
 * <p>
 * Methods on the interface type are scanned for the presence of the
 * annotation {@link ForPath}. These must have the same or compatible
 * arguments, return type and exceptions as
 * {@link PatternHandler#accept(Matcher)}. Methods are added to the test
 * sequence in priority order as determined by the annotation
 * {@link Priority}.
 * 
 * <p>
 * Java methods with the annotation {@link ForMethod} (which may appear
 * more than once) are restricted to those specific request methods.
 * 
 * </ul>
 * 
 * @param <T> the receiver type
 * 
 * @author simpsons
 */
public final class PatternDispatcher<T> {
    private static class Entry<T> {
        Pattern pattern;

        Predicate<? super String> methods;

        Function<? super T, ? extends PatternHandler> selector;

        public Entry(Pattern pattern, Predicate<? super String> methods,
                     Function<? super T, ? extends PatternHandler> selector) {
            this.pattern = pattern;
            this.methods = methods;
            this.selector = selector;
        }

        public void accept(T receiver, Matcher matcher)
            throws SessionException,
                IOException,
                InterruptedException {
            selector.apply(receiver).accept(matcher);
        }
    }

    private final List<Entry<T>> entries;

    private PatternDispatcher(Stream<Entry<T>> entries) {
        this.entries = entries.collect(Collectors.toList());
    }

    private PatternDispatcher(List<Entry<T>> entries) {
        this.entries = entries;
    }

    /**
     * Start building a dispatcher.
     * 
     * @param <T> the receiver type
     * 
     * @return the initially empty dispatcher
     * 
     * @constructor
     */
    public static <T> Builder<T> start() {
        return new Builder<>();
    }

    /**
     * Builds a dispatcher in stages.
     * 
     * @param <T> the receiver type
     */
    public static class Builder<T> {
        private final List<Entry<T>> entries = new ArrayList<>();

        Builder() {}

        /**
         * Create a dispatcher with the current sequence of patterns and
         * actions.
         * 
         * @return the requested dispatcher
         * 
         * @constructor
         */
        public PatternDispatcher<T> create() {
            return new PatternDispatcher<>(entries);
        }

        /**
         * Append an entry to the sequence, using a pre-compiled
         * pattern, and matching specific request methods.
         * 
         * @param pattern the pattern to match against the path
         * 
         * @param methods a test for matching request methods
         * 
         * @param selector a means to select the method to be invoked on
         * the application-specific receiver
         * 
         * @return this object
         * 
         * @see #with(String, Predicate, Function)
         * 
         * @see #with(Pattern, Function)
         * 
         * @see #with(String, Function)
         */
        public Builder<T>
            with(Pattern pattern, Predicate<? super String> methods,
                 Function<? super T, ? extends PatternHandler> selector) {
            entries.add(new Entry<>(pattern, methods, selector));
            return this;
        }

        /**
         * Append an entry to the sequence, matching specific request
         * methods.
         * 
         * @param pattern the pattern to match against the path
         * 
         * @param methods a test for matching request methods
         * 
         * @param selector a means to select the method to be invoked on
         * the application-specific receiver
         * 
         * @return this object
         * 
         * @throws PatternSyntaxException if the pattern is not a valid
         * regular expression
         * 
         * @implNote This compiles the pattern with
         * {@link Pattern#compile(String)}, and then passes the result
         * and other arguments to {@link #with(Pattern, Function)}.
         */
        public Builder<T>
            with(String pattern, Predicate<? super String> methods,
                 Function<? super T, ? extends PatternHandler> selector) {
            return with(Pattern.compile(pattern), methods, selector);
        }

        /**
         * Append an entry to the sequence, using a pre-compiled
         * pattern.
         * 
         * @param pattern the pattern to match against the path
         * 
         * @param selector a means to select the method to be invoked on
         * the application-specific receiver
         * 
         * @return this object
         * 
         * @implNote This invokes
         * {@link #with(Pattern, Predicate, Function)}, passing a
         * predicate that always returns {@code true} as the second
         * argument.
         */
        public Builder<T>
            with(Pattern pattern,
                 Function<? super T, ? extends PatternHandler> selector) {
            return with(pattern, m -> true, selector);
        }

        /**
         * Append an entry to the sequence.
         * 
         * @param pattern the pattern to match against the path
         * 
         * @param methods a test for matching request methods
         * 
         * @param selector a means to select the method to be invoked on
         * the application-specific receiver
         * 
         * @return this object
         * 
         * @throws PatternSyntaxException if the pattern is not a valid
         * regular expression
         * 
         * @implNote This invokes
         * {@link #with(String, Predicate, Function)}, passing a
         * predicate that always returns {@code true} as the second
         * argument.
         */
        public Builder<T>
            with(String pattern,
                 Function<? super T, ? extends PatternHandler> selector) {
            return with(pattern, m -> true, selector);
        }
    }

    /**
     * Dispatch to an action matching a given request method and path.
     * 
     * @param method the request method
     * 
     * @param target the path to match against the actions' regular
     * expressions
     * 
     * @param receiver the object to pass the matching regular
     * expression on to
     * 
     * @return {@code true} if pattern and method set were matched;
     * {@code false} otherwise
     * 
     * @throws InterruptedException if the application was interrupted
     * (usually by the server or the remote client aborting the session)
     * 
     * @throws SessionException if the application is temporarily unable
     * to respond to the request
     * 
     * @throws IOException if an I/O error occurs in processing any of
     * the streams of the session context
     */
    public boolean dispatch(String method, CharSequence target, T receiver)
        throws SessionException,
            IOException,
            InterruptedException {
        return dispatch(method, target, () -> receiver);
    }

    /**
     * Dispatch to an action matching a given request method and path,
     * creating the receiver on demand.
     * 
     * @param method the request method
     * 
     * @param target the path to match against the actions' regular
     * expressions
     * 
     * @param receiver provider of the object to pass the matching
     * regular expression on to
     * 
     * @return {@code true} if pattern and method set were matched;
     * {@code false} otherwise
     * 
     * @throws InterruptedException if the application was interrupted
     * (usually by the server or the remote client aborting the session)
     * 
     * @throws SessionException if the application is temporarily unable
     * to respond to the request
     * 
     * @throws IOException if an I/O error occurs in processing any of
     * the streams of the session context
     */
    public boolean dispatch(String method, CharSequence target,
                            Supplier<? extends T> receiver)
        throws SessionException,
            IOException,
            InterruptedException {
        for (var opt : entries) {
            if (!opt.methods.test(method)) continue;
            var m = opt.pattern.matcher(target);
            if (m.matches()) {
                opt.accept(receiver.get(), m);
                return true;
            }
        }
        return false;
    }

    private static final Collection<Class<?>> THROWN_TYPES;

    static {
        Set<Class<?>> acceptable = new HashSet<>();
        acceptable.add(RuntimeException.class);
        acceptable.add(Error.class);
        acceptable.addAll(Arrays
            .asList(PatternHandler.class.getMethods()[0].getExceptionTypes()));
        THROWN_TYPES = Set.copyOf(acceptable);
    }

    private static boolean badException(Class<?> type) {
        for (var okay : THROWN_TYPES)
            if (okay.isAssignableFrom(type)) return false;
        return true;
    }

    private static int priority(Method m) {
        var ann = m.getAnnotation(Priority.class);
        if (ann == null) return Integer.MAX_VALUE;
        return ann.value();
    }

    /**
     * Create a dispatcher for an interface type with annotated methods.
     * Only methods annotated with {@link ForPath} are considered, and
     * the value of that annotation is taken as the regular expression
     * to match on. A method may also be annotated with multiple
     * {@link ForMethod}s, indicating the set of request methods that
     * the Java method would be invoked for; if absent, it is used for
     * all request methods. An optional {@link Priority} annotation
     * enforces ordering.
     * 
     * @param <T> the receiver type
     * 
     * @param type a reflection of the interface type
     * 
     * @param returnType a reflection of the result type
     * 
     * @return the dispatcher for the interface type
     * 
     * @throws IllegalArgumentException if an annotated method has the
     * wrong signature, including return type and thrown exceptions, or
     * is inaccessible
     * 
     * @throws PatternSyntaxException if the pattern applied to a Java
     * method with {@link ForPath} is not a valid regular expression
     * 
     * @constructor
     */
    public static <T> PatternDispatcher<T> of(Class<T> type) {
        var lookup = MethodHandles.lookup();

        List<Map.Entry<Integer, Entry<T>>> options = new ArrayList<>();
        for (var method : type.getMethods()) {
            try {
                var pathAnnot = method.getAnnotation(ForPath.class);
                if (pathAnnot == null) continue;

                /* Check that the method has the right signature. TODO:
                 * Formally check this against
                 * PatternHandler.accept(Matcher). */
                var params = method.getParameterTypes();
                if (params.length != 1)
                    throw new IllegalArgumentException("bad number of params: "
                        + method);
                if (!params[0].isAssignableFrom(Matcher.class))
                    throw new IllegalArgumentException("bad param type: "
                        + method);
                if (Void.TYPE != method.getReturnType())
                    throw new IllegalArgumentException("bad return type: "
                        + method);
                for (var ext : method.getExceptionTypes())
                    if (badException(ext))
                        throw new IllegalArgumentException("bad exception "
                            + ext + " on " + method);

                /* Compile the pattern. */
                var pattern = Pattern.compile(pathAnnot.value());

                /* Establish which methods the method is limited to. */
                var methsAnnot = method.getAnnotation(ForMethods.class);
                final Predicate<String> methods;
                if (methsAnnot == null) {
                    var methAnnot = method.getAnnotation(ForMethod.class);
                    if (methAnnot == null) {
                        methods = s -> true;
                    } else {
                        var va = methAnnot.value();
                        methods = s -> s.equals(va);
                    }
                } else {
                    methods = Arrays.asList(methsAnnot.value()).stream()
                        .map(ForMethod::value)
                        .collect(Collectors.toSet())::contains;
                }

                /* Get a reference to the method, and a way to bind it
                 * to the receiver supplied later. */
                MethodHandle ref = lookup.unreflect(method);
                Function<T, PatternHandler> selector =
                    (receiver) -> toPatternHandler(ref, receiver);
                options.add(Map.entry(priority(method),
                                      new Entry<>(pattern, methods, selector)));
            } catch (IllegalAccessException ex) {
                throw new IllegalArgumentException("inaccessible", ex);
            }
        }

        return new PatternDispatcher<>(options.stream()
            .sorted((a, b) -> Integer.compare(a.getKey(), b.getKey()))
            .map(Map.Entry::getValue));
    }

    private interface Test {
        @ForPath("^/foo/([^/]+)/.*")
        @ForMethod("GET")
        @ForMethod("POST")
        void m1(Matcher m) throws IOException;
    }

    private static final PatternDispatcher<Test> testDisp =
        PatternDispatcher.of(Test.class);

    @SuppressWarnings("unchecked")
    private static <T> PatternHandler toPatternHandler(MethodHandle ref,
                                                       T receiver) {
        return MethodHandleProxies.asInterfaceInstance(PatternHandler.class,
                                                       ref.bindTo(receiver));
    }

    /**
     * @hidden
     */
    public static void main(String[] args) throws Exception {
        var rcv = new Test() {
            @Override
            public void m1(Matcher m) {
                System.out.printf("$1=%s%n", m.group(1));
            }
        };

        boolean rc = testDisp.dispatch("GET", "/foo/bar/baz", rcv);
        System.out.printf("rc=%s%n", rc);

        PatternDispatcher.<Test>start()
            .with(Pattern.compile(""), s -> true, r -> r::m1).create();
    }
}
