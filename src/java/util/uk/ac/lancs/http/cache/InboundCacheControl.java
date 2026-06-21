// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2025, Lancaster University
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
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import uk.ac.lancs.http.field.FieldId;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Parses and holds the parameters of a <code>Cache-Control</code>
 * header field.
 * 
 * @author simpsons
 */
public final class InboundCacheControl {
    private static void parse(CharSequence line, Map<String, String> qualified,
                              Collection<String> unqualified) {
        Tokenizer tokens = new Tokenizer(line);
        boolean expected = false;
        for (;; expected = true) {
            tokens.whitespace(0);
            if (tokens.end()) {
                if (expected) {
                    tokens.abort("more directives expected");
                    throw new AssertionError("unreachable");
                }
                return;
            }

            var key = tokens.atom();
            if (key == null) {
                tokens.abort("directive key expected");
                throw new AssertionError("unreachable");
            }
            tokens.whitespace(0);
            if (tokens.character(',')) {
                unqualified.add(key.toString());
                continue;
            }
            if (tokens.character('=')) {
                tokens.whitespace(0);
                var val = tokens.atom();
                if (val == null) val = tokens.quotedString();
                if (val == null) {
                    tokens.abort("directive value expected");
                    throw new AssertionError("unreachable");
                }
                qualified.put(key.toString(), val.toString());
                continue;
            }
            tokens.whitespace(0);
            if (tokens.end()) break;
            if (tokens.character(',')) continue;
            tokens.abort("comma/end expcected");
            throw new AssertionError("unreachable");
        }
    }

    public static final int UNLIMITED = -2;

    public static final int UNSPECIFIED = -1;

    /**
     * Derive cache-control parameters from a field value, without
     * regard to whether it is in a request or a response.
     * 
     * @param line the comma-concatenated value of the
     * <code>Cache-Control</code> field
     * 
     * @return the requested collection of parsed directives
     */
    public static InboundCacheControl of(CharSequence line) {
        return new InboundCacheControl(line, 0);
    }

    /**
     * Derive cache-control parameters from a field value, ignoring
     * directives inappropriate for a response.
     * 
     * @param line the comma-concatenated value of the
     * <code>Cache-Control</code> field
     * 
     * @return the requested collection of parsed directives
     */
    public static InboundCacheControl ofRequest(CharSequence line) {
        return new InboundCacheControl(line, -1);
    }

    /**
     * Derive cache-control parameters from a field value, ignoring
     * directives inappropriate for a request.
     * 
     * @param line the comma-concatenated value of the
     * <code>Cache-Control</code> field
     * 
     * @return the requested collection of parsed directives
     */
    public static InboundCacheControl ofResponse(CharSequence line) {
        return new InboundCacheControl(line, +1);
    }

    /**
     * Derive cache-control parameters from a header field value.
     * 
     * @param line the comma-concatenated value of the
     * <code>Cache-Control</code> field
     * 
     * @param mode negative if directives invalid in a response field
     * are to be ignored; positive if directives in a request field are
     * to be ignored
     * 
     * @throws NumberFormatException if a directive's value is not a
     * number when expected to be
     * 
     * @throws IllegalArgumentException if the line does not parse as a
     * <code>Cache-Control</code> value
     */
    private InboundCacheControl(CharSequence line, int mode) {
        Map<String, String> qualifiedDirectives =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Collection<String> unqualifiedDirectives =
            new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        parse(line, qualifiedDirectives, unqualifiedDirectives);

        this.noCache = unqualifiedDirectives.contains(Directives.NO_CACHE);
        this.noStore = unqualifiedDirectives.contains(Directives.NO_STORE);
        this.noTransform =
            unqualifiedDirectives.contains(Directives.NO_TRANSFORM);
        this.staleIfError =
            unqualifiedDirectives.contains(Directives.STALE_IF_ERROR);

        if (qualifiedDirectives.containsKey(Directives.MAX_AGE)) {
            this.maxAge =
                Integer.parseInt(qualifiedDirectives.get(Directives.MAX_AGE));
            if (this.maxAge < 0)
                throw new NumberFormatException("-ve " + Directives.MAX_AGE);
        } else
            maxAge = UNSPECIFIED;

        /* These directives are only valid in requests. */
        if (mode <= 0) {
            this.onlyIfCached =
                unqualifiedDirectives.contains(Directives.ONLY_IF_CACHED);
            if (unqualifiedDirectives.contains(Directives.MAX_STALE))
                this.maxStale = UNLIMITED;
            else if (qualifiedDirectives.containsKey(Directives.MAX_STALE)) {
                this.maxStale = Integer
                    .parseInt(qualifiedDirectives.get(Directives.MAX_STALE));
                if (this.maxStale < 0) throw new NumberFormatException("-ve "
                    + Directives.MAX_STALE);
            } else
                maxStale = UNSPECIFIED;

            if (qualifiedDirectives.containsKey(Directives.MIN_FRESH)) {
                this.minFresh = Integer
                    .parseInt(qualifiedDirectives.get(Directives.MIN_FRESH));
                if (this.minFresh < 0) throw new NumberFormatException("-ve "
                    + Directives.MIN_FRESH);
            } else
                minFresh = UNSPECIFIED;
        } else {
            this.onlyIfCached = false;
            this.maxStale = UNSPECIFIED;
            this.minFresh = UNSPECIFIED;
        }

        /* These directives are only valid in responses. */
        if (mode >= 0) {
            if (qualifiedDirectives.containsKey(Directives.NO_CACHE)) {
                Tokenizer vtoks =
                    new Tokenizer(qualifiedDirectives.get(Directives.NO_CACHE));
                Collection<String> rawNames =
                    new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                var f = vtoks.whitespaceAtom(0);
                if (f == null) {
                    vtoks.abort("token expected");
                    throw new AssertionError("unreachable");
                }
                rawNames.add(f.toString());
                while (vtoks.whitespaceCharacter(0, ',') &&
                    (f = vtoks.whitespaceAtom(0)) != null) {
                    rawNames.add(f.toString());
                }
                vtoks.whitespace(0);
                if (!vtoks.end()) {
                    vtoks.abort("end or \",\" expected");
                    throw new AssertionError("unreachable");
                }

                /* TODO: Convert the raw names into field ids. */
            }
            if (qualifiedDirectives.containsKey(Directives.S_MAXAGE)) {
                this.sMaxAge = Integer
                    .parseInt(qualifiedDirectives.get(Directives.S_MAXAGE));
                if (this.sMaxAge < 0) throw new NumberFormatException("-ve "
                    + Directives.S_MAXAGE);
            } else
                sMaxAge = UNSPECIFIED;

            this.mustRevalidate =
                unqualifiedDirectives.contains(Directives.MUST_REVALIDATE);
            this.proxyRevalidate =
                unqualifiedDirectives.contains(Directives.PROXY_REVALIDATE);
            this.mustUnderstand =
                unqualifiedDirectives.contains(Directives.MUST_UNDERSTAND);
            this.private_ = unqualifiedDirectives.contains(Directives.PRIVATE);
            this.public_ = unqualifiedDirectives.contains(Directives.PUBLIC);
            this.immutable =
                unqualifiedDirectives.contains(Directives.IMMUTABLE);
            this.staleWhileRevalidate = unqualifiedDirectives
                .contains(Directives.STALE_WHILE_REVALIDATE);
        } else {
            sMaxAge = UNSPECIFIED;
            this.mustRevalidate = false;
            this.proxyRevalidate = false;
            this.mustUnderstand = false;
            this.private_ = false;
            this.public_ = false;
            this.immutable = false;
            this.staleWhileRevalidate = false;
        }
    }

    private final int maxAge, maxStale, minFresh, sMaxAge;

    private final boolean noCache, noTransform, onlyIfCached, noStore,
        staleIfError, mustRevalidate, proxyRevalidate, mustUnderstand, private_,
        public_, immutable, staleWhileRevalidate;

    private final Collection<FieldId> noCacheFields = new HashSet<>();

    /**
     * Determine whether a specific field name has been
     * 
     * @param fieldId
     * 
     * @return {@code true} if an unqualified
     * {@value Directives#NO_CACHE} directive was specified, or the
     * given field was specified in the directive; {@code false}
     * otherwise
     */
    public boolean noCache(FieldId fieldId) {
        return noCache || noCacheFields.contains(fieldId);
    }

    /**
     * Determine whether the {@value Directives#MUST_REVALIDATE}
     * directive is present.
     * 
     * @return {@code true} if the directive is present; {@code false}
     * otherwise
     */
    public boolean mustRevalidate() {
        return mustRevalidate;
    }

    /**
     * Determine whether the {@value Directives#PROXY_VALIDATE}
     * directive is present.
     * 
     * @return {@code true} if the directive is present; {@code false}
     * otherwise
     */
    public boolean proxyRevalidate() {
        return proxyRevalidate;
    }

    /**
     * Determine whether the {@value Directives#MUST_UNDERSTAND}
     * directive is present.
     * 
     * @return {@code true} if the directive is present; {@code false}
     * otherwise
     */
    public boolean mustUnderstand() {
        return mustUnderstand;
    }

    /**
     * Determine whether the {@value Directives#PRIVATE} directive is
     * present.
     * 
     * @return {@code true} if the directive is present; {@code false}
     * otherwise
     */
    public boolean private_() {
        return private_;
    }

    /**
     * Determine whether the {@value Directives#PUBLIC} directive is
     * present.
     * 
     * @return {@code true} if the directive is present; {@code false}
     * otherwise
     */
    public boolean public_() {
        return public_;
    }

    /**
     * Determine whether the {@value Directives#IMMUTABLE} directive is
     * present.
     * 
     * @return {@code true} if the directive is present; {@code false}
     * otherwise
     */
    public boolean immutable() {
        return immutable;
    }

    /**
     * Determine whether the {@value Directives#STALE_WHILE_REVALIDATE}
     * directive is present.
     * 
     * @return {@code true} if the directive is present; {@code false}
     * otherwise
     */
    public boolean staleWhileRevalidate() {
        return staleWhileRevalidate;
    }

    /**
     * Determine whether the {@value Directives#STALE_IF_ERROR}
     * directive is present.
     * 
     * @return {@code true} if the directive is present; {@code false}
     * otherwise
     */
    public boolean staleIfError() {
        return staleIfError;
    }

    /**
     * Determine whether the {@value Directives#NO_STORE} directive is
     * present.
     * 
     * @return {@code true} if the directive is present; {@code false}
     * otherwise
     */
    public boolean noStore() {
        return noStore;
    }

    /**
     * Get the argument of the {@value Directives#S_MAXAGE} directive.
     * 
     * @return the non-negative integral value of the directive; or
     * {@link #UNSPECIFIED} if not specified
     */
    public int sMaxAge() {
        return sMaxAge;
    }

    /**
     * Get the argument of the {@value Directives#MAX_AGE} directive.
     * 
     * @return the non-negative integral value of the directive; or
     * {@link #UNSPECIFIED} if not specified
     * 
     * @see https://www.rfc-editor.org/rfc/rfc9111.html#name-max-age
     * RFC9111 §5.2.1.1 <code>max-age</code>
     */
    public int maxAge() {
        return maxAge;
    }

    /**
     * Get the argument of the {@value Directives#MAX_STALE} directive.
     * 
     * @return the non-negative integral value of the directive; or
     * {@link #UNLIMITED} if the directive was specified without a
     * value; or {@link #UNSPECIFIED} if not specified
     * 
     * @see https://www.rfc-editor.org/rfc/rfc9111.html#name-max-stale
     * RFC9111 §5.2.1.2 <code>max-stale</code>
     */
    public int maxStale() {
        return maxStale;
    }

    /**
     * Get the argument of the {@value Directives#MIN_FRESH} directive.
     * 
     * @return the non-negative integral value of the directive; or
     * {@code -1} if not specified
     */
    public int minFresh() {
        return minFresh;
    }

    /**
     * Determine whether the {@value Directives#NO_CACHE} directive is
     * present.
     * 
     * @return {@code true} if the directive is present; {@code false}
     * otherwise
     * 
     * @see https://www.rfc-editor.org/rfc/rfc9111.html#name-no-cache
     * RFC9111 §5.2.1.4 <code>no-cache</code>
     */
    public boolean noCache() {
        return noCache;
    }

    /**
     * Determine whether the {@value Directives#NO_TRANSFORM} directive
     * is present.
     * 
     * @return {@code true} if the directive is present; {@code false}
     * otherwise
     */
    public boolean noTransform() {
        return noTransform;
    }

    /**
     * Determine whether the {@value Directives#ONLY_IF_CACHED}
     * directive is present.
     * 
     * @return {@code true} if the directive is present; {@code false}
     * otherwise
     */
    public boolean onlyIfCached() {
        return onlyIfCached;
    }
}
