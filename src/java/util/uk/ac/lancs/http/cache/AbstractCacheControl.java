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

/**
 * Provides the basic readable storage for inbound and outbound cache
 * controls.
 *
 * @author simpsons
 */
public abstract class AbstractCacheControl implements CacheControl {
    /**
     * Indexes directive state by key. The value is a pair giving the
     * directive and the state. For an access to an existing state to be
     * valid, the calling directive must be object-identical to the key
     * part of the pair. This must be ensured by inserting only pairs
     * where the key part generated the value part.
     */
    protected final Map<String, Map.Entry<CacheDirective<?>, Object>> states =
        new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * {@inheritDoc}
     * 
     * @param <S> the internal state type of the directive
     * 
     * @param dir {@inheritDoc}
     * 
     * @return {@inheritDoc}
     * 
     * @implNote The directive is also considered not set even if a
     * state exists under the directive's key, when the key part of the
     * value is not that directive.
     */
    @Override
    public <S> S get(CacheDirective<S> dir) {
        /* See if we have an entry for this key. If not, the result is
         * null. */
        var pair = states.get(dir.key());
        if (pair == null) return null;

        /* There is a value. Did this directive put it in? */
        if (pair.getKey() != dir) return null;

        /* Extract the value, and cast to the right type. Prior checks
         * ensure that we have an object of the right type, so no
         * class-cast exception. */
        var t = dir.type();
        var state = pair.getValue();
        return t.cast(state);
    }
}
