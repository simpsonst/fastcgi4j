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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Defines a sequential field capable of holding multiple values in
 * order.
 * 
 * @author simpsons
 */
public final class SequentialField<T> extends Field<T> {
    SequentialField(FieldId id, Function<Tokenizer, T> parser,
                    Function<? super T, ?> generator) {
        super(id, parser, generator);
    }

    /**
     * Replace the sequence of instances of fields with another.
     * 
     * @param cap the raw destination fields
     * 
     * @param elems the new sequence
     * 
     * @throws UnsupportedOperationException if the cap is not for
     * output
     */
    public void set(Cap cap, List<? extends T> elems) {
        checkOut();
        var vals = cap.get(id);
        vals.clear();
        for (var e : elems)
            vals.add(generate(e));
    }

    /**
     * Append an instance of the field. For a flat field, all prior
     * instances are first removed, making this call equivalent to
     * {@link #set(Cap, Object)}.
     * 
     * @param cap the raw destination fields
     * 
     * @param elem the new value
     * 
     * @throws UnsupportedOperationException if the cap is not for
     * output
     */
    public void add(Cap cap, T elem) {
        checkOut();
        var vals = cap.get(id);
        vals.add(generate(elem));
    }

    /**
     * Get the values of each distinct occurrence of the field in a cap.
     * The results are in transmission order for a sequential field. For
     * a flat field, only the last instance is significant, and is
     * returned as a singleton.
     * 
     * @param cap the raw source fields
     * 
     * @return a list of effective values
     * 
     * @throws UnsupportedOperationException if the cap is not for input
     */
    public List<T> get(Cap cap) {
        checkIn();
        List<String> raw = cap.get(id);
        return raw.stream().map(s -> {
            var t = new Tokenizer(s);
            List<T> r = new ArrayList<>();
            for (;;) {
                T v = parse(t);
                if (v == null) {
                    if (r.isEmpty()) {
                        t.whitespace(0);
                        if (t.end()) break;
                        t.abort(id.toString() + ": unterminated sequence");
                    }
                    t.abort(id.toString() + ": wrong type");
                }
                t.whitespace(0);
                if (t.end()) break;
                if (t.character(',')) continue;
                t.abort(id.toString() + ": comma in sequence");
            }
            return r;
        }).flatMap(List::stream).collect(Collectors.toList());
    }

}
