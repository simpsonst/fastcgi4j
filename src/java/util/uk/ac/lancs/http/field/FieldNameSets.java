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

package uk.ac.lancs.http.field;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import static uk.ac.lancs.http.field.FieldNames.*;

/**
 * Defines immutable, case-insensitive sets of standard field names.
 * 
 * @author simpsons
 */
public class FieldNameSets {
    /**
     * An immutable case-insensitive set of entity end-to-end field
     * names
     */
    public static final Set<String> ENTITY_END_TO_END =
        nameSet(ALLOW, CONTENT_BASE, CONTENT_ENCODING, CONTENT_LANGUAGE,
                CONTENT_LOCATION, CONTENT_MD5, CONTENT_RANGE, CONTENT_TYPE,
                ETAG, EXPIRES, LAST_MODIFIED);

    /**
     * An immutable case-insensitive set of general end-to-end field
     * names
     */
    public static final Set<String> GENERAL_END_TO_END =
        Set.of(CACHE_CONTROL, DATE, PRAGMA, UPGRADE, VIA, MAN, OPT);

    /**
     * An immutable case-insensitive set of request hop-by-hop field
     * names
     */
    public static final Set<String> REQUEST_HOP_BY_HOP =
        nameSet(MAX_FORWARDS, PROXY_AUTHORIZATION, TE);

    /**
     * An immutable case-insensitive set of response end-to-end field
     * names
     */
    public static final Set<String> RESPONSE_END_TO_END =
        nameSet(AGE, LOCATION, RETRY_AFTER, SERVER, VARY, WARNING,
                WWW_AUTHENTICATE, EXT, SET_COOKIE);

    /**
     * An immutable case-insensitive set of request end-to-end field
     * names
     */
    public static final Set<String> REQUEST_END_TO_END =
        nameSet(ACCEPT, ACCEPT_CHARSET, ACCEPT_ENCODING, ACCEPT_LANGUAGE,
                AUTHORIZATION, FROM, HOST, IF_UNMODIFIED_SINCE, IF_MATCH,
                IF_NONE_MATCH, IF_RANGE, IF_MODIFIED_SINCE, RANGE, REFERER,
                USER_AGENT, COOKIE);

    /**
     * An immutable case-insensitive set of general hop-by-hop field
     * names
     */
    public static final Set<String> GENERAL_HOP_BY_HOP =
        nameSet(CONNECTION, TRANSFER_ENCODING, C_MAN, C_OPT, TRAILER);

    /**
     * An immutable case-insensitive set of response hop-by-hop field
     * names
     */
    public static final Set<String> RESPONSE_HOP_BY_HOP =
        nameSet(PROXY_AUTHENTICATE, PUBLIC, C_EXT);

    /**
     * An immutable case-insensitive set of entity hop-by-hop field
     * names
     */
    public static final Set<String> ENTITY_HOP_BY_HOP = nameSet(CONTENT_LENGTH);

    /**
     * Create an immutable set of case-insensitive strings.
     *
     * @param names the set of character sequences to build the set from
     *
     * @return the requested set
     */
    static Set<String> nameSet(CharSequence... names) {
        return Collections.unmodifiableSet(nameSet(names).stream()
            .map(Object::toString).collect(Collectors
                .toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER))));
    }

    private FieldNameSets() {}
}
