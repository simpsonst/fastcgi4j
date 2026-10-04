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

/**
 * Provides a basic implementation of a cache directive, including its
 * key token, internal state type, and whether it is for requests and/or
 * responses.
 *
 * @param <S> the internal state type
 * 
 * @author simpsons
 */
public abstract class AbstractCacheDirective<S> implements CacheDirective<S> {
    private final String key;

    private final Class<S> type;

    private final boolean forResponses;

    private final boolean forRequests;

    /**
     * Create an abstract cache directive.
     * 
     * @param key the token identifying the directive
     * 
     * @param type the internal state type
     * 
     * @param forRequests {@code true} if the directive applies to
     * requests; {@code false otherwise}
     * 
     * @param forResponses {@code true} if the directive applies to
     * responses; {@code false otherwise}
     */
    protected AbstractCacheDirective(Class<S> type, String key,
                                     boolean forRequests,
                                     boolean forResponses) {
        this.key = key;
        this.type = type;
        this.forRequests = forRequests;
        this.forResponses = forResponses;
    }

    @Override
    public final String key() {
        return key;
    }

    @Override
    public Class<S> type() {
        return type;
    }

    @Override
    public boolean forResponses() {
        return forResponses;
    }

    @Override
    public boolean forRequests() {
        return forRequests;
    }
}
