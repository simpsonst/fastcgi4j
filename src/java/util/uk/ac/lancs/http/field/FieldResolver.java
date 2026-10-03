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

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves raw field names into namespaced ones.
 *
 * @author simpsons
 * 
 * @param <K> the raw field name type, typically {@link String} or
 * {@link CharSequence}
 * 
 * @param <V> a value type, which the raw field maps to
 */
public final class FieldResolver<K, V> {
    private static final Pattern NAME_PATTERN =
        Pattern.compile("^(X-|[0-9]{2,}-)?(.+)$", Pattern.CASE_INSENSITIVE);

    private final ExtensionIndex index;

    private final Predicate<? super String> isHopByHop;

    private final BiConsumer<? super FieldId, ? super V> okay;

    private final BiConsumer<? super K, ? super RejectionReason> bad;

    /**
     * Create a resolver.
     * 
     * @param index a mapping from internal id to namespace
     * 
     * @param isHopByHop a predicate to case-insensitively recognize the
     * raw name of a hop-by-hop field
     * 
     * @param okay invoked for a successful mapping, with the namespaced
     * field identifier as the first argument, and the corresponding
     * value as the second
     * 
     * @param bad invoked for a failed mapping, with the raw field as
     * the first argument, and a reason for rejection as the second
     */
    public FieldResolver(ExtensionIndex index,
                         Predicate<? super String> isHopByHop,
                         BiConsumer<? super FieldId, ? super V> okay,
                         BiConsumer<? super K, ? super RejectionReason> bad) {
        this.index = index;
        this.isHopByHop = isHopByHop;
        this.okay = okay;
        this.bad = bad;
    }

    /**
     * Attempt to map a raw field name to a namespaced one.
     * 
     * @param k the key to map; converted with {@link Object#toString()}
     * before use
     * 
     * @param v the value corresponding to the key, to be passed to the
     * {@code okay} consumer as the second argument
     * 
     * @return {@code true} if the key was successfully mapped;
     * {@code false} otherwise
     */
    public boolean seek(K k, V v) {
        String key = k.toString();
        FieldScope scope = isHopByHop.test(key) ? FieldScope.HOP_BY_HOP :
            FieldScope.END_TO_END;

        FieldNamespace ns;
        Matcher m = NAME_PATTERN.matcher(key);
        if (!m.matches()) {
            bad.accept(k, RejectionReason.MALFORMED);
            return false;
        }
        String pfx = m.group(1);
        String tail = m.group(2);
        if (pfx == null) {
            ns = switch (scope) {
            case END_TO_END -> FieldNamespace.STANDARD_END_TO_END;
            case HOP_BY_HOP -> FieldNamespace.STANDARD_HOP_BY_HOP;
            };
        } else if (pfx.length() == 2) {
            ns = switch (scope) {
            case END_TO_END -> FieldNamespace.EXPERIMENTAL_END_TO_END;
            case HOP_BY_HOP -> FieldNamespace.EXPERIMENTAL_HOP_BY_HOP;
            };
        } else {
            ns = index.seek(InternalId.of(pfx));
            if (ns == null) {
                bad.accept(k, RejectionReason.UNKNOWN_EXTENSION);
                return false;
            }
            if (ns.scope() != scope) {
                bad.accept(k, RejectionReason.SCOPE_MISMATCH);
                return false;
            }
        }
        assert ns != null;
        okay.accept(new FieldId(ns, tail), v);
        return true;
    }

    /**
     * Attempt to map multiple raw field names to namespaced field
     * identifiers.
     * 
     * @param map a mapping from each raw name to its corresponding
     * value
     * 
     * @return the number of successful mappings
     */
    public int seek(Map<? extends K, ? extends V> map) {
        int count = 0;
        for (var ent : map.entrySet())
            if (seek(ent.getKey(), ent.getValue())) count++;
        return count;
    }
}
