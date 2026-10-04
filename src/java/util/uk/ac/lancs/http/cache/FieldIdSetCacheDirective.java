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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import uk.ac.lancs.http.field.FieldId;
import uk.ac.lancs.http.field.FieldResolver;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Implements a cache directive whose value is a set of namespaced field
 * identifiers.
 * 
 * @author simpsons
 */
public class FieldIdSetCacheDirective extends SetCacheDirective<FieldId> {
    /**
     * Define a field set directive.
     * 
     * @param key the token identifying the directive
     * 
     * @param forRequests {@code true} if the directive applies to
     * requests; {@code false otherwise}
     * 
     * @param forResponses {@code true} if the directive applies to
     * responses; {@code false otherwise}
     */
    public FieldIdSetCacheDirective(String key, boolean forRequests,
                                    boolean forResponses) {
        super(() -> new HashSet<>(), key, forRequests, forResponses);
    }

    @Override
    public void parse(InCacheContext ctxt, String qualification,
                      Consumer<? super Set> dest) {
        if (qualification == null) return;
        Tokenizer vtoks = new Tokenizer(qualification);
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
        Set<FieldId> fields = new HashSet<>();
        var resolver =
            new FieldResolver<CharSequence, Void>(ctxt.index, ctxt.isHopByHop,
                                                  (k, v) -> fields.add(k),
                                                  (k, r) -> {});
        for (var raw : rawNames)
            resolver.seek(raw, null);
        dest.accept(fields);
    }

    @Override
    public void emit(OutCacheContext ctxt, Consumer<? super String> dest,
                     Object state) {
        @SuppressWarnings("unchecked")
        Set<FieldId> set = (Set<FieldId>) state;
        var rawNames =
            set.stream().map(id -> id.optionalPrefixedName(ctxt.exts))
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toSet());
        if (rawNames.isEmpty()) return;
        dest.accept(rawNames.stream().collect(Collectors.joining(",")));
    }
}
