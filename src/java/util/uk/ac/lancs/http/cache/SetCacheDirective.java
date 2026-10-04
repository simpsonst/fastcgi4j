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

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Implements a cache directive with a set value.
 * 
 * @author simpsons
 */
public abstract class SetCacheDirective<T> extends AbstractCacheDirective<Set> {
    private final Supplier<Set<T>> constructor;

    /**
     * Define a directive with a set value.
     * 
     * @param constructor a means of creating an empty set
     * 
     * @param key the token identifying the directive
     * 
     * @param forRequests {@code true} if the directive applies to
     * requests; {@code false otherwise}
     * 
     * @param forResponses {@code true} if the directive applies to
     * responses; {@code false otherwise}
     */
    public SetCacheDirective(Supplier<Set<T>> constructor, String key,
                             boolean forRequests, boolean forResponses) {
        super(Set.class, key, forRequests, forResponses);
        this.constructor = constructor;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean owns(Set state) {
        return Set.class.isInstance(state);
    }

    @SuppressWarnings("unchecked")
    private Set<T> ensureState(OutCacheControl ctrl) {
        return ctrl.ensure(this, () -> constructor.get());
    }

    /**
     * Include an element in the set.
     * 
     * @param ctrl the cache control
     * 
     * @param elem the element to include
     */
    public void include(OutCacheControl ctrl, T elem) {
        ensureState(ctrl).add(elem);
    }

    /**
     * Exclude an element from the set.
     * 
     * @param ctrl the cache control
     * 
     * @param elem the element to exclude
     */
    public void exclude(OutCacheControl ctrl, T elem) {
        var state = ctrl.get(this);
        if (state == null) return;
        state.remove(elem);
    }

    /**
     * Test whether an element is in the set.
     * 
     * @param ctrl the cache control
     * 
     * @param elem the element to test
     * 
     * @return {@code true} if the element is in the set; {@code false}
     * otherwise
     */
    public boolean test(CacheControl ctrl, T elem) {
        var state = ctrl.get(this);
        if (state == null) return false;
        return state.contains(elem);
    }

    /**
     * Get a stream of the elements in the set.
     * 
     * @param ctrl the cache control
     * 
     * @return a stream of the elements
     */
    @SuppressWarnings("unchecked")
    public Stream<T> stream(CacheControl ctrl) {
        var state = ctrl.get(this);
        if (state == null) return Stream.empty();
        return state.stream();
    }
}
