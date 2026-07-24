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

package uk.ac.lancs.http.encoding;

import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Provides some internal utilities that encoding providers might need.
 * 
 * @author simpsons
 */
class Utils {
    /**
     * Search for the first defined property within a sequence.
     * 
     * @param props the properties to search
     * 
     * @param sfx the suffix of the sought property's name
     * 
     * @param pfxs a sequence of prefixes to test in order with the
     * suffix
     * 
     * @return the first defined property's value; or {@code null} if
     * not found
     */
    static String getText(Properties props, String sfx,
                          Stream<? extends CharSequence> pfxs) {
        return pfxs.map(s -> pfxs.toString() + s).map(props::getProperty)
            .filter(Predicate.not(Objects::isNull)).findFirst().orElse(null);
    }

    /**
     * Expand each element of a stream of character sequences into
     * sub-stream formed by appending each of a sequence of suffixes.
     * For example, if the base stream is {@code a, b, c}, and the
     * suffixes are {@code 1, 2}, the new stream is
     * {@code a1, a2, b1, b2, c1, c2}.
     * 
     * @param base the base stream
     * 
     * @param sfxs additional suffixes to be applied to each element of
     * the base
     * 
     * @return the modified stream
     */
    static Stream<String> multiply(Stream<? extends CharSequence> base,
                                   CharSequence... sfxs) {
        return base.flatMap(e -> Stream.of(sfxs).map(p -> e.toString() + p));
    }

    /**
     * Search for decreasingly specific properties, and convert the
     * value of the most specific. This takes the result of
     * {@link #getText(Properties, String, Stream)}, and converts it
     * with the supplied function. If no matching property is present, a
     * default value is returned.
     * 
     * @param <T> the result type
     * 
     * @param props the properties to search in
     * 
     * @param pfxs a common prefix for all properties
     * 
     * @param sfx a common suffix for all properties
     * 
     * @param dfl the default value
     * 
     * @param conv a function converting from string to the required
     * type
     * 
     * @return the converted value of the most specific property; or the
     * provided default value
     */
    static <T> T getDefault(Properties props, String sfx, T dfl,
                            Function<? super String, T> conv,
                            Stream<? extends CharSequence> pfxs) {
        String text = getText(props, sfx, pfxs);
        if (text == null) return dfl;
        return conv.apply(text);
    }

    /**
     * Expand a sequence of prefixes with a common structure.
     * 
     * <p>
     * Each prefix <samp>&lt;pfx&gt;</samp> in the stream is expanded
     * into the following:
     * 
     * <ol>
     * 
     * <li><samp>&lt;pfx&gt;&lt;name&gt;.&lt;dir&gt;.&lt;ctxt&gt;.</samp>
     * 
     * <li><samp>&lt;pfx&gt;&lt;name&gt;.&lt;ctxt&gt;.</samp>
     * 
     * <li><samp>&lt;pfx&gt;&lt;name&gt;</samp>
     * 
     * </ol>
     * 
     * @param dir a direction, usually <samp>in</samp> or
     * <samp>out</samp>
     * 
     * @param ctxt the encoding context, whose
     * {@link EncodingContext#key key} is used as
     * <samp>&lt;ctxt&gt;</samp>
     * 
     * @param name usually the name of the encoding
     * 
     * @param pfxs a sequence of prefixes to be expanded
     * 
     * @return the expanded sequence
     */
    static Stream<String> multiplyForEncoding(String name, String dir,
                                              EncodingContext ctxt,
                                              CharSequence... pfxs) {
        return multiply(multiply(Stream.of(pfxs), name + '.'),
                        dir + '.' + ctxt.key + '.', ctxt.key + '.', "");
    }

    private Utils() {}
}
