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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Locates internal resources with awareness of its own location. A
 * navigator has two purposes, given an invocation of a script:
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
 * @author simpsons
 */
public interface Navigator {
    /**
     * Determine whether the service instance's root path can be
     * referenced. This is not the case if the instance's root is the
     * server root.
     *
     * @return {@code true} if the root path can be referenced;
     * {@code false} otherwise
     */
    boolean allowRoot();

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
    PathReference locate(String path);

    /**
     * Identify the resource as a slash-separated string. This is either
     * an empty string (meaning the service root) or a sequence of path
     * elements prefixed by forward slashes. The last path element may
     * be empty.
     * 
     * @return the resource identifier
     * 
     * @default By default, {@link #resourceElements()} is called, and
     * then the elements are concatenated with intervening forward
     * slashes.
     */
    default String resource() {
        return resourceElements().stream().collect(Collectors.joining("/"));
    }

    /**
     * Identify the resource as a sequence of path elements.
     * 
     * @return an immutable list beginning with an empty element
     */
    List<String> resourceElements();

    /**
     * Match the resource against a regular expression.
     * 
     * @param pattern the regular expression
     * 
     * @return a matcher for the resource against the expression
     * 
     * @default By default, {@link #resource()} is called, and the
     * result passed to the pattern to obtain the matcher.
     */
    default Matcher recognize(Pattern pattern) {
        return pattern.matcher(resource());
    }

    /**
     * Match the resource against a regular expression, and take an
     * action if it matches.
     * 
     * @param pattern the pattern to match the resource against
     * 
     * @param action the action to perform, given a match
     * 
     * @return {@code true} if the pattern matched and the action was
     * performed; {@code false} if the pattern did not match
     * 
     * @default By default, {@link #recognize(Pattern)} is called. If
     * the resource matches, the {@link Matcher} is passed to the second
     * argument, and then {@code true} is returned. Otherwise
     * {@code false} is returned.
     */
    default boolean recognize(Pattern pattern, PatternAction action)
        throws Exception {
        Matcher m = recognize(pattern);
        if (!m.matches()) return false;
        action.accept(m);
        return true;
    }

    /**
     * Match the resource against a sequence of regular expressions, and
     * take a corresponding action on the first match.
     * 
     * @param actions the sequence of actions to take
     * 
     * @return {@code true} if a match was found; {@code false}
     * otherwise
     * 
     * @default By default, {@link Action#attempt(Locator)} is called on
     * each action in sequence, until one returns {@code true}.
     */
    default boolean recognize(Binding... actions) throws Exception {
        for (var action : actions)
            if (action.attempt(this)) return true;
        return false;
    }
}
