// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2022,2023,2026, Lancaster University
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
 * Defines symbolic constants for directives used in the HTTP
 * <samp>Cache-Control</samp> field.
 *
 * @author simpsons
 * 
 * @see <a href=
 * "https://www.rfc-editor.org/info/rfc9111">RFC&nbsp;9111/STD&nbsp;98,
 * HTTP Caching</a>
 */
final class Directives {
    /**
     * Limits caching to caches that understand and conform to the
     * response's status code's requirements. The value is {@value}.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-must-understand">RFC&nbsp;9111
     * Response Directives: must-understand</a>
     */
    static final String MUST_UNDERSTAND = "must-understand";

    /**
     * 
     * <p>
     * The value is {@value}.
     * 
     */
    static final String IMMUTABLE = "immutable";

    /**
     * Limits caching of a response or fields to an unshared
     * (single-user) cache. The value is {@value}.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-private">RFC&nbsp;9111
     * Response Directives: private</a>
     */
    static final String PRIVATE = "private";

    /**
     * Indicates the client's deprecation of a cached response, or
     * limits caching of a response, without origin revalidation. The
     * value is {@value}.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-no-cache">RFC&nbsp;9111
     * Request Directives: no-cache</a>
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-no-cache-2">RFC&nbsp;9111
     * Response Directives: no-cache</a>
     */
    static final String NO_CACHE = "no-cache";

    /**
     * Indicates the client's deprecation of a cached response, or
     * limits caching of a response, regardless of origin revalidation.
     * <p>
     * The value is {@value}.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-no-store">RFC&nbsp;9111
     * Request Directives: no-store</a>
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-no-store-2">RFC&nbsp;9111
     * Response Directives: no-store</a>
     */
    static final String NO_STORE = "no-store";

    /**
     * Indicates the client's deprecation of a response transformed by
     * intermediaries, or limits transformation.
     * <p>
     * The value is {@value}.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-no-transform">RFC&nbsp;9111
     * Request Directives: no-transform</a>
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-no-transform-2">RFC&nbsp;9111
     * Response Directives: no-transform</a>
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110#section-7.7">Message
     * Transformations</a>
     */
    static final String NO_TRANSFORM = "no-transform";

    /**
     * Specifies an overriding maximum age of a response. The value is
     * {@value}.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-s-maxage">RFC&nbsp;9111
     * Response Directives: s-maxage</a>
     */
    static final String S_MAXAGE = "s-maxage";

    static final String STALE_WHILE_REVALIDATE = "stale-while-revalidate";

    /**
     * Authorizes storing an otherwise prohibited response. The value is
     * {@value}.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-public">RFC&nbsp;9111
     * Response Directives: public</a>
     */
    static final String PUBLIC = "public";

    /**
     * Specifies a client's deprecation of a response with insufficient
     * freshness lifetime. The value is {@value}.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-min-fresh">RFC&nbsp;9111
     * Request Directives: min-fresh</a>
     */
    static final String MIN_FRESH = "min-fresh";

    /**
     * Specifies a client's deprecation of a response beyond a certain
     * age, or how long before a response should be regarded as stale.
     * The value is {@value}.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-max-age">RFC&nbsp;9111
     * Request Directives: max-age</a>
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-max-age-2">RFC&nbsp;9111
     * Response Directives: max-age</a>
     */
    static final String MAX_AGE = "max-age";

    /**
     * Indicates that a cache must not re-use that response without
     * origin revalidation. The value is {@value}.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-must-revalidate">RFC&nbsp;9111
     * Response Directives: must-revalidate</a>
     */
    static final String MUST_REVALIDATE = "must-revalidate";

    /**
     * Specifies a client's acceptance of a not-too-stale response. The
     * value is {@value}.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-max-stale">RFC&nbsp;9111
     * Request Directives: max-stale</a>
     */
    static final String MAX_STALE = "max-stale";

    static final String STALE_IF_ERROR = "stale-if-error";

    /**
     * Indicates that a shared cache must not re-use that response
     * without origin revalidation. The value is {@value}.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-proxy-revalidate">RFC&nbsp;9111
     * Response Directives: proxy-revalidate</a>
     */
    static final String PROXY_REVALIDATE = "proxy-revalidate";

    /**
     * Indicates a client's requirement for a cached response, or a 504.
     * The value is {@value}.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9111.html#name-only-if-cached">RFC&nbsp;9111
     * Request Directives: only-if-cached</a>
     */
    static final String ONLY_IF_CACHED = "only-if-cached";

    private Directives() {}
}
