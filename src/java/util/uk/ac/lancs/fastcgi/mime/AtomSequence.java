// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2023, Lancaster University
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

package uk.ac.lancs.fastcgi.mime;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Holds an immutable sequence of atoms.
 *
 * @author simpsons
 */
public class AtomSequence extends AbstractList<String> {
    private final String[] base;

    /**
     * Get the atom at a given position.
     * 
     * @param index the index
     * 
     * @return the atom
     */
    @Override
    public String get(int index) {
        return base[index];
    }

    /**
     * Get the number of atoms.
     * 
     * @return the number of atoms
     */
    @Override
    public int size() {
        return base.length;
    }

    /**
     * Create an atom sequence from a sequence of strings.
     * 
     * @param src the source strings
     * 
     * @throws IllegalAtomException if an encountered string is not an
     * atom
     */
    public AtomSequence(Collection<? extends CharSequence> src) {
        base = new String[src.size()];
        int i = 0;
        for (var e : src) {
            if (!Tokenizer.isAtom(e))
                throw new IllegalAtomException(e.toString());
            base[i++] = e.toString();
        }
    }

    /**
     * Create an atom sequence which is a concatenation of two others.
     * 
     * @param src1 the first source sequence
     * 
     * @param src2 the second source sequence
     */
    public AtomSequence(AtomSequence src1, AtomSequence src2) {
        base = new String[src1.size() + src2.size()];
        int i = 0;
        for (var e : src1)
            base[i++] = e;
        for (var e : src2)
            base[i++] = e;
    }

    private static AtomSequence decode(List<? extends CharSequence> parts) {
        List<String> result = new ArrayList<>();
        for (var cs : parts) {
            Tokenizer toks = new Tokenizer(cs);
            toks.whitespace(0);
            CharSequence tok;
            while ((tok = toks.atom()) != null) {
                result.add(tok.toString());
                toks.whitespace(0);
                if (!toks.character(',')) break;
                toks.whitespace(0);
            }
            if (!toks.end())
                throw new IllegalArgumentException("not atom sequence: " + cs);
        }
        return new AtomSequence(result);
    }

    private static List<CharSequence> encode(AtomSequence tokens) {
        if (tokens == null || tokens.isEmpty()) return Collections.emptyList();
        StringBuilder result = new StringBuilder();
        String sep = "";
        for (var t : tokens) {
            result.append(sep).append(t);
            sep = ", ";
        }
        return Collections.singletonList(result.toString());
    }

    /**
     * Converts atom sequences. The encoder translates an atom sequence
     * into a single comma-separated list, unless it is empty or
     * {@code null}, which yields an empty list instead. The decoder
     * splits multiple strings on commas, and concatenates the results.
     * Coalescing is concatenation.
     * 
     * <p>
     * {@link IllegalArgumentException} is thrown if a string is not a
     * comma-separated atom sequence.
     */
    public static final Format<AtomSequence> FORMAT =
        Format.of("atom-sequence", AtomSequence.class, AtomSequence::decode,
                  AtomSequence::encode, AtomSequence::new);
}
