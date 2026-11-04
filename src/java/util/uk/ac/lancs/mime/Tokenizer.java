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

package uk.ac.lancs.mime;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * Parses MIME header tokens, quoted strings and related structures. The
 * entire input is required at once. A copy of it is retained
 * internally, and a position is initialized at the start. Each time a
 * recognized sequence is successfully parsed, the position advances.
 * 
 * @author simpsons
 */
public final class Tokenizer {
    /**
     * Identifies RFC2045 code points in the US-ASCII range that are
     * excluded for tokens. They consist of the following punctuation:
     * 
     * <ul>
     *
     * <li>U+0028 (40; <samp>&#40;</samp>), U+0029 (41;
     * <samp>&#41;</samp>), U+005B (91; <samp>&#91;</samp>), U+005D (93;
     * <samp>&#93;</samp>)
     *
     * <li>U+003C (60; <samp>&lt;</samp>), U+003E (62;
     * <samp>&gt;</samp>), U+003D (61; <samp>=</samp>)
     *
     * <li>U+003A (58; <samp>:</samp>), U+003B (59; <samp>;</samp>),
     * U+002C (44; <samp>,</samp>), U+003F (63; <samp>?</samp>)
     *
     * <li>U+002F (47; <samp>/</samp>), U+005C (92; <samp>\</samp>)
     *
     * <li>U+0040 (64; <samp>&#64;</samp>)
     * 
     * </ul>
     * 
     * @spec https://www.rfc-editor.org/info/rfc2045/#section-5.1
     * RFC2045 &sect;5.1
     */
    public static final IntPredicate RFC2045_TSPECIALS = c -> switch (c) {
    case '(', ')', '<', '>', '@', ',', ';', ':', '\\', '?', '=', '/', '"', '[',
        ']' -> true;
    default -> false;
    };

    /**
     * Identifies digit US-ASCII code points. These are the decimal
     * digits U+0030 (48; <samp>0</samp>) to U+0039 (57; <samp>9</samp>)
     * inclusive.
     */
    public static final IntPredicate US_ASCII_DIGITS = c -> c >= 48 && c <= 57;

    /**
     * Identifies hexadecimal digit US-ASCII code points. These are:
     * 
     * <ul>
     * 
     * <li>the decimal digits U+0030 (48; <samp>0</samp>) to U+0039 (57;
     * <samp>9</samp>) inclusive;
     * 
     * <li>the upper-case letters U+0041 (65; <samp>A</samp>) to U+0046
     * (70; <samp>F</samp>) inclusive; and
     * 
     * <li>the lower-case letters U+0061 (97; <samp>a</samp>) to U+0066
     * (102; <samp>f</samp>) inclusive.
     * 
     * </ul>
     */
    public static final IntPredicate US_ASCII_HEX_DIGITS = US_ASCII_DIGITS
        .or(c -> c >= 65 && c <= 70).or(c -> c >= 97 && c <= 102);

    /**
     * Identifies upper-case alphabetic US-ASCII code points. These are
     * U+0041 (65; <samp>A</samp>) to U+005A (90; <samp>Z</samp>)
     * inclusive.
     */
    public static final IntPredicate US_ASCII_UPPERCASE_LETTERS =
        c -> c >= 65 && c <= 90;

    /**
     * Identifies lower-case alphabetic US-ASCII code points. These are
     * U+0061 (97; <samp>a</samp>) to U+007A (122; <samp>z</samp>)
     * inclusive.
     */
    public static final IntPredicate US_ASCII_LOWERCASE_LETTERS =
        c -> c >= 97 && c <= 122;

    /**
     * Identifies alphabetic US-ASCII code points. These are U+0041 (65;
     * <samp>A</samp>) to U+005A (90; <samp>Z</samp>) inclusive, and
     * U+0061 (97; <samp>a</samp>) to U+007A (122; <samp>z</samp>)
     * inclusive.
     * 
     * @see #US_ASCII_LOWERCASE_LETTERS
     * 
     * @see #US_ASCII_UPPERCASE_LETTERS
     */
    public static final IntPredicate US_ASCII_ALPHAS =
        US_ASCII_UPPERCASE_LETTERS.or(US_ASCII_LOWERCASE_LETTERS);

    /**
     * Identifies graphic US-ASCII code points. These do not include
     * control characters, not space, and are strictly the code points
     * from U+0021 (33; <samp>!</samp>) to U+007E (126; <samp>~</samp>)
     * inclusive.
     */
    public static final IntPredicate US_ASCII_GRAPHICS =
        c -> c >= 33 && c <= 126;

    /**
     * Identifies code points that can form RFC2045 tokens. These are
     * {@linkplain #US_ASCII_GRAPHICS graphic US-ASCII} characters,
     * except for {@linkplain #RFC2045_TSPECIALS some special ones}.
     * 
     * @see #US_ASCII_GRAPHICS
     * 
     * @see #RFC2045_TSPECIALS
     */
    public static final IntPredicate RFC2045_TOKEN_CHARS =
        US_ASCII_GRAPHICS.and(RFC2045_TSPECIALS.negate());

    /**
     * Identifies linear white-space characters. These are space U+0020
     * (32) and tab U+0009 (9).
     */
    public static IntPredicate LWSP_CHARS = c -> c == ' ' || c == '\t';

    private final char[] text;

    private int pos = 0;

    /**
     * Marks a position in a tokenizer. As an auto-closable, you can use
     * it as follows, calling {@link #pass()} only if a compound
     * structure completely parses:
     * 
     * <pre>
     * Tokenizer tok = ...;
     * try (var m = tok.mark()) {
     *   tok.spaces(0);
     *   String major, minor;
     *   if ((major = tok.token(Tokenizer.RFC2045_TOKEN_CHARS)) != null &amp;&amp;
     *       tok.punctuation("/") &amp;&amp;
     *       (minor = tok.token(Tokenizer.RFC2045_TOKEN_CHARS)) {
     *     m.pass();
     *     return MediaType.of(major, minor);
     *   }
     * }
     * return null;
     * </pre>
     * 
     * @author simpsons
     */
    public final class Mark implements AutoCloseable {
        private int pos = Tokenizer.this.pos;

        Mark() {}

        /**
         * Prevent reset during auto-closure.
         * 
         * @see #close()
         */
        public void pass() {
            pos = -1;
        }

        /**
         * Reset the tokenizer if not passed.
         * 
         * @see #pass()
         */
        @Override
        public void close() {
            if (pos >= 0) Tokenizer.this.pos = pos;
        }
    }

    /**
     * Get the current mark for the tokenizer.
     * 
     * @return a mark for the tokenizer at its current position
     * 
     * @constructor
     */
    public Mark mark() {
        return new Mark();
    }

    /**
     * Get the original text.
     * 
     * @return the original text as a string
     */
    @Override
    public String toString() {
        return new String(text);
    }

    /**
     * Presents a view of the text as a character sequence.
     */
    private class Substring implements CharSequence {
        private final int start;

        private final int length;

        Substring(int start, int length) {
            this.start = start;
            this.length = length;
        }

        @Override
        public int length() {
            return length;
        }

        @Override
        public char charAt(int index) {
            if (index < 0 || index >= length)
                throw new IndexOutOfBoundsException(index);
            return text[start + index];
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            if (start < 0)
                throw new IndexOutOfBoundsException("-ve start: " + start);
            if (end < 0) throw new IndexOutOfBoundsException("-ve end: " + end);
            if (end > length) throw new IndexOutOfBoundsException("end: " + end
                + ">" + length);
            if (start > end)
                throw new IndexOutOfBoundsException(Integer.toString(start)
                    + ">" + end);
            return new Substring(this.start + start, end - start);
        }

        @Override
        public String toString() {
            return new String(text, start, length);
        }
    }

    /**
     * Get the remaining text.
     * 
     * @return the original text from the current position; or
     * {@code null} if the end of the text has been parsed with
     * {@link #end()}
     */
    public CharSequence remnant() {
        int rem = remaining();
        if (rem < 0) return null;
        return new Substring(pos, rem);
    }

    /**
     * Throw an exception indicating failure to parse.
     * 
     * @param msg a message to include in the exception
     * 
     * @throws TokenException always, containing the supplied message,
     * and an indication of what had and had not been parsed
     */
    public void abort(CharSequence msg) {
        throw new TokenException(msg.toString() + ": ["
            + new String(text, 0, pos) + "][" + remnant() + "]");
    }

    /**
     * Get the number of characters remaining. This can be negative,
     * indicating that the end has been parsed using {@link #end()}.
     * 
     * @return the number of remaining characters
     */
    public int remaining() {
        return text.length - pos;
    }

    /**
     * Get the number of code points remaining. This can be negative,
     * indicating that the end has been parsed using {@link #end()}.
     * 
     * @return the number of remaining code points
     */
    public int remainingCodePoints() {
        var r = remaining();
        if (r < 0) return r;
        return Character.codePointCount(text, pos, r);
    }

    /**
     * Prepare a character sequence to be tokenized.
     * 
     * @param text the text to be tokenized
     */
    public Tokenizer(CharSequence text) {
        this.text = new char[text.length()];
        text.getChars(0, this.text.length, this.text, 0);
    }

    /**
     * Match a minimum number of code points within a set.
     * 
     * @param set a function recognizing acceptable code points
     * 
     * @param min the minimum number of code points to match
     * 
     * @param max the maximum number of code points to match
     * 
     * @return the subsequence of matching characters; or {@code null}
     * if none matched
     */
    public CharSequence match(IntPredicate set, int min, int max) {
        int p = pos;
        while (pos < text.length && max > 0) {
            int codePoint = Character.codePointAt(text, pos);
            if (!set.test(codePoint)) break;
            pos += Character.charCount(codePoint);
            min--;
            max--;
        }
        if (min <= 0) return new Substring(p, pos - p);
        pos = p;
        return null;
    }

    /**
     * Consume and discard a minimum number of code points within a set.
     * 
     * @param set a function recognizing acceptable code points
     * 
     * @param min the minimum number of code points to match
     * 
     * @param max the maximum number of code points to match
     * 
     * @return {@code true} if the required number of code points was
     * matched; {@code false} otherwise
     */
    public boolean consume(IntPredicate set, int min, int max) {
        int p = pos;
        while (pos < text.length && max > 0) {
            int codePoint = Character.codePointAt(text, pos);
            if (!set.test(codePoint)) break;
            pos += Character.charCount(codePoint);
            min--;
            max--;
        }
        if (min <= 0) return true;
        pos = p;
        return false;
    }

    /**
     * Parse a token consisting of code points within a set. A token
     * consists of at least one code point.
     * 
     * @param set a function recognizing acceptable code points for a
     * token
     * 
     * @return the string of matching characters; or {@code null} if
     * none matched
     */
    public CharSequence token(IntPredicate set) {
        return match(set, 1, Integer.MAX_VALUE);
    }

    /**
     * Parse linear white-space characters. If a minimum number cannot
     * be parsed, none are parsed. CRLF sequences will not match, but
     * should have been folded or recognized as field terminators by
     * now.
     * 
     * @param min minimum number of characters to parse
     * 
     * @return true if the minimum number were parsed
     */
    public boolean spaces(int min) {
        return consume(LWSP_CHARS, min, Integer.MAX_VALUE);
    }

    /**
     * Match an exact sequence of characters.
     * 
     * @param expected the expected sequence of characters
     * 
     * @return {@code true} if the sequence is immediately encountered;
     * {@code false} otherwise
     */
    public boolean punctuation(CharSequence expected) {
        final int len = expected.length();
        if (remaining() < len) return false;
        for (int i = 0; i < len; i++)
            if (expected.charAt(i) != text[pos + i]) return false;
        pos += len;
        return true;
    }

    /**
     * Match white space and an exact sequence of characters.
     * 
     * @param min minimum number of spaces to parse
     * 
     * @param expected the expected sequence of characters
     * 
     * @return {@code true} if minimum required white space and the
     * expected sequence are immediately encountered; {@code false}
     * otherwise
     */
    public boolean spacesPunctuation(int min, CharSequence expected) {
        int p = pos;
        do {
            if (!spaces(min)) break;
            if (!punctuation(expected)) break;
            return true;
        } while (false);
        pos = p;
        return false;
    }

    /**
     * Parse a quoted string.
     * 
     * @return the string after de-quoting; or {@code null} if no quoted
     * string is parsed
     */
    public String quotedString() {
        int p = pos;
        if (p == text.length || text[p] != '"') return null;
        p++;
        StringBuilder buf = new StringBuilder();
        while (p < text.length && text[p] != '"') {
            if (text[p] == '\\') {
                if (p + 1 >= text.length) return null;
                p++;
            }
            buf.append(text[p++]);
        }
        if (p >= text.length) return null;
        p++;
        pos = p;
        return buf.toString();
    }

    /**
     * Parse a quoted string preceded by white space.
     * 
     * @param min minimum number of white-space characters to parse
     * 
     * @return the string after de-quoting; or {@code null} if no string
     * or insufficient white space is encountered
     */
    public String spacesQuotedString(int min) {
        int p = pos;
        do {
            if (!spaces(min)) break;
            String t = quotedString();
            if (t != null) return t;
        } while (false);
        pos = p;
        return null;
    }

    /**
     * Parse a quoted string or a token.
     * 
     * @param set a function recognizing acceptable code points for a
     * token
     * 
     * @return the string after de-quoting; or the token converted to a
     * string; or {@code null} if no token or quoted string is parsed
     */
    public String value(IntPredicate set) {
        var r = quotedString();
        if (r != null) return r;
        var a = token(set);
        return a == null ? null : a.toString();
    }

    /**
     * Parse white space and a token.
     * 
     * @param min minimum number of white-space characters to parse
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @return the string of matching characters; or {@code null} if
     * none matched
     */
    public CharSequence spacesToken(int min, IntPredicate set) {
        final int p = pos;
        do {
            if (!spaces(min)) break;
            var t = token(set);
            if (t != null) return t;
        } while (false);
        pos = p;
        return null;
    }

    /**
     * Parse white space and then either a quoted string or a token.
     * 
     * @param min minimum number of white-space characters to parse
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @return the string after de-quoting; or the token converted to a
     * string; or {@code null} if no token or quoted string is parsed,
     * of if the minimum amount of white space is not parsed
     */
    public String spacesValue(int min, IntPredicate set) {
        final int p = pos;
        do {
            if (!spaces(min)) break;
            var t = value(set);
            if (t != null) return t;
        } while (false);
        pos = p;
        return null;
    }

    /**
     * Create a quoted string. Each backslash and double quote is
     * preceded by a backslash, then double quotes surround the result.
     * 
     * @param text the literal text of the string
     * 
     * @return the encoded text as a quoted string
     */
    public static String quote(CharSequence text) {
        StringBuilder result = new StringBuilder("\"");
        final int len = text.length();
        int last = 0;
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            switch (c) {
            case '"':
            case '\\':
                result.append(text.subSequence(last, i)).append('\\').append(c);
                last = i + 1;
                break;
            }
        }
        result.append(text.subSequence(last, len));
        return result.append('"').toString();
    }

    /**
     * Create a token or a quoted string (if necessary).
     * 
     * @param text the literal text of the token or string
     * 
     * @param acceptable the set of characters that can form a token
     * 
     * @return the encoded text as either a token or a quoted string
     */
    public static String quoteOptionally(CharSequence text,
                                         IntPredicate acceptable) {
        final int len = text.length();
        if (len > 0) for (int i = 0; i < len; i++)
            if (!acceptable.test(text.charAt(i))) return quote(text);
        return text.toString();
    }

    /**
     * Create a token or a quoted string (if necessary).
     * 
     * @param text the literal text of the token or string
     * 
     * @param acceptable the set of characters that can form a token
     * 
     * @return the encoded text as either a token or a quoted string
     */
    public static String quoteOptionally(CharSequence text) {
        return quoteOptionally(text, RFC2045_TOKEN_CHARS);
    }

    /**
     * Get the index of the first code point in a sequence that <em>is
     * not</em> in a given set.
     * 
     * @param text the sequence to search
     * 
     * @param set the set of acceptable code points
     * 
     * @return the index of the first mismatch; or the character length
     * of the sequence
     */
    public static int prefixLength(CharSequence text, IntPredicate set) {
        return text.codePoints().takeWhile(set).map(Character::charCount).sum();
    }

    /**
     * Test whether all code points in a sequence form a token. An empty
     * sequence is not a token.
     * 
     * @param text the sequence to test
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @return {@code true} if all code points are in the set;
     * {@code false} otherwise
     */
    public static boolean isToken(CharSequence text, IntPredicate set) {
        return !text.isEmpty() && prefixLength(text, set) == text.length();
    }

    /**
     * Parse the end of the string. Note that, on success, this sets the
     * position to one past the end of the text. Subsequent calls will
     * fail, and a subsequent call to {@link #remaining()} will yield a
     * negative value.
     * 
     * @return {@code true} if the whole input has been parsed;
     * {@code false} otherwise
     */
    public boolean end() {
        if (pos == text.length) {
            pos++;
            return true;
        }
        return false;
    }

    /**
     * Parse white space and the end of the string. Note that, on
     * success, this sets the position to one past the end of the text.
     * Subsequent calls will fail, and a subsequent call to
     * {@link #remaining()} will yield <code>-1</code>.
     * 
     * @param min minimum number of white-space characters to parse
     * 
     * @return {@code true} if the whole input has been parsed;
     * {@code false} otherwise
     */
    public boolean spacesEnd(int min) {
        int p = pos;
        do {
            if (!spaces(min)) break;
            if (end()) return true;
        } while (false);
        pos = p;
        return false;
    }

    /**
     * Parse a parameter, yielding a map entry. The parameter has the
     * form <samp>; <var>name</var> = <var>value</var></samp>, where
     * <var>name</var> is a token, and <var>value</var> is a quoted
     * string or token. White space is permitted before the name, the
     * value and the punctuation.
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @return the parsed parameter as a key-value pair; or {@code null}
     * no parameter was parsed
     */
    public Map.Entry<CharSequence, String> parameter(IntPredicate set) {
        final int p = pos;
        CharSequence name;
        String value;
        if (spacesPunctuation(0, ";") && (name = spacesToken(0, set)) != null &&
            spacesPunctuation(0, "=") && (value = spacesValue(0, set)) != null)
            return Map.entry(name, value);
        pos = p;
        return null;
    }

    /**
     * Parse a parameter, submitting it to a consumer. The parameter has
     * the form <samp>; <var>name</var> = <var>value</var></samp>, where
     * <var>name</var> is an atom, and <var>value</var> is a word. White
     * space is permitted before the name, the value and the
     * punctuation.
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @param dest the destination for the parameter
     * 
     * @return {@code true} if a parameter was parsed; {@code false}
     * otherwise
     */
    public boolean
        parameter(IntPredicate set,
                  BiConsumer<? super CharSequence, ? super String> dest) {
        final int p = pos;
        CharSequence name;
        String value;
        if (spacesPunctuation(0, ";") && (name = spacesToken(0, set)) != null &&
            spacesPunctuation(0, "=") &&
            (value = spacesValue(0, set)) != null) {
            dest.accept(name, value);
            return true;
        }
        pos = p;
        return false;
    }

    /**
     * Create a consumer of key-value pairs that stores them in map,
     * converting the key to a string.
     * 
     * @param dest where to store the key-value pairs
     * 
     * @param <T> the type of the consumer's first parameter
     * 
     * @return the requested consumer
     */
    private static <T> BiConsumer<T, String>
        intoMap(Map<? super String, ? super String> dest) {
        return (k, v) -> dest.put(k.toString(), v);
    }

    /**
     * Parse as many parameters as possible to a consumer.
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @param dest where to store parameters
     * 
     * @return the number of parameters parsed, which could be zero
     */
    public int
        parameters(IntPredicate set,
                   BiConsumer<? super CharSequence, ? super String> dest) {
        int r = 0;
        while (parameter(set, dest))
            r++;
        return r;
    }

    /**
     * Parse as many parameters as possible into a map. Parameters are
     * added to a map, which is not cleared first.
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @param dest where to store parameters
     * 
     * @return the number of parameters parsed, which could be zero
     */
    public int parameters(IntPredicate set,
                          Map<? super String, ? super String> dest) {
        return parameters(set, intoMap(dest));
    }

    /**
     * Parse a quoted string, and as many parameters as possible into a
     * consumer.
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @param dest where to store parameters
     * 
     * @return the string; or {@code null} if not parsed
     */
    public String quotedStringParameters(IntPredicate set,
                                         BiConsumer<? super CharSequence,
                                                    ? super String> dest) {
        var t = quotedString();
        if (t == null) return null;
        parameters(set, dest);
        return t;
    }

    /**
     * Parse a quoted string, and as many parameters as possible into a
     * map.
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @param dest where to store parameters
     * 
     * @return the string; or {@code null} if not parsed
     */
    public String
        quotedStringParameters(IntPredicate set,
                               Map<? super String, ? super String> dest) {
        return quotedStringParameters(set, intoMap(dest));
    }

    /**
     * Parse a quoted string, and as many parameters as possible into a
     * consumer.
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @param dest where to store parameters
     * 
     * @return the string; or {@code null} if not parsed
     */
    public String
        spacesQuotedStringParameters(int min, IntPredicate set,
                                     BiConsumer<? super CharSequence,
                                                ? super String> dest) {
        var t = spacesQuotedString(min);
        if (t == null) return null;
        parameters(set, dest);
        return t;
    }

    /**
     * Parse a quoted string, and as many parameters as possible into a
     * map.
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @param dest where to store parameters
     * 
     * @return the string; or {@code null} if not parsed
     */
    public String
        spacesQuotedStringParameters(int min, IntPredicate set,
                                     Map<? super String, ? super String> dest) {
        return spacesQuotedStringParameters(min, set, intoMap(dest));
    }

    /**
     * Parse a token, and as many parameters as possible into a
     * consumer.
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @param dest where to store parameters
     * 
     * @return the string; or {@code null} if not parsed
     */
    public CharSequence
        tokenParameters(IntPredicate set,
                        BiConsumer<? super CharSequence, ? super String> dest) {
        var t = token(set);
        if (t == null) return null;
        parameters(set, dest);
        return t;
    }

    /**
     * Parse a quoted string, and as many parameters as possible into a
     * map.
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @param dest where to store parameters
     * 
     * @return the string; or {@code null} if not parsed
     */
    public CharSequence
        tokenParameters(IntPredicate set,
                        Map<? super String, ? super String> dest) {
        return tokenParameters(set, intoMap(dest));
    }

    /**
     * Parse a quoted string, and as many parameters as possible into a
     * consumer.
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @param dest where to store parameters
     * 
     * @return the string; or {@code null} if not parsed
     */
    public CharSequence spacesTokenParameters(int min, IntPredicate set,
                                              BiConsumer<? super CharSequence,
                                                         ? super String> dest) {
        var t = spacesToken(min, set);
        if (t == null) return null;
        parameters(set, dest);
        return t;
    }

    /**
     * Parse a quoted string, and as many parameters as possible into a
     * map.
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @param dest where to store parameters
     * 
     * @return the string; or {@code null} if not parsed
     */
    public CharSequence
        spacesTokenParameters(int min, IntPredicate set,
                              Map<? super String, ? super String> dest) {
        return spacesTokenParameters(min, set, intoMap(dest));
    }

    /**
     * Extract a single element.
     * 
     * @param <T> the represented type of the element
     * 
     * @param extractor the parser for a single element
     * 
     * @return a representation of the element; or {@code null} if not
     * parsed, with the cursor reset to its initial position
     */
    public <T> T extract(Function<? super Tokenizer, T> extractor) {
        final int p = pos;
        T val = extractor.apply(this);
        if (val != null) pos = p;
        return val;
    }

    /**
     * Extract a comma-separated sequence, and then the end of input.
     * 
     * @param <T> the represented type of the elements
     * 
     * @param extractor the parser for a single element
     * 
     * @return a list of the represented elements; or {@code null} if
     * the remainder of the input is not a comma-separated sequence of
     * elements recognized by the extractor
     */
    public <T> List<T>
        extractCommaSequence(Function<? super Tokenizer, T> extractor) {
        List<T> r = new ArrayList<>();
        final int p = pos;
        each_entry: while (true) {
            T v = extract(extractor);
            if (v == null) break;
            r.add(v);

            switch (separator(0, ",")) {
            case END:
                return r;

            case UNEXPECTED:
                break each_entry;
            }
        }
        pos = p;
        return null;
    }

    /**
     * Expect white space, and then a separator or the end of input.
     * 
     * @param min the minimum amount of white space, matched with
     * {@link #spaces(int)}
     * 
     * @param expected the expected separator, matched with
     * {@link #punctuation(CharSequence)}
     * 
     * @return the separation between the prior element and the next
     */
    public Separation separator(int min, CharSequence expected) {
        final int p = pos;
        do {
            if (!spaces(min)) break;
            if (punctuation(expected)) return Separation.PUNCTUATION;
            if (end()) return Separation.END;
        } while (false);
        pos = p;
        return Separation.UNEXPECTED;
    }

    /**
     * Indicates how a prior element is separated from the next.
     */
    public static enum Separation {
        /**
         * Indicates that the expected punctuation was found.
         */
        PUNCTUATION,

        /**
         * Indicates that the end of input was found.
         */
        END,

        /**
         * Indicates that something unexpected was found, neither the
         * separator nor the end of input.
         */
        UNEXPECTED;
    }

    /**
     * Extract a comma-separated sequence, aborting with an exception on
     * failure.
     * 
     * @param <T> the represented type of the elements
     * 
     * @param extractor the parser for a single element
     * 
     * @param label a label to use in abort messages
     * 
     * @return a list of the represented elements
     * 
     * @throws TokenException if the remainder of the input is not a
     * comma-separated sequence of elements recognized by the extractor
     */
    public <T> List<T>
        extractCommaSequence(String label,
                             Function<? super Tokenizer, T> extractor) {
        List<T> r = new ArrayList<>();
        final int p = pos;
        while (true) {
            T v = extract(extractor);
            if (v == null) {
                pos = p;
                abort(label + " (element)");
            }
            r.add(v);

            switch (separator(0, ",")) {
            case END:
                return r;

            case UNEXPECTED:
                pos = p;
                abort(label + " (end)");
            }
        }
    }

    /**
     * Parse a character sequence as comma-separated tokens. Each
     * extracted token is converted to a string.
     * 
     * @param text the source characters
     * 
     * @param set the set of acceptable code points for a token
     * 
     * @return a list of atoms; an empty list if the input is
     * {@code null}
     */
    public static List<String> tokenSequenceOf(CharSequence text,
                                               IntPredicate set) {
        if (text == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        Tokenizer tokens = new Tokenizer(text);
        CharSequence atom;
        while ((atom = tokens.spacesToken(0, set)) != null) {
            result.add(atom.toString());
            tokens.spaces(0);
            if (tokens.end()) break;
            if (tokens.punctuation(",")) continue;
            throw new TokenException("not token list: " + text);
        }
        return List.copyOf(result);
    }

    /**
     * Parse text until a potential encoded word is found. The caller
     * should then attempt to parse an encoded word and, upon failure,
     * consume the next two characters as literal text.
     * 
     * @return the non-empty text; or {@code null} if no text could be
     * found
     * 
     * @todo This won't do. Encoded words must be separated from each
     * other and anything else by white space. But should that white
     * space be absorbed? Only between encoded words?
     */
    public CharSequence text() {
        /* Seek a =? sequence. */
        int p = pos;
        while (p < text.length) {
            if (p > pos && text[p - 1] == '=' && text[p] == '?') {
                /* Found it, so go back one, and stop. */
                p--;
                break;
            }
            p++;
        }
        /* Fail rather than return an empty sequence. */
        if (p == pos) return null;

        /* Form the result and advance the position. */
        CharSequence r = new Substring(pos, p - pos);
        pos = p;
        return r;
    }

    /**
     * Parse an encoded word. This has the form
     * <samp>=?<var>charset</var>?<var>encoding</var>?<var>data</var>?=</samp>.
     * 
     * <p>
     * If <var>encoding</var> is <samp>Q</samp>, then <var>data</var> is
     * in Quoted-Printable form, and consists of US-ASCII characters and
     * escape sequences of the form <samp>=<var>XX</var></samp>. Each
     * escape sequence is converted to a single byte with the
     * hexadecimal value <var>XX</var>. Each underscore <samp>_</samp>
     * is converted to a byte with the value 32 (US-ASCII space). Other
     * characters are converted to their US-ASCII equivalents.
     * 
     * <p>
     * If <var>encoding</var> is <samp>B</samp>, then <var>data</var>
     * consists of US-ASCII characters that represent Base64 sextets.
     * The characters <samp>A</samp> through <samp>Z</samp> are
     * converted to 0 through 25, <samp>a</samp> through <samp>z</samp>
     * to 26 through 51, <samp>0</samp> through <samp>9</samp> to 52
     * through 61, <samp>+</samp> to 62 and <samp>/</samp> to 63.
     * <samp>=</samp> is ignored as padding. Sextets are concatenated,
     * and repartitioned as bytes.
     * 
     * <p>
     * Both byte sequences
     * 
     * @return the decoded word; or {@code null} if there is no match
     * 
     * @todo Is Quoted-Printable handling correct? Ordinarily, bytes
     * that correspond to safe US-ASCII characters are represented as
     * those characters, but do not necessarily decode into those
     * characters. For example, an EBCDIC <samp>?</samp> has the value
     * 111, so it may appear as <samp>o</samp>, whose US-ASCII code is
     * also 111. This is appropriate for
     * <samp>Content-Transfer-Encoding</samp>, as that is about
     * conversion from one byte sequence to another (one of which is
     * 7-bit-safe), and doesn't even have to involve a character
     * encoding. In contrast, an <samp>encoded-word</samp> in RFC2047 is
     * meant to express a character sequence, not a byte sequence, so it
     * could be reasonable to treat the unescaped characters as plain
     * characters, not character representations of bytes to be later
     * converted back to (potentially other) characters.
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc2047.html">RFC2047:
     * Message Header Extensions</a>
     */
    public String encodedWord() {
        final int p = pos;
        CharSequence csName, encName, data;
        if (punctuation("=?") &&
            (csName = token(RFC2045_TOKEN_CHARS)) != null && punctuation("?") &&
            (encName = token(RFC2045_TOKEN_CHARS)) != null &&
            punctuation("?") && (data = token(ENCODED_CHARS)) != null &&
            punctuation("?=")) {
            Charset cs = Charset.forName(csName.toString());
            if (encName.equals("Q")) return QuotedPrintable.decode(data, cs);
            if (encName.equals("B")) return Base64.decode(data, cs);
        }
        pos = p;
        return null;
    }

    /**
     * Identifies characters usable in an encoded word.
     * 
     * @see Tokenizer#encodedWord()
     */
    public static IntPredicate ENCODED_CHARS =
        c -> c >= '!' && c <= '~' && c != '?';

    /**
     * Parse a sequence of encoded words. White space between encoded
     * words is discarded.
     * 
     * @return the concatenation of decoded adjacent words; or
     * {@code null} if no encoded words were found
     */
    public String encodedWords() {
        String first = encodedWord();
        if (first == null) return null;
        StringBuilder buf = new StringBuilder(first);
        for (;;) {
            int p = pos;
            spaces(0);
            String next = encodedWord();
            if (next == null) {
                pos = p;
                break;
            }
            buf.append(next);
        }
        return buf.toString();
    }

    /**
     * @hidden
     */
    public static void main(String[] args) {
        System.out.println(Tokenizer.quoteOptionally("foobar"));
        System.out.println(Tokenizer.quoteOptionally("\"foobar"));
        System.out.println(Tokenizer.quoteOptionally("foobar\""));
        System.out.println(Tokenizer.quoteOptionally("foo bar"));
        System.out.println(Tokenizer.quoteOptionally("foo\\bar"));
        System.out.println(Tokenizer.quoteOptionally("\"foo\"bar"));
        System.out.println(Tokenizer.quoteOptionally("foo\"bar\""));
        System.out.println(Tokenizer.quote("foobar"));
        System.out.println(Tokenizer.quote("\"foobar"));
        System.out.println(Tokenizer.quote("foobar\""));
        System.out.println(Tokenizer.quote("\"foo\"bar"));
        System.out.println(Tokenizer.quote("foo\"bar\""));
    }
}
