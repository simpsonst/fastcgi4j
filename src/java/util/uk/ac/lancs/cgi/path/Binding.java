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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Combines a path pattern with an action to take on a matching resource
 * path.
 */
public class Binding {
    /**
     * Create an action.
     *
     * @param pattern the pattern to match the resource path against
     *
     * @param action the action to take, given the successful matcher
     * from the pattern
     *
     * @return the requested action
     *
     * @constructor
     */
    public static Binding of(Pattern pattern, PatternAction action) {
        return new Binding(pattern, action);
    }

    final Pattern pattern;

    final PatternAction action;

    Binding(Pattern pattern, PatternAction action) {
        this.pattern = pattern;
        this.action = action;
    }

    /**
     * Test whether a navigator recognizes this binding's pattern. If
     * the test succeeds, the resulting {@link Matcher} is passed to
     * this binding's action.
     * 
     * @param navigator the navigator to test
     * 
     * @return {@code true} if the navigator recognizes the pattern;
     * {@code false} otherwise
     * 
     * @throws Exception if an error occurs in performing the action
     */
    boolean attempt(Navigator navigator) throws Exception {
        Matcher m = navigator.recognize(pattern);
        if (!m.matches()) return false;
        action.accept(m);
        return true;
    }
}
