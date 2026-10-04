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

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import uk.ac.lancs.http.field.FieldNames;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Retains mutable cache directives in preparation for generating a
 * <samp>{@value "%s" FieldNames#CACHE_CONTROL}</samp> field.
 * 
 * @author simpsons
 */
public final class OutCacheControl implements CacheControl {
    private final Map<String, Map.Entry<CacheDirective<?>, Object>> states =
        new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Remove a directive. This does not remove another directive with
     * the same {@linkplain CacheDirective#key() key}.
     * 
     * @param <S> the internal state type of the directive
     * 
     * @param dir the directive to remove
     */
    public <S> void remove(CacheDirective<S> dir) {
        var state = get(dir);
        if (state == null) return;
        states.remove(dir.key());
    }

    /**
     * Get the internal state of a directive.
     * 
     * @param <S> the internal state type of the directive
     * 
     * @param dir the directive whose internal state is sought
     * 
     * @return the internal state; or {@code null} if the directive is
     * not set (even if another directive with the same
     * {@linkplain CacheDirective#key() key} is present
     */
    @Override
    public <S> S get(CacheDirective<S> dir) {
        var state = states.get(dir.key());
        if (state == null) return null;
        var t = dir.type();
        if (!t.isInstance(state)) return null;
        S s = t.cast(state);
        return dir.owns(s) ? s : null;
    }

    /**
     * Ensure that a directive belongs to this control. Any prior state
     * owned by a different directive with the same
     * {@linkplain CacheDirective#key() key} is erased. If the directive
     * already belongs to this control, no changes are made.
     * 
     * @param <S> the internal state type of the directive
     * 
     * @param dir the directive whose internal state is to be ensured
     * 
     * @param newState a means to create new, empty state if necessary
     * 
     * @return the internal state of the directive
     */
    public <S> S ensure(CacheDirective<S> dir, Supplier<S> newState) {
        var state = get(dir);
        if (state == null) {
            state = newState.get();
            states.put(dir.key(), Map.entry(dir, state));
        }
        return state;
    }

    /**
     * Set the state of a directive. Prior state owned by any directive
     * with the same {@linkplain CacheDirective#key() key} is erased,
     * even if it belongs to this directive.
     * 
     * @param <S> the internal state type of the directive
     * 
     * @param dir the directive whose internal state is to be set
     * 
     * @param newState the replacement state
     * 
     * @return the internal state of the directive
     */
    public <S> S set(CacheDirective<S> dir, S newState) {
        states.put(dir.key(), Map.entry(dir, newState));
        return newState;
    }

    private void emit(String key, String qualification,
                      Consumer<? super String> dest) {
        if (qualification == null)
            dest.accept(key);
        else
            dest.accept(key + '=' + Tokenizer.quoteOptionally(qualification));
    }

    private void write(OutCacheContext ctxt, Consumer<? super String> dest,
                       int mode) {
        for (var ent : states.values()) {
            var d = ent.getKey();
            if (mode > 0 && !d.forResponses()) continue;
            if (mode < 0 && !d.forRequests()) continue;
            d.emit(ctxt, q -> emit(d.key(), q, dest), ent.getValue());
        }
    }

    /**
     * Generate field values for the <samp>{@value "%s"
     * FieldNames#CACHE_CONTROL}</samp> header field of a request.
     * 
     * @param ctxt a context for generating values
     * 
     * @param dest an object invoked with each comma-separated value
     */
    public void forRequest(OutCacheContext ctxt,
                           Consumer<? super String> dest) {
        write(ctxt, dest, -1);
    }

    /**
     * Generate field values for the <samp>{@value "%s"
     * FieldNames#CACHE_CONTROL}</samp> header field of a response.
     * 
     * @param ctxt a context for generating values
     * 
     * @param dest an object invoked with each comma-separated value
     */
    public void forResponse(OutCacheContext ctxt,
                            Consumer<? super String> dest) {
        write(ctxt, dest, +1);
    }
}
