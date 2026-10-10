// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2026, Lancaster University
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
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.ac.lancs.fastcgi.SessionException;

/**
 * Dispatches paths to a sequence of candidates, matching on regular
 * expressions and HTTP methods.
 * 
 * @param <T> the application-defined type
 * 
 * @param <R> the result type of submissions
 * 
 * @author simpsons
 */
public final class PatternDispatcher<T, R> {
    private static class Entry<T, R> {
        Pattern pattern;

        Predicate<? super String> methods;

        PatternAction<? super T, ? extends R> action;

        public Entry(Pattern pattern, Predicate<? super String> methods,
                     PatternAction<? super T, ? extends R> action) {
            this.pattern = pattern;
            this.methods = methods;
            this.action = action;
        }
    }

    private final List<Entry<T, R>> entries;

    private PatternDispatcher(Stream<Entry<T, R>> entries) {
        this.entries = entries.collect(Collectors.toList());
    }

    private PatternDispatcher(List<Entry<T, R>> entries) {
        this.entries = entries;
    }

    /**
     * Start building a dispatcher.
     * 
     * @param <T> the application-defined type
     * 
     * @param <R> the result type of submissions
     * 
     * @return the initially empty dispatcher
     * 
     * @constructor
     */
    public <T, R> Builder<T, R> start() {
        return new Builder<>();
    }

    /**
     * Builds a dispatcher in stages.
     * 
     * @param <T> the application-defined type
     * 
     * @param <R> the result type of submissions
     */
    public static class Builder<T, R> {
        private final List<Entry<T, R>> entries = new ArrayList<>();

        Builder() {}

        /**
         * Create a dispatcher with the current sequence of patterns and
         * actions.
         * 
         * @return the requested dispatcher
         * 
         * @constructor
         */
        public PatternDispatcher<T, R> create() {
            return new PatternDispatcher<>(entries);
        }

        /**
         * Append an entry to the sequence.
         * 
         * @param pattern the pattern to match against the path
         * 
         * @param methods a test for matching methods
         * 
         * @param action the action to invoke on a match
         * 
         * @return this object
         */
        public Builder<T, R>
            with(Pattern pattern, Predicate<? super String> methods,
                 PatternAction<? super T, ? extends R> action) {
            entries.add(new Entry<>(pattern, methods, action));
            return this;
        }
    }

    /**
     * Dispatch to an action matching a given HTTP method and path.
     * 
     * @param method the HTTP method
     * 
     * @param target the path to match against the actions' regular
     * expressions
     * 
     * @param receiver the object to pass the matching regular
     * expression on to
     * 
     * @param defaultValue the value to return if no action is invoked
     * 
     * @return the value of the first matching action, or the default
     * value
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
    public R dispatch(String method, CharSequence target, R defaultValue,
                      T receiver)
        throws SessionException,
            IOException,
            InterruptedException {
        for (var opt : entries) {
            if (!opt.methods.test(method)) continue;
            var m = opt.pattern.matcher(target);
            if (m.matches()) return opt.action.accept(receiver, m);
        }
        return defaultValue;
    }

    private static final Collection<Class<?>> THROWN_TYPES;

    static {
        Set<Class<?>> acceptable = new HashSet<>();
        acceptable.add(RuntimeException.class);
        acceptable.add(Error.class);
        acceptable.addAll(Arrays
            .asList(PatternAction.class.getMethods()[0].getExceptionTypes()));
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
     * Methods on the interface type are scanned for the presence of the
     * annotation {@link ForPath}. These must return {@code R}, take a
     * single {@link Matcher} argument, and throw only the exceptions
     * listed on {@link PatternAction#accept(Object, Matcher)}. Methods
     * are added to the test sequence in priority order as determine by
     * the annotation {@link Priority}.
     * 
     * <p>
     * Methods with the annotation {@link ForMethod} (which may appear
     * more than once) are restricted to those specific HTTP methods.
     * 
     * <p>
     * A dispatcher can be created statically:
     * 
     * <pre class="java">
     * interface MyStuff {
     *   &#64;ForPath("/action(/.*)?$")
     *   Void action1(Matcher m);
     *   &#64;ForPath("/stuff(/.*)?")
     *   Void action2(Matcher m);
     * }
     * 
     * static final PatternDispatcher&lt;MyStuff, Void&gt; dispatcher =
     *   PatternDispatcher.of(MyStuff.class, Void.class);
     * </pre>
     * 
     * <p>
     * It can be used later with a newly created instance of {@code T}:
     * 
     * <pre class="java">
     * ResponderSession session = ...;
     * Navigator nav = ...;
     * dispatch(session.parameters.get(CGIParameters.REQUEST_METHOD),
     *          nav.resource(), null, new MyStuff() {
     *   public Void action1(Matcher m) {
     *     ...
     *   }
     *   public Void action2(Matcher m) {
     *     ...
     *   }
     * });
     * </pre>
     * 
     * @param <T> the interface type
     * 
     * @param <R> the submission result type
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
     */
    public static <T, R> PatternDispatcher<T, R> of(Class<T> type,
                                                    Class<R> returnType) {
        var lookup = MethodHandles.lookup();

        List<Map.Entry<Integer, Entry<T, R>>> options = new ArrayList<>();
        for (var method : type.getMethods()) {
            try {
                var pathAnnot = method.getAnnotation(ForPath.class);
                if (pathAnnot == null) continue;
                var params = method.getParameterTypes();
                if (params.length != 1) continue;
                if (!params[0].isAssignableFrom(Matcher.class)) continue;
                if (!method.getReturnType().isAssignableFrom(returnType))
                    throw new IllegalArgumentException("bad return type: "
                        + method);
                for (var ext : method.getExceptionTypes())
                    if (badException(ext))
                        throw new IllegalArgumentException("bad exception "
                            + ext + " on " + method);

                var pattern = Pattern.compile(pathAnnot.value());
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
                MethodHandle ref = lookup.unreflect(method);
                PatternAction<T, R> action = (m, r) -> {
                    try {
                        return returnType.cast(ref.invoke(m, r));
                    } catch (SessionException | IOException |
                             InterruptedException | RuntimeException |
                             Error ex) {
                        throw ex;
                    } catch (Throwable ex) {
                        throw new AssertionError("unreachable", ex);
                    }
                };
                options.add(Map.entry(priority(method),
                                      new Entry<>(pattern, methods, action)));
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
        Integer m1(Matcher m) throws IOException;
    }

    private static final PatternDispatcher<Test, Integer> testDisp =
        PatternDispatcher.of(Test.class, Integer.class);

    /**
     * @hidden
     */
    public static void main(String[] args) throws Exception {
        var rcv = new Test() {
            @Override
            public Integer m1(Matcher m) {
                System.out.printf("$1=%s%n", m.group(1));
                return 10;
            }
        };

        int rc = testDisp.dispatch("GET", "/foo/bar/baz", 0, rcv);
        System.out.printf("rc=%d%n", rc);
    }
}
