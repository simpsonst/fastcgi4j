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
import java.util.List;
import java.util.function.Consumer;
import uk.ac.lancs.http.field.FieldId;

/**
 * Specifies a directive that can be used on an inbound or outbound
 * cache control.
 *
 * @param <S> the internal state type
 * 
 * @author simpsons
 */
public interface CacheDirective<S> {
    /**
     * Get the directive name to recognize in parsing, or to generate.
     * No two directives with the same key may occupy a cache control.
     * 
     * @return the directive key
     */
    String key();

    /**
     * Get the type of the internal state.
     * 
     * @return the internal state type
     */
    Class<S> type();

    /**
     * Determine whether the directive qualification requires a quoted
     * string.
     * 
     * @return {@code true} if the qualification requires a quoted
     * string; {@code false} if it make be a token
     * 
     * @implNote The default is to not require a quoted string.
     */
    default boolean forceQuoting() {
        return false;
    }

    /**
     * Interpret the raw value of a cache directive.
     * 
     * @param ctxt the context for understanding the value
     * 
     * @param qualification the value of a qualified directive; or
     * {@code null} if unqualified
     * 
     * @param dest a place to submit the internal representation if
     * successfully parsed
     */
    void parse(InCacheContext ctxt, String qualification,
               Consumer<? super S> dest);

    /**
     * Write the state.
     * 
     * @param ctxt a context for understanding the state
     * 
     * @param dest the destination to write the qualification to;
     * {@code null} if the directive is unqualified; the qualification
     * will be quoted if not representable as a token
     * 
     * @param state the state to be expressed
     */
    void emit(OutCacheContext ctxt, Consumer<? super String> dest,
              Object state);

    /**
     * Determine whether this directive is suitable for responses.
     * 
     * @return {@code true} if this directive is suitable for responses;
     * {@code false} otherwise
     */
    boolean forResponses();

    /**
     * Determine whether this directive is suitable for requests.
     * 
     * @return {@code true} if this directive is suitable for requests;
     * {@code false} otherwise
     */
    boolean forRequests();

    /**
     * Remove this directive from an outward cache control.
     * 
     * @param ctrl the cache control
     * 
     * @implNote The default behaviour is to call <code class=
     * "java">ctrl.{@linkplain OutCacheControl#remove(CacheDirective) remove}(this)</code>.
     */
    default void clear(OutCacheControl ctrl) {
        ctrl.remove(this);
    }

    /**
     * The Boolean <samp>{@value "%s" Directives#NO_STORE}</samp>
     * directive for requests and responses
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-no-store
     * no-store (request)
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-no-store-2
     * no-store (response)
     */
    BooleanCacheDirective NO_STORE =
        new BooleanCacheDirective(Directives.NO_STORE, true, true);

    /**
     * The Boolean <samp>{@value "%s" Directives#NO_TRANSFORM}</samp>
     * directive for requests and responses
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-no-transform
     * no-transform (request)
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-no-transform-2
     * no-transform (response)
     */
    BooleanCacheDirective NO_TRANSFORM =
        new BooleanCacheDirective(Directives.NO_TRANSFORM, true, true);

    /**
     * The Boolean <samp>{@value "%s" Directives#STALE_IF_ERROR}</samp>
     * directive for requests and responses
     * 
     * @spec https://www.rfc-editor.org/info/rfc5861/#section-4
     * stale-if-error
     */
    IntegerCacheDirective STALE_IF_ERROR =
        new IntegerCacheDirective(Directives.STALE_IF_ERROR, true, true);

    /**
     * The Boolean <samp>{@value "%s" Directives#ONLY_IF_CACHED}</samp>
     * directive for requests
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-only-if-cached
     * only-if-cached
     */
    BooleanCacheDirective ONLY_IF_CACHED =
        new BooleanCacheDirective(Directives.ONLY_IF_CACHED, true, false);

    /**
     * The Boolean <samp>{@value "%s" Directives#NO_CACHE}</samp>
     * directive for requests and responses
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-no-cache
     * no-cache (request)
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-no-cache-2
     * no-cache (response)
     */
    BooleanCacheDirective NO_CACHE =
        new BooleanCacheDirective(Directives.NO_CACHE, true, true);

    /**
     * The {@link FieldId}-set <samp>{@value "%s"
     * Directives#NO_CACHE}</samp> directive for responses
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-no-cache-2
     * no-cache (response)
     */
    FieldIdSetCacheDirective NO_CACHE_FIELDS =
        new FieldIdSetCacheDirective(Directives.NO_CACHE, false, true);

    /**
     * The Boolean <samp>{@value "%s" Directives#MUST_REVALIDATE}</samp>
     * directive for responses
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-must-revalidate
     * must-revalidate
     */
    BooleanCacheDirective MUST_REVALIDATE =
        new BooleanCacheDirective(Directives.MUST_REVALIDATE, false, true);

    /**
     * The Boolean <samp>{@value "%s" Directives#MUST_UNDERSTAND}</samp>
     * directive for responses
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-must-understand
     * must-understand
     */
    BooleanCacheDirective MUST_UNDERSTAND =
        new BooleanCacheDirective(Directives.MUST_UNDERSTAND, false, true);

    /**
     * The Boolean <samp>{@value "%s"
     * Directives#PROXY_REVALIDATE}</samp> directive for responses
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-proxy-revalidate
     * proxy-revalidate
     */
    BooleanCacheDirective PROXY_REVALIDATE =
        new BooleanCacheDirective(Directives.PROXY_REVALIDATE, false, true);

    /**
     * The Boolean <samp>{@value "%s" Directives#PUBLIC}</samp>
     * directive for responses
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-public public
     */
    BooleanCacheDirective PUBLIC =
        new BooleanCacheDirective(Directives.PUBLIC, false, true);

    /**
     * The Boolean <samp>{@value "%s" Directives#PRIVATE}</samp>
     * directive for responses
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-private
     * private
     */
    BooleanCacheDirective PRIVATE =
        new BooleanCacheDirective(Directives.PRIVATE, false, true);

    /**
     * The Boolean <samp>{@value "%s" Directives#IMMUTABLE}</samp>
     * directive for responses
     */
    BooleanCacheDirective IMMUTABLE =
        new BooleanCacheDirective(Directives.IMMUTABLE, false, true);

    /**
     * The integer <samp>{@value "%s"
     * Directives#STALE_WHILE_REVALIDATE}</samp> directive for requests
     * 
     * @spec https://www.rfc-editor.org/info/rfc5861/#section-3
     * stale-while-revalidate
     */
    IntegerCacheDirective STALE_WHILE_REVALIDATE =
        new IntegerCacheDirective(Directives.STALE_WHILE_REVALIDATE, false,
                                  true);

    /**
     * The Boolean <samp>{@value "%s" Directives#MAX_STALE}</samp>
     * directive for requests
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-max-stale
     * max-stale
     */
    BooleanCacheDirective MAX_STALE_UNLIMITED =
        new BooleanCacheDirective(Directives.MAX_STALE, true, false);

    /**
     * The integer <samp>{@value "%s" Directives#MAX_STALE}</samp>
     * directive for requests
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-max-stale
     * max-stale
     */
    IntegerCacheDirective MAX_STALE =
        new IntegerCacheDirective(Directives.MAX_STALE, true, false);

    /**
     * The integer <samp>{@value "%s" Directives#MAX_AGE}</samp>
     * directive for requests and responses
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-max-age
     * max-age (request)
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-max-age-2
     * max-age (response)
     */
    IntegerCacheDirective MAX_AGE =
        new IntegerCacheDirective(Directives.MAX_AGE, true, true);

    /**
     * The integer <samp>{@value "%s" Directives#MIN_FRESH}</samp>
     * directive for requests
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-min-fresh
     * min-fresh
     */
    IntegerCacheDirective MIN_FRESH =
        new IntegerCacheDirective(Directives.MIN_FRESH, true, false);

    /**
     * The integer <samp>{@value "%s" Directives#S_MAXAGE}</samp>
     * directive for responses
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#name-s-maxage
     * s-maxage
     */
    IntegerCacheDirective S_MAXAGE =
        new IntegerCacheDirective(Directives.S_MAXAGE, false, true);

    /**
     * A set of all standard directives
     */
    Collection<CacheDirective<?>> ALL_DIRECTIVES = List
        .of(IMMUTABLE, MAX_AGE, MAX_STALE, MAX_STALE_UNLIMITED, MIN_FRESH,
            MUST_REVALIDATE, MUST_UNDERSTAND, NO_CACHE, NO_CACHE_FIELDS,
            NO_STORE, NO_TRANSFORM, ONLY_IF_CACHED, PRIVATE, PROXY_REVALIDATE,
            PUBLIC, STALE_IF_ERROR, STALE_WHILE_REVALIDATE, S_MAXAGE);
}
