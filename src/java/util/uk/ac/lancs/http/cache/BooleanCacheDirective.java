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

import java.util.function.Consumer;

/**
 * Implements a cache directive which is either present or absent.
 *
 * @author simpsons
 */
public class BooleanCacheDirective extends AbstractCacheDirective<Boolean> {
    /**
     * Create a Boolean cache directive.
     * 
     * @param key the token identifying the directive
     * 
     * @param forRequests {@code true} if the directive applies to
     * requests; {@code false otherwise}
     * 
     * @param forResponses {@code true} if the directive applies to
     * responses; {@code false otherwise}
     */
    public BooleanCacheDirective(String key, boolean forRequests,
                                 boolean forResponses) {
        super(Boolean.class, key, forRequests, forResponses);
    }

    @Override
    public void parse(InCacheContext ctxt, String qualification,
                      Consumer<? super Boolean> dest) {
        if (qualification == null) dest.accept(Boolean.TRUE);
    }

    @Override
    public void emit(OutCacheContext ctxt, Consumer<? super String> dest,
                     Object state) {
        if (owns((Boolean) state)) dest.accept(null);
    }

    /**
     * Test whether the directive is present.
     * 
     * @param ctrl the cache control
     * 
     * @return {@code true} if the directive is present; {@code false}
     * otherwise
     */
    public boolean test(CacheControl ctrl) {
        return owns(ctrl.get(this));
    }

    /**
     * Enable the directive in the control.
     * 
     * @param ctrl the cache control
     */
    public void set(OutCacheControl ctrl) {
        ctrl.set(this, Boolean.TRUE);
    }

    @Override
    public boolean owns(Boolean state) {
        /* Yes, we do want object identity here. It's the only value we
         * use. */
        return state == Boolean.TRUE;
    }
}
