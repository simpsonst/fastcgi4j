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

package uk.ac.lancs.http.field;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.ac.lancs.http.FieldNameSets;
import uk.ac.lancs.http.FieldNames;

/**
 * Holds a valid header or trailer field name, possibly with a
 * namespace. A field identifier can also be deemed just for use in a
 * response header, a response trailer, or both.
 *
 * @author simpsons
 */
public final class FieldId {
    private static final String FIELD_PATTERN_TEXT =
        "^[a-zA-Z][0-9a-zA-Z]*(-[a-zA-Z][0-9a-zA-Z]*)*$";

    private static final Pattern FIELD_PATTERN =
        Pattern.compile(FIELD_PATTERN_TEXT);

    private final FieldNamespace namespace;

    private final String core;

    /**
     * Identify a field by namespace and core name.
     * 
     * @param namespace the namespace
     * 
     * @param core the core name
     * 
     * @throws IllegalArgumentException if the core name fails to match
     * the regular expression <samp>{@value "%s"
     * #FIELD_PATTERN_TEXT}</samp>
     * 
     * @throws NullPointerException if either argument is {@code null}
     */
    FieldId(FieldNamespace namespace, String core) {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(core, "core");
        Matcher m = FIELD_PATTERN.matcher(core);
        if (!m.matches())
            throw new IllegalArgumentException("bad field id core: " + core);
        this.namespace = namespace;
        this.core = core;
    }

    /**
     * Get the field's namespace.
     * 
     * @return the field's namespace
     */
    public FieldNamespace namespace() {
        return namespace;
    }

    /**
     * Get the field name core.
     * 
     * @return the core used to name the field
     */
    public String name() {
        return core;
    }

    /**
     * Get the full prefixed name of this field, given a mapping
     * context.
     * 
     * @param table the mapping from extension to prefix
     * 
     * @return the full prefixed name within the given context
     * 
     * @throws NoSuchElementException if the field's namespace is not
     * present in the table
     */
    public String prefixedName(ExtensionTable table) {
        return namespace.prefix(table) + core;
    }

    /**
     * Get the full prefixed name of this field, if present in a mapping
     * context.
     * 
     * @param table the mapping from extension to prefix
     * 
     * @return the full prefixed name within the given context
     */
    public Optional<String> optionalPrefixedName(ExtensionTable table) {
        return namespace.optionalPrefix(table).map(s -> s + core);
    }

    /**
     * Get the name as it appears in the CGI environment. That is, the
     * plain name is converted to upper case, dashes are replaced with
     * underscores.
     * 
     * @return the converted name
     */
    public String gatewayName() {
        return core.replace('-', '_').toUpperCase(Locale.ROOT);
    }

    /**
     * Get a string representation of this identifier.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        return core + '/' + namespace;
    }

    /**
     * Get the hash code of this object. This combines the hash codes of
     * its namespace and core components.
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.namespace);
        hash = 23 * hash + Objects.hashCode(this.core.toLowerCase(Locale.ROOT));
        return hash;
    }

    /**
     * Test whether another object is equal to this object.
     * 
     * @param obj the object to test
     * 
     * @return {@code true} if the object is a {@link FieldId}, and has
     * an identical namespace and core name (case-insensitive);
     * {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final FieldId other = (FieldId) obj;
        if (!Objects.equals(this.core.toLowerCase(Locale.ROOT),
                            other.core.toLowerCase(Locale.ROOT)))
            return false;
        return Objects.equals(this.namespace, other.namespace);
    }

    /**
     * Remove all instances of a field.
     * 
     * @param cap the raw destination fields
     * 
     * @throws UnsupportedOperationException if the cap is not for
     * output
     */
    public final void clear(Cap cap) {
        cap.get(this).clear();
    }

    /**
     * Replace all instances of the field with a new value.
     * 
     * @param cap the raw destination fields
     * 
     * @param elem the new value
     * 
     * @throws UnsupportedOperationException if the cap is not for
     * output
     */
    public final void set(Cap cap, CharSequence text) {
        var vals = cap.get(this);
        vals.clear();
        vals.add(text.toString());
    }

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#LOCATION}</samp>.
     */
    public static final FieldId LOCATION =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.LOCATION);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#CONTENT_DIGEST}</samp>.
     */
    public static final FieldId CONTENT_DIGEST =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.CONTENT_DIGEST);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#CONTENT_TYPE}</samp>.
     */
    public static final FieldId CONTENT_TYPE =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.CONTENT_TYPE);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#CONTENT_ENCODING}</samp>.
     */
    public static final FieldId CONTENT_ENCODING =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.CONTENT_ENCODING);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#ACCEPT}</samp>.
     */
    public static final FieldId ACCEPT =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.ACCEPT);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#ACCEPT_CHARSET}</samp>.
     */
    public static final FieldId ACCEPT_CHARSET =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.ACCEPT_CHARSET);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#ACCEPT_ENCODING}</samp>.
     */
    public static final FieldId ACCEPT_ENCODING =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.ACCEPT_ENCODING);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#ACCEPT_LANGUAGE}</samp>.
     */
    public static final FieldId ACCEPT_LANGUAGE =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.ACCEPT_LANGUAGE);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#AGE}</samp>.
     */
    public static final FieldId AGE =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.AGE);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#ALLOW}</samp>.
     */
    public static final FieldId ALLOW =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.ALLOW);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#AUTHORIZATION}</samp>.
     */
    public static final FieldId AUTHORIZATION =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.AUTHORIZATION);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#CACHE_CONTROL}</samp>.
     */
    public static final FieldId CACHE_CONTROL =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.CACHE_CONTROL);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#CONTENT_BASE}</samp>.
     */
    public static final FieldId CONTENT_BASE =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.CONTENT_BASE);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#CONTENT_LANGUAGE}</samp>.
     */
    public static final FieldId CONTENT_LANGUAGE =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.CONTENT_LANGUAGE);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#CONTENT_LOCATION}</samp>.
     */
    public static final FieldId CONTENT_LOCATION =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.CONTENT_LOCATION);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#CONTENT_MD5}</samp>.
     */
    public static final FieldId CONTENT_MD5 =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.CONTENT_MD5);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#CONTENT_RANGE}</samp>.
     */
    public static final FieldId CONTENT_RANGE =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.CONTENT_RANGE);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#COOKIE}</samp>.
     */
    public static final FieldId COOKIE =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.COOKIE);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#DATE}</samp>.
     */
    public static final FieldId DATE =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.DATE);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#ETAG}</samp>.
     */
    public static final FieldId ETAG =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.ETAG);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#EXPIRES}</samp>.
     */
    public static final FieldId EXPIRES =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.EXPIRES);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#EXT}</samp>.
     */
    public static final FieldId EXT =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.EXT);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#FROM}</samp>.
     */
    public static final FieldId FROM =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.FROM);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#HOST}</samp>.
     */
    public static final FieldId HOST =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.HOST);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#IF_MATCH}</samp>.
     */
    public static final FieldId IF_MATCH =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.IF_MATCH);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#IF_MODIFIED_SINCE}</samp>.
     */
    public static final FieldId IF_MODIFIED_SINCE =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.IF_MODIFIED_SINCE);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#IF_NONE_MATCH}</samp>.
     */
    public static final FieldId IF_NONE_MATCH =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.IF_NONE_MATCH);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#IF_RANGE}</samp>.
     */
    public static final FieldId IF_RANGE =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.IF_RANGE);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#IF_UNMODIFIED_SINCE}</samp>.
     */
    public static final FieldId IF_UNMODIFIED_SINCE =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.IF_UNMODIFIED_SINCE);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#LAST_MODIFIED}</samp>.
     */
    public static final FieldId LAST_MODIFIED =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.LAST_MODIFIED);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#MAN}</samp>.
     */
    public static final FieldId MAN =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.MAN);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#OPT}</samp>.
     */
    public static final FieldId OPT =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.OPT);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#PRAGMA}</samp>.
     */
    public static final FieldId PRAGMA =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.PRAGMA);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#RANGE}</samp>.
     */
    public static final FieldId RANGE =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.RANGE);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#REFERER}</samp>.
     */
    public static final FieldId REFERER =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.REFERER);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#REPR_DIGEST}</samp>.
     */
    public static final FieldId REPR_DIGEST =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.REPR_DIGEST);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#RETRY_AFTER}</samp>.
     */
    public static final FieldId RETRY_AFTER =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.RETRY_AFTER);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#SERVER}</samp>.
     */
    public static final FieldId SERVER =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.SERVER);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#SET_COOKIE}</samp>.
     */
    public static final FieldId SET_COOKIE =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.SET_COOKIE);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#USER_AGENT}</samp>.
     */
    public static final FieldId USER_AGENT =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.USER_AGENT);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#VARY}</samp>.
     */
    public static final FieldId VARY =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.VARY);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#VIA}</samp>.
     */
    public static final FieldId VIA =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.VIA);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#WANT_CONTENT_DIGEST}</samp>.
     */
    public static final FieldId WANT_CONTENT_DIGEST =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.WANT_CONTENT_DIGEST);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#WANT_REPR_DIGEST}</samp>.
     */
    public static final FieldId WANT_REPR_DIGEST =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.WANT_REPR_DIGEST);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#WARNING}</samp>.
     */
    public static final FieldId WARNING =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.WARNING);

    /**
     * Identifies the standard end-to-end header field
     * <samp>{@value "%s" FieldNames#WWW_AUTHENTICATE}</samp>.
     */
    public static final FieldId WWW_AUTHENTICATE =
        FieldNamespace.STANDARD_END_TO_END.of(FieldNames.WWW_AUTHENTICATE);

    /* Content-Length must be hop-by-hop, because it is forbidden to use
     * it with Transfer-Encoding, which could change on each hop. */
    /**
     * Identifies the standard hop-by-hop header field
     * <samp>{@value "%s" FieldNames#CONTENT_LENGTH}</samp>.
     */
    public static final FieldId CONTENT_LENGTH =
        FieldNamespace.STANDARD_HOP_BY_HOP.of(FieldNames.CONTENT_LENGTH);

    /**
     * Identifies the standard hop-by-hop header field
     * <samp>{@value "%s" FieldNames#C_EXT}</samp>.
     */
    public static final FieldId C_EXT =
        FieldNamespace.STANDARD_HOP_BY_HOP.of(FieldNames.C_EXT);

    /**
     * Identifies the standard hop-by-hop header field
     * <samp>{@value "%s" FieldNames#C_MAN}</samp>.
     */
    public static final FieldId C_MAN =
        FieldNamespace.STANDARD_HOP_BY_HOP.of(FieldNames.C_MAN);

    /**
     * Identifies the standard hop-by-hop header field
     * <samp>{@value "%s" FieldNames#C_OPT}</samp>.
     */
    public static final FieldId C_OPT =
        FieldNamespace.STANDARD_HOP_BY_HOP.of(FieldNames.C_OPT);

    /**
     * Identifies the standard hop-by-hop header field
     * <samp>{@value "%s" FieldNames#CONNECTION}</samp>.
     */
    public static final FieldId CONNECTION =
        FieldNamespace.STANDARD_HOP_BY_HOP.of(FieldNames.CONNECTION);

    /**
     * Identifies the standard hop-by-hop header field
     * <samp>{@value "%s" FieldNames#MAX_FORWARDS}</samp>.
     */
    public static final FieldId MAX_FORWARDS =
        FieldNamespace.STANDARD_HOP_BY_HOP.of(FieldNames.MAX_FORWARDS);

    /**
     * Identifies the standard hop-by-hop header field
     * <samp>{@value "%s" FieldNames#PROXY_AUTHENTICATE}</samp>.
     */
    public static final FieldId PROXY_AUTHENTICATE =
        FieldNamespace.STANDARD_HOP_BY_HOP.of(FieldNames.PROXY_AUTHENTICATE);

    /**
     * Identifies the standard hop-by-hop header field
     * <samp>{@value "%s" FieldNames#PROXY_AUTHORIZATION}</samp>.
     */
    public static final FieldId PROXY_AUTHORIZATION =
        FieldNamespace.STANDARD_HOP_BY_HOP.of(FieldNames.PROXY_AUTHORIZATION);

    /**
     * Identifies the standard hop-by-hop header field
     * <samp>{@value "%s" FieldNames#PUBLIC}</samp>.
     */
    public static final FieldId PUBLIC =
        FieldNamespace.STANDARD_HOP_BY_HOP.of(FieldNames.PUBLIC);

    /**
     * Identifies the standard hop-by-hop header field
     * <samp>{@value "%s" FieldNames#TE}</samp>.
     */
    public static final FieldId TE =
        FieldNamespace.STANDARD_HOP_BY_HOP.of(FieldNames.TE);

    /**
     * Identifies the standard hop-by-hop header field
     * <samp>{@value "%s" FieldNames#TRAILER}</samp>.
     */
    public static final FieldId TRAILER =
        FieldNamespace.STANDARD_HOP_BY_HOP.of(FieldNames.TRAILER);

    /**
     * Identifies the standard hop-by-hop header field
     * <samp>{@value "%s" FieldNames#TRANSFER_ENCODING}</samp>.
     */
    public static final FieldId TRANSFER_ENCODING =
        FieldNamespace.STANDARD_HOP_BY_HOP.of(FieldNames.TRANSFER_ENCODING);

    /**
     * Identifies the standard hop-by-hop header field
     * <samp>{@value "%s" FieldNames#UPGRADE}</samp>.
     */
    public static final FieldId UPGRADE =
        FieldNamespace.STANDARD_HOP_BY_HOP.of(FieldNames.UPGRADE);

    private static final Set<FieldId> ILLEGALLY_SCOPED_FIELDS = Stream
        .concat(Stream
            .concat(Stream
                .concat(Stream.concat(FieldNameSets.GENERAL_END_TO_END.stream(),
                                      FieldNameSets.REQUEST_END_TO_END
                                          .stream()),
                        FieldNameSets.ENTITY_END_TO_END.stream()),
                    FieldNameSets.RESPONSE_END_TO_END.stream())
            .map(s -> FieldNamespace.STANDARD_HOP_BY_HOP.of(s)),
                Stream
                    .concat(Stream
                        .concat(Stream
                            .concat(FieldNameSets.GENERAL_HOP_BY_HOP.stream(),
                                    FieldNameSets.REQUEST_HOP_BY_HOP.stream()),
                                FieldNameSets.ENTITY_HOP_BY_HOP.stream()),
                            FieldNameSets.RESPONSE_HOP_BY_HOP.stream())
                    .map(s -> FieldNamespace.STANDARD_END_TO_END.of(s)))
        .collect(Collectors.toSet());

    /**
     * Determine whether a field identifier has an illegal scope. To
     * have an illegal scope, it must have a standard core name, but
     * belong to the wrong scope. For example, <samp>Connection</samp>
     * is a hop-by-hop header, so it is an error to use such an
     * identifier defined in {@link FieldNamespace#STANDARD_END_TO_END}.
     * 
     * @param id the identifier to test
     * 
     * @return {@code true} if the field identifier has an illegal
     * scope; {@code false} otherwise
     */
    public static boolean hasIllegalScope(FieldId id) {
        return ILLEGALLY_SCOPED_FIELDS.contains(id);
    }
}
