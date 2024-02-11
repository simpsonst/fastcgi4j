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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * Describes the format of a sequence of strings. This is primarily for
 * parsing header fields, including those which can appear multiple
 * times, and are to be concatenated. It therefore specifies a data
 * type, and converters to and from string sequences. It also specifies
 * how to coalesce two internal representations.
 *
 * @author simpsons
 */
public final class Format<T> {
    private final String name;

    private final Class<T> dataType;

    private final Function<? super List<? extends CharSequence>, T> decoder;

    private final Function<T, ? extends List<? extends CharSequence>> encoder;

    private final BinaryOperator<T> coalescer;

    private Format(String name, Class<T> type,
                   Function<? super List<? extends CharSequence>, T> decoder,
                   Function<T, ? extends List<? extends CharSequence>> encoder,
                   BinaryOperator<T> coalescer) {
        this.name = name;
        this.dataType = type;
        this.decoder = decoder;
        this.encoder = encoder;
        this.coalescer = coalescer;
    }

    /**
     * Get the name of this format.
     * 
     * @return the name of this format
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Create a format.
     * 
     * @param <T> the data type
     * 
     * @param name the format name
     * 
     * @param dataType a reflection of the data type
     * 
     * @param decoder a converter from a sequence of strings
     * 
     * @param encoder a converter to a sequence of strings
     * 
     * @return the requested format
     */
    public static <T> Format<T>
        of(String name, Class<T> dataType,
           Function<? super List<? extends CharSequence>, T> decoder,
           Function<T, ? extends List<? extends CharSequence>> encoder,
           BinaryOperator<T> coalescer) {
        return new Format<>(name, dataType, decoder, encoder, coalescer);
    }

    /**
     * Attempt to cast an object into this format's data type.
     * 
     * @param data the data to cast
     * 
     * @return the same data in the right type
     * 
     * @throws ClassCastException if the data is of the wrong type
     */
    public T cast(Object data) {
        return dataType.cast(data);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.dataType);
        hash = 31 * hash + Objects.hashCode(this.decoder);
        hash = 31 * hash + Objects.hashCode(this.encoder);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Format<?> other = (Format<?>) obj;
        if (!Objects.equals(this.dataType, other.dataType)) return false;
        if (!Objects.equals(this.decoder, other.decoder)) return false;
        return Objects.equals(this.encoder, other.encoder);
    }

    /**
     * Decode a sequence of strings.
     * 
     * @param parts the string sequence; not {@code null}
     * 
     * @return the internal representation of the sequence; possibly
     * {@code null} where an empty sequence is provided
     */
    public T decode(List<? extends CharSequence> parts) {
        T result = decoder.apply(parts);
        return result;
    }

    /**
     * Encode an internal representation.
     * 
     * @param data the internal representation
     * 
     * @return an external representation of the provided object; not
     * {@code null}
     * 
     * @throws ClassCastException if the object is not of the right type
     */
    public List<? extends CharSequence> encode(Object data) {
        return encoder.apply(dataType.cast(data));
    }

    /**
     * Coalesce two internal representations.
     * 
     * @param a the earlier representation
     * 
     * @param b the later representation
     * 
     * @return a representation which optionally incorporates both
     * provided in sequence
     * 
     * @throws ClassCastException if an object is not of the right type
     */
    public T coalesce(Object a, Object b) {
        return coalescer.apply(dataType.cast(a), dataType.cast(b));
    }

    private static String decodeString(List<? extends CharSequence> parts) {
        if (parts.isEmpty()) return null;
        final int len = parts.size();
        return parts.get(len - 1).toString();
    }

    private static List<String> encodeString(String data) {
        if (data == null) return Collections.emptyList();
        return Collections.singletonList(data);
    }

    /**
     * Converts plain strings. The encoder translates {@code null} into
     * an empty sequence, and other values into singleton. The decoder
     * translates an empty sequence into {@code null}, and yields only
     * the last element of non-empty sequences. Coalescing ignores the
     * former value. This models a simple header field which, if
     * repeated, is overridden by later fields.
     */
    public static final Format<String> LAST_STRING_FORMAT =
        Format.of("last-string", String.class, Format::decodeString,
                  Format::encodeString, (a, b) -> b);

    private static String decodeAtom(List<? extends CharSequence> parts) {
        if (parts.isEmpty()) return null;
        final int len = parts.size();
        final CharSequence first = parts.get(len - 1);
        Tokenizer toks = new Tokenizer(first);
        CharSequence tok;
        if ((tok = toks.whitespaceAtom(0)) != null && toks.whitespace(0) &&
            toks.end()) return tok.toString();
        throw new IllegalAtomException(first.toString());
    }

    private static List<String> encodeAtom(CharSequence atom) {
        if (atom == null) return Collections.emptyList();
        if (!Tokenizer.isAtom(atom))
            throw new IllegalAtomException(atom.toString());
        return Collections.singletonList(atom.toString());
    }

    /**
     * Converts atoms. The encoder translates {@code null} into an empty
     * sequence, and other values into singleton. The decoder translates
     * an empty sequence into {@code null}, and yields only the last
     * element of non-empty sequences. Coalescing ignores the former
     * value. This models a simple header field which, if repeated, is
     * overridden by later fields.
     * 
     * <p>
     * {@link IllegalAtomException} is thrown if a non-atom string is
     * encountered.
     */
    public static final Format<String> LAST_ATOM_FORMAT =
        Format.of("last-atom", String.class, Format::decodeAtom,
                  Format::encodeAtom, (a, b) -> b);
}
