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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import uk.ac.lancs.http.field.ExtensionTable;
import uk.ac.lancs.http.field.FieldId;
import uk.ac.lancs.http.FieldNames;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Holds mutable parameters of a <samp>{@value "%s"
 * FieldNames#CACHE_CONTROL}</samp> header field for transmission.
 * 
 * @author simpsons
 * 
 * @hidden
 */
@Deprecated
final class CacheControlFoo {
    private boolean immutable = false;

    private boolean mustRevalidate = false;

    private boolean proxyRevalidate = false;

    private boolean mustUnderstand = false;

    private boolean noStore = false;

    private boolean noCache = false;

    private boolean private_ = false;

    private boolean public_ = false;

    private boolean onlyIfCached = false;

    private boolean noTransform = false;

    private boolean staleIfError = false;

    private boolean staleWhileRevalidate = false;

    private final Set<FieldId> noCacheFields = new HashSet<>();

    private int maxAge = Directives.UNSPECIFIED;

    private int maxStale = Directives.UNSPECIFIED;

    private int minFresh = Directives.UNSPECIFIED;

    private int sMaxAge = Directives.UNSPECIFIED;

    private void write(ExtensionTable exts, Consumer<? super String> dest,
                       int mode) {
        if (noStore) dest.accept(Directives.NO_STORE);
        if (noTransform) dest.accept(Directives.NO_TRANSFORM);
        if (staleIfError) dest.accept(Directives.STALE_IF_ERROR);
        if (noCache) {
            dest.accept(Directives.NO_CACHE);
        } else if (mode >= 0) {
            Set<String> fields =
                noCacheFields.stream().map(id -> id.optionalPrefixedName(exts))
                    .filter(Optional::isPresent).map(Optional::get)
                    .collect(Collectors.toSet());
            if (!fields.isEmpty()) dest.accept(Directives.NO_CACHE + '='
                + fields.stream().map(name -> Tokenizer.quote(name))
                    .collect(Collectors.joining(", ")));
        }
        /* TODO: Is this suppressed if sMaxAge is set? */
        if (maxAge >= 0) dest.accept(Directives.MAX_AGE + '=' + maxAge);
        if (mode >= 0) {
            /* These directives are only valid in responses. */
            if (sMaxAge >= 0) dest.accept(Directives.S_MAXAGE + '=' + sMaxAge);
            if (mustRevalidate) dest.accept(Directives.MUST_REVALIDATE);
            if (mustUnderstand) dest.accept(Directives.MUST_UNDERSTAND);
            if (immutable) dest.accept(Directives.IMMUTABLE);
            if (staleWhileRevalidate)
                dest.accept(Directives.STALE_WHILE_REVALIDATE);
            if (proxyRevalidate) dest.accept(Directives.PROXY_REVALIDATE);
            if (public_) dest.accept(Directives.PUBLIC);
            if (private_) dest.accept(Directives.PRIVATE);
        }
        if (mode <= 0) {
            /* These directives are only valid in requests. */
            if (onlyIfCached) dest.accept(Directives.ONLY_IF_CACHED);
            if (maxStale == Directives.UNLIMITED)
                dest.accept(Directives.MAX_STALE);
            else if (maxStale >= 0)
                dest.accept(Directives.MAX_STALE + '=' + maxStale);
            if (minFresh >= 0)
                dest.accept(Directives.MIN_FRESH + '=' + minFresh);
        }
    }

    /**
     * Write the <samp>{@value "%s" FieldNames#CACHE_CONTROL}</samp>
     * response header field.
     * 
     * @param exts a table to map field extensions to internal ids
     * 
     * @param dest invoked with a value to be added to the field
     */
    public void writeInResponse(ExtensionTable exts,
                                Consumer<? super String> dest) {
        write(exts, dest, +1);
    }

    /**
     * Write the <samp>{@value "%s" FieldNames#CACHE_CONTROL}</samp>
     * request header field.
     * 
     * @param exts a table to map field extensions to internal ids
     * 
     * @param dest invoked with a value to be added to the field
     */
    public void writeInRequest(ExtensionTable exts,
                               Consumer<? super String> dest) {
        write(exts, dest, -1);
    }

    /**
     * Specify whether the <samp>{@value "%s"
     * Directives#NO_STORE}</samp> directive should be present.
     * 
     * @param state {@code true} if the directive should appear;
     * {@code false} if not
     * 
     * @return this object
     */
    public CacheControlFoo noStore(boolean state) {
        this.noStore = state;
        return this;
    }

    /**
     * Specify whether the <samp>{@value "%s" Directives#PRIVATE}</samp>
     * directive should be present.
     * 
     * @param state {@code true} if the directive should appear;
     * {@code false} if not
     * 
     * @return this object
     */
    public CacheControlFoo private_(boolean state) {
        this.private_ = state;
        return this;
    }

    /**
     * Specify whether the <samp>{@value "%s" Directives#PUBLIC}</samp>
     * directive should be present.
     * 
     * @param state {@code true} if the directive should appear;
     * {@code false} if not
     * 
     * @return this object
     */
    public CacheControlFoo public_(boolean state) {
        this.public_ = state;
        return this;
    }

    public CacheControlFoo sMaxAge(int state) {
        if (state < -1)
            throw new IllegalArgumentException("-ve sMaxAge " + state);
        this.sMaxAge = state < 0 ? Directives.UNSPECIFIED : state;
        return this;
    }

    public CacheControlFoo maxAge(int state) {
        if (state < -1)
            throw new IllegalArgumentException("-ve maxAge " + state);
        this.maxAge = state < 0 ? Directives.UNSPECIFIED : state;
        return this;
    }

    public CacheControlFoo minFresh(int state) {
        if (state < -1)
            throw new IllegalArgumentException("-ve minFresh " + state);
        this.minFresh = state < 0 ? Directives.UNSPECIFIED : state;
        return this;
    }

    public CacheControlFoo maxStale(int state) {
        if (state < -2)
            throw new IllegalArgumentException("-ve maxStale " + state);
        this.maxStale = state < -2 ? Directives.UNSPECIFIED : state;
        return this;
    }

    public CacheControlFoo unlimitedStale() {
        this.maxStale = Directives.UNLIMITED;
        return this;
    }

    public CacheControlFoo noMaxAge() {
        this.maxAge = Directives.UNSPECIFIED;
        return this;
    }

    public CacheControlFoo noMaxStale() {
        this.maxAge = Directives.UNSPECIFIED;
        return this;
    }

    public CacheControlFoo noMinFresh() {
        this.minFresh = Directives.UNSPECIFIED;
        return this;
    }

    /**
     * Specify whether the <samp>{@value "%s"
     * Directives#NO_CACHE}</samp> directive should be present.
     * 
     * @param state {@code true} if the directive should appear;
     * {@code false} if not
     * 
     * @return this object
     */
    public CacheControlFoo noCache(boolean state) {
        this.noCache = state;
        return this;
    }

    /**
     * Specify whether the <samp>{@value "%s"
     * Directives#PROXY_REVALIDATE}</samp> directive should be present.
     * 
     * @param state {@code true} if the directive should appear;
     * {@code false} if not
     * 
     * @return this object
     */
    public CacheControlFoo proxyRevalidate(boolean state) {
        this.proxyRevalidate = state;
        return this;
    }

    /**
     * Specify whether the <samp>{@value "%s"
     * Directives#ONLY_IF_CACHED}</samp> directive should be present.
     * 
     * @param state {@code true} if the directive should appear;
     * {@code false} if not
     * 
     * @return this object
     */
    public CacheControlFoo onlyIfCached(boolean state) {
        this.onlyIfCached = state;
        return this;
    }

    /**
     * Specify whether the <samp>{@value "%s"
     * Directives#NO_TRANSFORM}</samp> directive should be present.
     * 
     * @param state {@code true} if the directive should appear;
     * {@code false} if not
     * 
     * @return this object
     */
    public CacheControlFoo noTransform(boolean state) {
        this.noTransform = state;
        return this;
    }

    /**
     * Specify whether the <samp>{@value "%s"
     * Directives#STALE_IF_ERROR}</samp> directive should be present.
     * 
     * @param state {@code true} if the directive should appear;
     * {@code false} if not
     * 
     * @return this object
     */
    public CacheControlFoo staleIfError(boolean state) {
        this.staleIfError = state;
        return this;
    }

    /**
     * Specify whether the <samp>{@value "%s"
     * Directives#STALE_WHILE_REVALIDATE}</samp> directive should be
     * present.
     * 
     * @param state {@code true} if the directive should appear;
     * {@code false} if not
     * 
     * @return this object
     */
    public CacheControlFoo staleWhileRevalidate(boolean state) {
        this.staleWhileRevalidate = state;
        return this;
    }

    public CacheControlFoo noCache(FieldId id) {
        noCacheFields.add(id);
        return this;
    }

    public CacheControlFoo cache(FieldId id) {
        noCacheFields.remove(id);
        return this;
    }

    /**
     * Specify whether the <samp>{@value "%s"
     * Directives#IMMUTABLE}</samp> directive should be present.
     * 
     * @param state {@code true} if the directive should appear;
     * {@code false} if not
     * 
     * @return this object
     */
    public CacheControlFoo immutable(boolean state) {
        this.immutable = state;
        return this;
    }

    /**
     * Specify whether the <samp>{@value "%s"
     * Directives#MUST_REVALIDATE}</samp> directive should be present.
     * 
     * @param state {@code true} if the directive should appear;
     * {@code false} if not
     * 
     * @return this object
     */
    public CacheControlFoo mustRevalidate(boolean state) {
        this.mustRevalidate = state;
        return this;
    }

    /**
     * Specify whether the <samp>{@value "%s"
     * Directives#MUST_UNDERSTAND}</samp> directive should be present.
     * 
     * @param state {@code true} if the directive should appear;
     * {@code false} if not
     * 
     * @return this object
     */
    public CacheControlFoo mustUnderstand(boolean state) {
        this.mustUnderstand = state;
        return this;
    }
}
