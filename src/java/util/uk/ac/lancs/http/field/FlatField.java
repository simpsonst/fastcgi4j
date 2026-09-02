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

import java.util.List;
import java.util.function.Function;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Defines a flat field that holds at most a single value.
 *
 * @author simpsons
 */
public final class FlatField<T> extends Field<T> {
    FlatField(FieldId id, Function<Tokenizer, T> parser,
              Function<? super T, ?> generator) {
        super(id, parser, generator);
    }

    /**
     * Get the value of the last occurrence of the field in a cap.
     * 
     * @param cap the raw source fields
     * 
     * @return the parsed value of the last instance of a field; or
     * {@code null} if not set
     * 
     * @throws UnsupportedOperationException if the field or the cap is
     * not for input
     * 
     * @throws FieldSyntaxException if the field is set, but does not
     * conform to the format
     */
    public T get(Cap cap) {
        checkIn();
        List<String> raw = cap.get(id);
        if (raw.isEmpty()) return null;
        var last = raw.get(raw.size() - 1);
        Tokenizer t = new Tokenizer(last);
        try {
            var r = parse(t);
            if (r == null) t.abort(id.toString());
            t.whitespace(0);
            if (!t.end()) t.abort(id.toString());
            return r;
        } catch (IllegalArgumentException ex) {
            throw new FieldSyntaxException(id.toString() + ": "
                + ex.getMessage(), ex);
        }
    }
}
