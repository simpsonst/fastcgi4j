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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a parameter value annotated with an optional locale,
 * usually specifying only a language.
 * 
 * @author simpsons
 */
public final class ParameterValue {
    private static final Pattern COMPLEX_PARAMETER_NAME =
        Pattern.compile("^(?<name>[^*]+)(?:\\*(?<part>[0-9]+))?(?<enc>\\*)?$");

    private static final Pattern PERCENT_SEQUENCE =
        Pattern.compile("(?:%[0-9A-F]{2})+");

    private static final Pattern ENCODED_PARAMETER_VALUE =
        Pattern.compile("^(?<charset>[^']*)'(?<lang>[^']*)'(?<data>.*)$");

    private static int hexVal(char c) {
        return switch (c) {
        case '0' -> 0;
        case '1' -> 1;
        case '2' -> 2;
        case '3' -> 3;
        case '4' -> 4;
        case '5' -> 5;
        case '6' -> 6;
        case '7' -> 7;
        case '8' -> 8;
        case '9' -> 9;
        case 'a', 'A' -> 10;
        case 'b', 'B' -> 11;
        case 'c', 'C' -> 12;
        case 'd', 'D' -> 13;
        case 'e', 'E' -> 14;
        case 'f', 'F' -> 15;
        default -> -1;
        };
    }

    private static byte[] decodePercents(CharSequence text) {
        assert text.length() % 3 == 0;
        byte[] r = new byte[text.length() / 3];
        for (int i = 0; i < r.length; i++) {
            final int i3 = i * 3;
            assert text.charAt(i3) == '%';
            final int n1 = hexVal(text.charAt(i3 + 1));
            final int n2 = hexVal(text.charAt(i3 + 2));
            assert n1 >= 0 && n1 <= 15;
            assert n2 >= 0 && n2 <= 15;
            r[i] = (byte) ((n1 << 4) | n2);
        }
        return r;
    }

    private static void decodeParameterValue(StringBuilder result,
                                             CharSequence text,
                                             Charset charset) {
        int last = 0;
        for (Matcher m = PERCENT_SEQUENCE.matcher(text); m.find();) {
            /* Encode the part we failed to match as a literal into
             * bytes. */
            if (last < m.start())
                result.append(text.subSequence(last, m.start()));

            /* Convert the %-encoded portion into bytes. */
            result.append(new String(decodePercents(m.group()), charset));

            /* Remember where the next literal begins. */
            last = m.end();
        }

        /* Append the last unencoded part. */
        if (last < text.length())
            result.append(text.subSequence(last, text.length()));
    }

    /**
     * Convert an atom with literal and percent-encoded parts into a
     * byte array.
     * 
     * @param text the atom to be converted
     * 
     * @param charset the character encoding to convert literal parts
     * into bytes.
     * 
     * @return the byte array equivalent to the input
     */
    private static byte[] decodeParameterValue(CharSequence text,
                                               Charset charset) {
        List<byte[]> bufs = new ArrayList<>();

        /* Split the text into encoded and unencoded portions. */
        int last = 0;
        for (Matcher m = PERCENT_SEQUENCE.matcher(text); m.find();) {
            /* Encode the part we failed to match as a literal into
             * bytes. */
            if (last < m.start()) bufs.add(text.subSequence(last, m.start())
                .toString().getBytes(charset));

            /* Convert the %-encoded portion into bytes. */
            bufs.add(decodePercents(m.group()));

            /* Remember where the next literal begins. */
            last = m.end();
        }

        /* Encode the last unencoded part. */
        if (last < text.length()) bufs.add(text.subSequence(last, text.length())
            .toString().getBytes(charset));

        return concat(bufs);
    }

    /**
     * Records a parameter value being re-assembled, decoded, and
     * language-annotated.
     */
    private static class Job {
        /**
         * Indexes the parts. Iterating over this maps yields the parts
         * in numerical order.
         */
        final Map<Integer, String> parts = new TreeMap<>();

        /**
         * Remembers which parts need decoding.
         */
        final BitSet encoded = new BitSet();

        /**
         * Specifies the language. This is {@code null} by default,
         * indicating that no language was specified.
         */
        Locale locale = null;

        /**
         * Specifies the character set. We default to US-ASCII, but
         * RFC2231 seems to deprecate that.
         */
        Charset charset = StandardCharsets.US_ASCII;

        /**
         * Compute the complete decoded value of the parameter. If no
         * part has been marked as encoded, the values from
         * {@link #parts} are simply concatenated. Otherwise, each part
         * is converted to bytes. The bytes are concatenated, and
         * interpreted according to the character encoding.
         * 
         * @return the decoded value
         */
        String value() {
            if (false) {
                StringBuilder result = new StringBuilder();
                for (var e : parts.entrySet()) {
                    int pos = e.getKey();
                    String val = e.getValue();
                    if (encoded.get(pos)) {
                        decodeParameterValue(result, val, charset);
                    } else {
                        result.append(val);
                    }
                }
                return result.toString();
            } else {
                /* If no parts are encoded, we just concatenate the
                 * parts. */
                if (encoded.isEmpty()) return parts.values().stream()
                    .collect(Collectors.joining());

                /* At least one part is encoded. */
                List<byte[]> bufs = parts.entrySet().stream().map(e -> {
                    int pos = e.getKey();
                    String val = e.getValue();
                    Charset cs = false ? charset : StandardCharsets.US_ASCII;
                    if (encoded.get(pos)) {
                        return decodeParameterValue(val, cs);
                    } else {
                        return val.getBytes(cs);
                    }
                }).collect(Collectors.toList());
                final byte[] buf = concat(bufs);
                return new String(buf, charset);
            }
        }

        /**
         * Compute the annotated value. This combines the locale with
         * the result of {@link #value()}.
         * 
         * @return the annotated and decoded value
         */
        ParameterValue paramValue() {
            return ParameterValue.of(locale, value());
        }
    }

    /**
     * Collapse raw parameter values. RFC2231 permits long parameter
     * values in structured header fields. These take the form
     * <samp><var>name</var>*<var>part</var>*<samp>,
     * <samp><var>name</var>*<var>part</var><samp> or
     * <samp><var>name</var>*<samp>.
     * 
     * <p>
     * <var>part</var> indicates a part number (non-negative; no leading
     * zeros). If absent, the whole parameter is specified in a single
     * part.
     * 
     * <p>
     * The trailing <samp>*</samp> indicates an encoded value or part.
     * If not present, the value or part must be an atom or a quoted
     * string. Otherwise, it is an atom, which may include
     * <samp>%<var>XX</var></samp> sequences. Literal (unencoded)
     * sequences are converted to US-ASCII bytes. Each
     * <samp>%</samp>-encoded sequence is converted to a single byte.
     * These byte subsequences are then concatenated.
     * 
     * <p>
     * If a whole value is encoded, or part 0 is encoded, the raw
     * value/part must be prefixed by
     * <samp><var>charset</var>'<var>lang</var>'</samp>. Both of these
     * fields may be empty. <var>charset</var> indicates how decoded
     * bytes in all parts of the value are to be converted to
     * characters. (This method assumes <samp>US-ASCII</samp> if absent,
     * though this might not be correct behaviour.) <var>lang</var> is a
     * language code, which (if not empty) is converted to a
     * {@link Locale}, and added to the value as the {@link #locale}
     * field of this class.
     * 
     * 
     * <p>
     * Note that this method is quite tolerant of missing and
     * out-of-order information. Missing components are treated as
     * empty.
     *
     * @param params the raw parameters
     *
     * @return an immutable map of the decoded, concatenated and
     * language-annotated parameters
     */
    public static Map<String, ParameterValue>
        decodeParameters(Map<? extends String, ? extends String> params) {
        Map<String, Job> data = new HashMap<>();
        for (Map.Entry<? extends String, ? extends String> ent : params
            .entrySet()) {
            /* Parse the raw name into the actual name, the optional
             * part number, and the optional encoded flag. For example,
             * foo*3* indicates that the raw value is encoded, and it is
             * part 3 (starting from 0) of the total value. If no part
             * is specified, 0 is assumed. */
            String name = ent.getKey();
            Matcher m = COMPLEX_PARAMETER_NAME.matcher(name);
            assert m.matches();
            Job job = data.computeIfAbsent(m.group("name"), k -> new Job());
            int part = part(m);
            String value = ent.getValue();
            if (m.group("enc") != null) {
                /* The raw part will have to be decoded, but we haven't
                 * necessarily got the charset or language yet. */
                job.encoded.set(part);
                if (part == 0) {
                    /* The raw value of an encoded part 0 includes the
                     * charset and language of the total value. Both are
                     * optional. We assume US-ASCII, though I don't
                     * think we're supposed to (but what else are we
                     * supposed to do?). */
                    Matcher vm = ENCODED_PARAMETER_VALUE.matcher(value);
                    String cst = vm.group("charset");
                    if (!cst.isEmpty()) job.charset = Charset.forName(cst);
                    String lot = vm.group("lang");
                    if (lot != null) job.locale = Locale.of(lot);
                    /* The remaining data has yet to be decoded. */
                    value = vm.group("data");
                }
            }
            job.parts.put(part, value);
        }
        return data.entrySet().stream().collect(Collectors
            .toMap(Map.Entry::getKey, ent -> ent.getValue().paramValue()));
    }

    private static byte[] concat(Collection<byte[]> parts) {
        final int len = parts.stream().mapToInt(e -> e.length).sum();
        final byte[] res = new byte[len];
        int off = 0;
        for (var e : parts) {
            System.arraycopy(e, 0, res, off, e.length);
            off += e.length;
        }
        assert off == res.length;
        return res;
    }

    private static int part(Matcher m) {
        String text = m.group("part");
        if (text == null) return 0;
        return Integer.parseInt(text);
    }

    /**
     * Specifies the locale of the text. May be {@code null}.
     */
    public final Locale locale;

    /**
     * Specifies the text.
     */
    public final String text;

    private ParameterValue(Locale locale, String text) {
        this.locale = locale;
        this.text = text;
    }

    /**
     * Create a locale-annotated parameter value.
     * 
     * @constructor
     * 
     * @param locale the locale of the text
     * 
     * @param text the text
     * 
     * @return the requested value
     */
    public static ParameterValue of(Locale locale, String text) {
        return new ParameterValue(locale, text);
    }

    /**
     * Create an unannotated parameter value.
     * 
     * @constructor
     * 
     * @param text the text
     * 
     * @return the requested value
     */
    public static ParameterValue of(String text) {
        return new ParameterValue(null, text);
    }

    @Override
    public String toString() {
        return text;
    }

    /**
     * Get the hash code for this object.
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.locale);
        hash = 37 * hash + Objects.hashCode(this.text);
        return hash;
    }

    /**
     * Test whether this parameter value equals another object.
     * 
     * @param obj the object to test against
     * 
     * @return {@code true} if the other object is a parameter value
     * with the same locale and text; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final ParameterValue other = (ParameterValue) obj;
        if (!Objects.equals(this.text, other.text)) return false;
        return Objects.equals(this.locale, other.locale);
    }
}
