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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Associates a field identifier with its format and internal type.
 * 
 * <p>
 * The format is defined by a parser (a function from tokenizer to the
 * internal type) and/or a generator (a function from the internal type
 * to a string). If the parser is absent, the field is
 * &lsquo;out-only&rsquo;, and the {@link #get(Cap)} method will be
 * inoperative. If the generator is absent, the field is
 * &lsquo;in-only&rsquo;, and the methods {@link #set(Cap, List)},
 * {@link #set(Cap, Object)}, {@link #add(Cap, Object)} and
 * {@link #clear(Cap)} are inoperative.
 * 
 * <p>
 * A field may be sequential or flat. A sequential field can be present
 * multiple times in a header or trailer, and is semantically equivalent
 * to a single field whose values are separated by commas. When a flat
 * field exists multiple times in a header or trailer, its last instance
 * defines the effective value, and other fields are ignored.
 *
 * @author simpsons
 * 
 * @param <T> the internal type
 */
public final class Field<T> {
    private final FieldId id;

    private final Function<Tokenizer, T> parser;

    private final Function<? super T, ?> generator;

    private final boolean flat;

    private Field(FieldId id, Function<Tokenizer, T> parser,
                  Function<? super T, ?> generator, boolean flat) {
        this.id = id;
        this.parser = parser;
        this.generator = generator;
        this.flat = flat;
    }

    private void checkIn() {
        if (parser == null)
            throw new UnsupportedOperationException("out-only field");
    }

    private void checkOut() {
        if (generator == null)
            throw new UnsupportedOperationException("in-only field");
    }

    private String generate(T arg) {
        return generator.apply(arg).toString();
    }

    private T parse(Tokenizer t) {
        return parser.apply(t);
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
        if (flat) {
            if (elems.isEmpty()) return;
            var last = elems.get(elems.size() - 1);
            vals.add(generate(last));
        } else {
            for (var e : elems)
                vals.add(generate(e));
        }
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
    public void set(Cap cap, T elem) {
        checkOut();
        var vals = cap.get(id);
        vals.clear();
        vals.add(generate(elem));
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
        if (flat) {
            set(cap, elem);
        } else {
            checkOut();
            var vals = cap.get(id);
            vals.add(generate(elem));
        }
    }

    /**
     * Remove all instances of the field.
     * 
     * @param cap the raw destination fields
     * 
     * @throws UnsupportedOperationException if the cap is not for
     * output
     */
    public void clear(Cap cap) {
        checkOut();
        cap.get(id).clear();
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
        if (flat) {
            List<String> raw = cap.get(id);
            if (raw.isEmpty()) Collections.emptyList();
            var last = raw.get(raw.size() - 1);
            Tokenizer t = new Tokenizer(last);
            return Collections.singletonList(parse(t));
        } else {
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
                            t.abort("unterminated sequence");
                            throw new AssertionError("unreachable");
                        }
                        t.abort("wrong type");
                        throw new AssertionError("unreachable");
                    }
                    t.whitespace(0);
                    if (t.end()) break;
                    if (t.character(',')) continue;
                    t.abort("comma in sequence");
                    throw new AssertionError("unreachable");
                }
                return r;
            }).flatMap(List::stream).collect(Collectors.toList());
        }
    }

    /**
     * Define a bi-directional sequential field.
     * 
     * @constructor
     * 
     * @param <T> the internal field type
     * 
     * @param id the field identifier
     * 
     * @param generator a means to convert from the internal type to a
     * string
     * 
     * @param parser a means to convert tokens into the internal type
     * 
     * @return the requested field
     * 
     * @throws NullPointerException if any argument is {@code null}
     */
    public static <T> Field<T>
        sequential(FieldId id,
                   Function<? super T, ? extends CharSequence> generator,
                   Function<Tokenizer, T> parser) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(generator, "generator");
        Objects.requireNonNull(parser, "parser");
        return new Field<>(id, parser, generator, false);
    }

    /**
     * Define a bi-directional flat field.
     * 
     * @constructor
     * 
     * @param <T> the internal field type
     * 
     * @param id the field identifier
     * 
     * @param generator a means to convert from the internal type to a
     * string
     * 
     * @param parser a means to convert tokens into the internal type
     * 
     * @return the requested field
     * 
     * @throws NullPointerException if any argument is {@code null}
     */
    public static <T> Field<T>
        flat(FieldId id, Function<? super T, ? extends CharSequence> generator,
             Function<Tokenizer, T> parser) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(generator, "generator");
        Objects.requireNonNull(parser, "parser");
        return new Field<>(id, parser, generator, true);
    }

    /**
     * Define an outward sequential field.
     * 
     * @constructor
     * 
     * @param <T> the internal field type
     * 
     * @param id the field identifier
     * 
     * @param generator a means to convert from the internal type to a
     * string
     * 
     * @return the requested field
     * 
     * @throws NullPointerException if either argument is {@code null}
     */
    public static <T> Field<T>
        outwardSequential(FieldId id,
                          Function<? super T,
                                   ? extends CharSequence> generator) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(generator, "generator");
        return new Field<>(id, null, generator, false);
    }

    /**
     * Define an outward flat field.
     * 
     * @constructor
     * 
     * @param <T> the internal field type
     * 
     * @param id the field identifier
     * 
     * @param generator a means to convert from the internal type to a
     * string
     * 
     * @return the requested field
     * 
     * @throws NullPointerException if either argument is {@code null}
     */
    public static <T> Field<T> outwardFlat(FieldId id,
                                           Function<? super T, ?> generator) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(generator, "generator");
        return new Field<>(id, null, generator, true);
    }

    /**
     * Define an inward sequential field.
     * 
     * @constructor
     * 
     * @param <T> the internal field type
     * 
     * @param id the field identifier
     * 
     * @param parser a means to convert tokens into the internal type
     * 
     * @return the requested field
     * 
     * @throws NullPointerException if either argument is {@code null}
     */
    public static <T> Field<T> inwardSequential(FieldId id,
                                                Function<Tokenizer, T> parser) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(parser, "parser");
        return new Field<>(id, parser, null, false);
    }

    /**
     * Define an inward flat field.
     * 
     * @constructor
     * 
     * @param <T> the internal field type
     * 
     * @param id the field identifier
     * 
     * @param parser a means to convert tokens into the internal type
     * 
     * @return the requested field
     * 
     * @throws NullPointerException if either argument is {@code null}
     */
    public static <T> Field<T> inwardFlat(FieldId id,
                                          Function<Tokenizer, T> parser) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(parser, "parser");
        return new Field<>(id, parser, null, false);
    }
}
