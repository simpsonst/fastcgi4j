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

import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * Implements an integer directive.
 *
 * @author simpsons
 */
public class IntegerCacheDirective extends AbstractCacheDirective<Integer> {
    /**
     * Create an integer directive.
     * 
     * @param key the token identifying the directive
     * 
     * @param forRequests {@code true} if the directive applies to
     * requests; {@code false otherwise}
     * 
     * @param forResponses {@code true} if the directive applies to
     * responses; {@code false otherwise}
     */
    public IntegerCacheDirective(String key, boolean forRequests,
                                 boolean forResponses) {
        super(Integer.class, key, forRequests, forResponses);
    }

    @Override
    public boolean owns(Integer state) {
        return true;
    }

    @Override
    public void parse(InCacheContext ctxt, String qualification,
                      Consumer<? super Integer> dest) {
        if (qualification == null) return;
        try {
            int value = Integer.parseInt(qualification, 10);
            dest.accept(value);
        } catch (NumberFormatException ex) {
            // ignore
        }
    }

    @Override
    public void emit(OutCacheContext ctxt, Consumer<? super String> dest,
                     Object state) {
        dest.accept(state.toString());
    }

    /**
     * Set the integer value of this directive.
     * 
     * @param ctrl the cache control
     * 
     * @param value the new value
     */
    public void set(OutCacheControl ctrl, int value) {
        ctrl.set(this, value);
    }

    /**
     * Get the integer value of this directive.
     * 
     * @param ctrl the cache control
     * 
     * @return the current value, if present
     */
    public OptionalInt get(CacheControl ctrl) {
        var o = ctrl.get(this);
        return o == null ? OptionalInt.empty() : OptionalInt.of(o);
    }
}
