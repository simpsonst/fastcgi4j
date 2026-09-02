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

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import uk.ac.lancs.mime.MediaType;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Defines a field in terms of its identification and format.
 * 
 * <p>
 * The format is defined by a parser (a function from tokenizer to the
 * internal type) and/or a generator (a function from the internal type
 * to a string). If the parser is absent, the field is
 * &lsquo;out-only&rsquo;, and the methods {@link FlarField#get(Cap)}
 * and {@link SequentialField#get(Cap)} will be inoperative. If the
 * generator is absent, the field is &lsquo;in-only&rsquo;, and the
 * methods {@link #set(Cap, Object)}, {@link #clear(Cap)},
 * {@link SequentialField#set(Cap, List)} and
 * {@link SequentialField#add(Cap, Object)} are inoperative.
 * 
 * <p>
 * A field may be sequential or flat. A sequential field can be present
 * multiple times in a header or trailer, and is semantically equivalent
 * to a single field whose values are separated by commas. When a flat
 * field exists multiple times in a header or trailer, its last instance
 * defines the effective value, and other fields are ignored.
 * 
 * <p>
 * Fields are defined in stages starting with {@link #of(FieldId)}.
 *
 * @author simpsons
 * 
 * @param <T> the internal type
 */
public abstract class Field<T> {
    final FieldId id;

    private final Function<Tokenizer, T> parser;

    private final Function<? super T, ?> generator;

    Field(FieldId id, Function<Tokenizer, T> parser,
          Function<? super T, ?> generator) {
        this.id = id;
        this.parser = parser;
        this.generator = generator;
    }

    void checkIn() {
        if (parser == null)
            throw new UnsupportedOperationException("out-only field");
    }

    void checkOut() {
        if (generator == null)
            throw new UnsupportedOperationException("in-only field");
    }

    /**
     * Determine whether this field can be written.
     * 
     * @return {@code true} if write operations are possible;
     * {@code false{ otherwise
     */
    public final boolean isOutward() {
        return generator != null;
    }

    /**
     * Determine whether this field can be read.
     * 
     * @return {@code true} if read operations are possible;
     * {@code false{ otherwise
     */
    public final boolean isInward() {
        return parser != null;
    }

    /**
     * Apply the configured generator to an object of the internal type
     * to convert it into a string.
     * 
     * @param arg the object to be converted
     * 
     * @return the field representation of the object
     */
    final String generate(T arg) {
        return generator.apply(arg).toString();
    }

    /**
     * Apply the configured parser to a tokenizer to extract an internal
     * representation.
     * 
     * @param t the tokenizer to extract from
     * 
     * @return the internal representation of the consumed tokens; or
     * {@code null} if not parsed
     */
    final T parse(Tokenizer t) {
        return parser.apply(t);
    }

    /**
     * Replace all instances of the field with a new value.
     * 
     * @param cap the raw destination fields
     * 
     * @param elem the new value
     * 
     * @throws UnsupportedOperationException if the field or the cap is
     * not for output
     */
    public final void set(Cap cap, T elem) {
        checkOut();
        var vals = cap.get(id);
        vals.clear();
        vals.add(generate(elem));
    }

    /**
     * Remove all instances of the field.
     * 
     * @param cap the raw destination fields
     * 
     * @throws UnsupportedOperationException if the field or the cap is
     * not for output
     */
    public final void clear(Cap cap) {
        checkOut();
        cap.get(id).clear();
    }

    /**
     * Start defining a field.
     * 
     * @param <T> the internal field type
     * 
     * @param id the field id
     * 
     * @return an object to complete the field definition
     * 
     * @constructor
     * 
     * @throws NullPointerException if the id is {@code null}
     */
    public static <T> Builder<T> of(FieldId id) {
        Objects.requireNonNull(id, "id");
        return new Builder<>(id);
    }

    /**
     * Defines an HTTP field in stages.
     * 
     * @param <T> the internal field type
     */
    public static class Builder<T> {
        private final FieldId id;

        Function<Tokenizer, T> parser;

        Function<? super T, ?> generator;

        Builder(FieldId id) {
            this.id = id;
        }

        /**
         * Set the parser for an inward field. The supplied function may
         * return {@code null} if the input cannot be tokenized, or
         * throw {@link IllegalArgumentException} if tokenization is
         * successful, but conversion is not.
         * 
         * @param parser a means to convert tokens into the internal
         * type
         * 
         * @return this object
         * 
         * @throws NullPointerException if the argument is {@code null}
         */
        public Builder<T> inward(Function<Tokenizer, T> parser) {
            Objects.requireNonNull(parser, "parser");
            this.parser = parser;
            return this;
        }

        /**
         * Set the generator for an outward field.
         * 
         * @param generator a means to convert from the internal type to
         * a string
         * 
         * @return this object
         * 
         * @throws NullPointerException if the argument is {@code null}
         */
        public Builder<T> outward(Function<? super T, ?> generator) {
            Objects.requireNonNull(generator, "generator");
            this.generator = generator;
            return this;
        }

        private void checkFunction() {
            if (parser != null) return;
            if (generator != null) return;
            throw new IllegalStateException("parser or generator must be set");
        }

        /**
         * Create a sequential field with the current configuration.
         * 
         * @return the new field
         * 
         * @constructor
         * 
         * @throws IllegalStateException if neither
         * {@link #inward(Function)} nor {@link #outward(Function)} have
         * been called.
         */
        public SequentialField<T> sequential() {
            checkFunction();
            return new SequentialField<>(id, parser, generator);
        }

        /**
         * Create a flat field with the current configuration.
         * 
         * @return the new field
         * 
         * @constructor
         * 
         * @throws IllegalStateException if neither
         * {@link #inward(Function)} nor {@link #outward(Function)} have
         * been called.
         */
        public FlatField<T> flat() {
            checkFunction();
            return new FlatField<>(id, parser, generator);
        }
    }

    private static Long decimalLong(Tokenizer t) {
        try (Tokenizer.Mark m = t.mark()) {
            var a = t.whitespaceAtom(0);
            t.whitespace(0);
            if (t.end()) {
                m.pass();
                return Long.valueOf(a.toString(), 10);
            }
        }
        return null;
    }

    private static BigInteger decimalBigInteger(Tokenizer t) {
        try (Tokenizer.Mark m = t.mark()) {
            var a = t.whitespaceAtom(0);
            t.whitespace(0);
            if (t.end()) {
                m.pass();
                return new BigInteger(a.toString(), 10);
            }
        }
        return null;
    }

    /**
     * Defines the {@value FieldId#CONTENT_TYPE_CORE} field.
     */
    public static final FlatField<MediaType> CONTENT_TYPE =
        Field.<MediaType>of(FieldId.CONTENT_TYPE).flat();

    /**
     * Defines the {@value FieldId#CONTENT_LENGTH_CORE} field using
     * {@code Long}.
     */
    public static final FlatField<Long> LONG_CONTENT_LENGTH = Field
        .<Long>of(FieldId.CONTENT_LENGTH).outward(v -> Long.toString(v, 10))
        .inward(Field::decimalLong).flat();

    /**
     * Defines the {@value FieldId#CONTENT_LENGTH_CORE} field using
     * {@link BigInteger}.
     */
    public static final FlatField<BigInteger> BIG_CONTENT_LENGTH =
        Field.<BigInteger>of(FieldId.CONTENT_LENGTH).outward(v -> v.toString())
            .inward(Field::decimalBigInteger).flat();
}
