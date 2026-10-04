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
import java.util.function.Consumer;
import java.util.function.Supplier;
import uk.ac.lancs.http.field.ExtensionManager;
import uk.ac.lancs.http.field.FieldNames;
import uk.ac.lancs.http.field.FieldNamespace;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Retains mutable cache directives in preparation for generating a
 * <samp>{@value "%s" FieldNames#CACHE_CONTROL}</samp> field.
 * 
 * @author simpsons
 */
public final class OutCacheControl extends AbstractCacheControl {
    private final int mode;

    /**
     * Create a cache control with a given mode.
     * 
     * @param mode -1 for a request; +1 for a response
     */
    private OutCacheControl(int mode) {
        this.mode = mode;
    }

    private void checkDirection(CacheDirective<?> dir) {
        if (mode < 0 && !dir.forResponses())
            throw new IllegalArgumentException(dir.key()
                + " not for responses");
        if (mode > 0 && !dir.forRequests())
            throw new IllegalArgumentException(dir.key() + " not for requests");
    }

    /**
     * Create a cache control for emitting a response.
     * 
     * @return the requested cache control
     */
    public static OutCacheControl forResponse() {
        return new OutCacheControl(-1);
    }

    /**
     * Create a cache control for emitting a request.
     * 
     * @return the requested cache control
     */
    public static OutCacheControl forRequest() {
        return new OutCacheControl(+1);
    }

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
     * {@inheritDoc}
     * 
     * @param <S> the internal state type of the directive
     * 
     * @param dir {@inheritDoc}
     * 
     * @return {@inheritDoc}
     * 
     * @throws IllegalArgumentException if the directive is not suitable
     * for the mode of the cache control
     */
    @Override
    public <S> S get(CacheDirective<S> dir) {
        checkDirection(dir);
        return super.get(dir);
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
     * 
     * @throws IllegalArgumentException if the directive is not suitable
     * for the mode of the cache control
     */
    public <S> S ensure(CacheDirective<S> dir, Supplier<S> newState) {
        checkDirection(dir);

        /* Get the existing state. If not present, or for a different
         * directive, we're setting/replacing it. */
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
     * 
     * @throws IllegalArgumentException if the directive is not suitable
     * for the mode of the cache control
     */
    public <S> S set(CacheDirective<S> dir, S newState) {
        checkDirection(dir);
        states.put(dir.key(), Map.entry(dir, newState));
        return newState;
    }

    private void emit(String key, String qualification, boolean forceQuotes,
                      Consumer<? super String> dest) {
        if (qualification == null)
            dest.accept(key);
        else
            dest.accept(key + '='
                + (forceQuotes ? Tokenizer.quote(qualification) :
                    Tokenizer.quoteOptionally(qualification)));
    }

    /**
     * Generate field values for the <samp>{@value "%s"
     * FieldNames#CACHE_CONTROL}</samp> header field of a request.
     * 
     * @param ctxt a context for generating values
     * 
     * @param dest an object invoked with each comma-separated value
     */
    public void write(OutCacheContext ctxt, Consumer<? super String> dest) {
        for (var ent : states.entrySet()) {
            var k = ent.getKey();
            var p = ent.getValue();
            var d = p.getKey();
            if (mode > 0 && !d.forRequests()) continue;
            if (mode < 0 && !d.forResponses()) continue;
            d.emit(ctxt, q -> emit(k, q, d.forceQuoting(), dest), p.getValue());
        }
    }

    /**
     * @hidden
     */
    public static void main(String[] args) throws Exception {
        Consumer<String> dest = s -> System.out.printf(" %s", s);
        OutCacheControl ctrl = OutCacheControl.forResponse();
        ExtensionManager exts = new ExtensionManager();
        OutCacheContext ctxt = new OutCacheContext(exts);

        System.out.print("empty:>");
        ctrl.write(ctxt, dest);
        System.out.println("<");

        System.out.print("max-stale=20 excluded as req-only:>");
        try {
            CacheDirective.MAX_STALE.set(ctrl, 20);
        } catch (IllegalArgumentException ex) {}
        ctrl.write(ctxt, dest);
        System.out.println("<");

        CacheDirective.NO_CACHE.set(ctrl);
        System.out.print("no-cache:>");
        ctrl.write(ctxt, dest);
        System.out.println("<");

        CacheDirective.MAX_AGE.set(ctrl, 10);
        System.out.print("max-stale=10:>");
        ctrl.write(ctxt, dest);
        System.out.println("<");

        CacheDirective.NO_CACHE_FIELDS
            .include(ctrl, FieldNamespace.STANDARD_END_TO_END.of("Ext"));
        CacheDirective.NO_CACHE_FIELDS
            .include(ctrl,
                     FieldNamespace.STANDARD_END_TO_END.of("Content-Type"));
        System.out.print("no-cache ext:>");
        ctrl.write(ctxt, dest);
        System.out.println("<");
    }
}
