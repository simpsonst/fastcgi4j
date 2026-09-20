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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Presents a map of raw trailer fields as read-only namespaced fields.
 *
 * @author simpsons
 */
public class TrailerMapCap implements Cap {
    private static final Pattern NAME_PATTERN =
        Pattern.compile("^(X-|[0-9]{2,}-)?(.+)$", Pattern.CASE_INSENSITIVE);

    private final Map<FieldNamespace, Map<String, List<String>>> store =
        new HashMap<>();

    /**
     * Create a trailer cap from a map of raw fields.
     * 
     * @param extMgr the extension manager defining namespace extensions
     * to be recognized in the trailer; usually the same one used to
     * interpret the corresponding header
     * 
     * @param hopByHop a set of raw field names that are considered
     * hop-by-hop rather than end-to-end; normally derived from a
     * comma-separated list of tokens in the <samp>Connection</samp>
     * header field
     * 
     * @param base the base map describing the trailer, using raw field
     * names as keys, and order-preserving lists as values; discarded
     * after construction
     * 
     * @param unused destination for keys from the base that are
     * rejected, with the reason for rejection
     */
    public TrailerMapCap(ExtensionManager extMgr,
                         Collection<? extends CharSequence> hopByHop,
                         Map<? extends CharSequence,
                             ? extends List<? extends CharSequence>> base,
                         BiConsumer<? super CharSequence,
                                    ? super RejectionReason> unused) {
        var hbh = hopByHop.stream().map(Object::toString).collect(Collectors
            .toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
        for (var ent : base.entrySet()) {
            String key = ent.getKey().toString();
            var val = ent.getValue();
            FieldScope scope = hbh.contains(key) ? FieldScope.HOP_BY_HOP :
                FieldScope.END_TO_END;

            FieldNamespace ns;
            Matcher m = NAME_PATTERN.matcher(key);
            if (!m.matches()) {
                unused.accept(key, RejectionReason.MALFORMED);
                continue;
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
                ns = extMgr.seek(ExtensionPrefix.of(pfx));
                if (ns == null) {
                    unused.accept(key, RejectionReason.UNKNOWN_EXTENSION);
                    continue;
                }
                if (ns.scope() != scope) {
                    unused.accept(key, RejectionReason.SCOPE_MISMATCH);
                    continue;
                }
            }
            assert ns != null;
            store.computeIfAbsent(ns, k -> newMap()).put(tail, val.stream()
                .map(Object::toString).collect(Collectors.toList()));
        }
    }

    private static TreeMap<String, List<String>> newMap() {
        return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    public List<String> get(FieldId id) {
        var m1 = store.get(id.namespace());
        if (m1 == null) return Collections.emptyList();
        var m2 = m1.get(id.name());
        if (m2 == null) return Collections.emptyList();
        return m2;
    }
}
