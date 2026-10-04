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

package uk.ac.lancs.http.cache;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Holds an immutable set of cache directives.
 *
 * @author simpsons
 */
public final class InCacheControl implements CacheControl {
    private static void parse(CharSequence line, Map<String, String> qualified,
                              Collection<String> unqualified) {
        if (line == null) return;
        Tokenizer tokens = new Tokenizer(line);
        boolean expected = false;
        for (;; expected = true) {
            tokens.whitespace(0);
            if (tokens.end()) {
                if (expected) {
                    tokens.abort("more directives expected");
                    throw new AssertionError("unreachable");
                }
                return;
            }

            var key = tokens.atom();
            if (key == null) {
                tokens.abort("directive key expected");
                throw new AssertionError("unreachable");
            }
            tokens.whitespace(0);
            if (tokens.character(',')) {
                unqualified.add(key.toString());
                continue;
            }
            if (tokens.character('=')) {
                tokens.whitespace(0);
                var val = tokens.atom();
                if (val == null) val = tokens.quotedString();
                if (val == null) {
                    tokens.abort("directive value expected");
                    throw new AssertionError("unreachable");
                }
                qualified.put(key.toString(), val.toString());
                continue;
            }
            tokens.whitespace(0);
            if (tokens.end()) break;
            if (tokens.character(',')) continue;
            tokens.abort("comma/end expcected");
            throw new AssertionError("unreachable");
        }
    }

    private final Map<String, Map.Entry<CacheDirective<?>, Object>> states =
        new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private InCacheControl(InCacheContext ctxt,
                           Collection<? extends CacheDirective<?>> dirs,
                           CharSequence line, int mode) {
        Map<String, String> qualifiedDirectives =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Collection<String> unqualifiedDirectives =
            new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        parse(line, qualifiedDirectives, unqualifiedDirectives);
        for (var dir : dirs) {
            var key = dir.key();
            if (mode > 0 && !dir.forResponses()) continue;
            if (mode < 0 && !dir.forRequests()) continue;
            var qual = qualifiedDirectives.get(key);
            if (qual != null)
                dir.parse(ctxt, qual, s -> states.put(key, Map.entry(dir, s)));
            else if (unqualifiedDirectives.contains(key))
                dir.parse(ctxt, null, s -> states.put(key, Map.entry(dir, s)));
        }
    }

    /**
     * Create a cache control from a request field value.
     * 
     * @param ctxt a context for resolving parts of the value
     * 
     * @param dirs the set of directives to look for
     * 
     * @param line the field value
     * 
     * @return the cache control parsed from the value
     */
    public static InCacheControl
        ofRequest(InCacheContext ctxt,
                  Collection<? extends CacheDirective<?>> dirs,
                  CharSequence line) {
        return new InCacheControl(ctxt, dirs, line, -1);
    }

    /**
     * Create a cache control from a response field value.
     * 
     * @param ctxt a context for resolving parts of the value
     * 
     * @param dirs the set of directives to look for
     * 
     * @param line the field value
     * 
     * @return the cache control parsed from the value
     */
    public static InCacheControl
        ofResponse(InCacheContext ctxt,
                   Collection<? extends CacheDirective<?>> dirs,
                   CharSequence line) {
        return new InCacheControl(ctxt, dirs, line, +1);
    }

    @Override
    public <S> S get(CacheDirective<S> dir) {
        var state = states.get(dir.key());
        if (state == null) return null;
        var t = dir.type();
        if (!t.isInstance(state)) return null;
        S s = t.cast(state);
        return dir.owns(s) ? s : null;
    }
}
