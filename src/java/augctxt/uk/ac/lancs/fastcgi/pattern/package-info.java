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

/**
 * Provides tools to dispatch on request path regular expressions and
 * HTTP methods. Dispatching centres around a user-defined interface
 * type (the <dfn>receiver type</dfn>) that performs different actions
 * in each method, and where method selection is based on the following
 * elements derived from a CGI context:
 * 
 * <ul>
 * 
 * <li>request method, usually provided by the
 * {@linkplain uk.ac.lancs.fastcgi.Session#parameters() CGI parameter}
 * {@value "%s" uk.ac.lancs.cgi.CGIParameters#REQUEST_METHOD}, and
 * matched by set membership; and
 * 
 * <li>virtual path, usually provided by
 * {@link uk.ac.lancs.cgi.path.Navigator#resource()}, and matched by
 * {@linkplain java.util.regex regular expression}.
 * 
 * </ul>
 * 
 * <p>
 * For example, one might define an interface such as this:
 * 
 * <pre class="java">
 * interface Actions {
 *   void index(Matcher m) throws SessionException,
 *                                IOException,
 *                                InterruptedException;
 * 
 *   void search(Matcher m) throws SessionException,
 *                                 IOException,
 *                                 InterruptedException;
 * }
 * </pre>
 * 
 * <p>
 * <code class="java">index</code> is to be invoked for paths of the
 * form <samp>/index/<var>UUID</var></samp>, while
 * <code class="java">search</code> matches the exact path
 * <samp>/search</samp>, relying exclusively on form fields for
 * argumentation. Note that each method must be compatible with
 * {@link uk.ac.lancs.fastcgi.pattern.PatternHandler#accept(java.util.regex.Matcher)},
 * and that only the result of a pattern match is provided as
 * argumentation; further context for the request must be provided by
 * other means, which this dispatching technique does not prescribe.
 * 
 * <p>
 * A dispatcher can be built for this receiver type statically (i.e.,
 * once at start-up, not per-request). The recommended way is to
 * annotate the receiver type's methods with the acceptable request
 * methods and a regular expression that virtual paths are to be matched
 * against:
 * 
 * <pre class="java">
 * interface Actions {
 *   &#64;ForPath("^/index/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}{3}-[0-9a-fA-F]{12})$")
 *   void index(Matcher m) throws SessionException,
 *                                IOException,
 *                                InterruptedException;
 * 
 *   &#64;ForPath("^/search$")
 *   &#64;ForMethod("GET")
 *   &#64;ForMethod("POST")
 *   void search(Matcher m) throws SessionException,
 *                                 IOException,
 *                                 InterruptedException;
 * }
 * </pre>
 * 
 * <p>
 * A dispatcher for this type can now be created with a single call:
 * 
 * <pre class="java">
 * static final PatternDispatcher&lt;Actions&gt; dispatcher =
 *     PatternDispatcher.of(Actions.class);
 * </pre>
 *
 * <p>
 * (Alternatively,
 * {@link uk.ac.lancs.fastcgi.pattern.PatternDispatcher#start()} can be
 * used to build one in stages without annotations.)
 * 
 * <p>
 * Per request, a new <code class="java">Actions</code> instance is
 * created, allowing the user's choice of context to be available to the
 * methods:
 * 
 * <pre class="java">
 * // per request
 * ResponderSession sess = ...;
 * String meth = sess.parameters.get(CGIParameters.REQUEST_METHOD);
 * Navigator nav = ...;
 * Actions actions = new Actions() {
 *   void index(Matcher m)
 *     throws SessionException,
 *            IOException,
 *            InterruptedException {
 *     // Respond using sess, meth and nav...
 *     <var>...</var>
 *   }
 * 
 *   void search(Matcher m)
 *     throws SessionException,
 *            IOException,
 *            InterruptedException {
 *     // Respond using sess, meth and nav...
 *     <var>...</var>
 *   }
 * };
 * </pre>
 * 
 * <p>
 * The request can now be dispatched to the correct Java method by
 * passing the CGI method, the virtual path, and the receiver instance:
 * 
 * <pre class="java">
 * dispatcher.dispatch(meth, nav.resource(), actions);
 * </pre>
 *
 * @author simpsons
 */
package uk.ac.lancs.fastcgi.pattern;
