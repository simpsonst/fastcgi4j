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
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Parses MIME header atoms, quoted strings and related structures. The
 * entire input is required at once. A copy of it is retained
 * internally, and a position is initialized at the start. Each time a
 * token is successfully parsed, the position advances.
 * 
 * @author simpsons
 */
public final class Tokenizer {
    private final char[] text;

    private int pos = 0;

    /**
     * Marks a position in a tokenizer. As an auto-closeable, you can
     * use it as follows, calling {@link #pass()} only if a compound
     * structure completely parses:
     * 
     * <pre>
     * Tokenizer tok = ...;
     * try (var m = tok.mark()) {
     *   tok.whitespace(0);
     *   String major, minor;
     *   if ((major = tok.atom()) != null &&
     *       tok.symbol('/') &&
     *       (minor = tok.atom()) {
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
     * Get the number of characters remaining. This can be negative,
     * indicating that the end has been parsed using {@link #end()}.
     * 
     * @return the number of remaining characters
     */
    public int remaining() {
        return text.length - pos;
    }

    /**
     * Prepare a character sequence to be tokenized.
     */
    public Tokenizer(CharSequence text) {
        this.text = new char[text.length()];
        for (int i = 0; i < this.text.length; i++)
            this.text[i] = text.charAt(i);
    }

    /**
     * Parse characters within a set.
     * 
     * @param set a function recognizing acceptable characters
     * 
     * @return the string of matching characters; or {@code null} if
     * none matched
     */
    public CharSequence set(CharacterSet set) {
        int p = pos;
        while (pos < text.length && set.contains(text[pos]))
            pos++;
        if (pos == p) return null;
        return new Substring(p, pos - p);
    }

    /**
     * Parse an atom.
     * 
     * @return the atom; or {@code null} if no atom could be parsed
     */
    public CharSequence atom() {
        return set(CharacterSet.ATOM_CHARS);
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
     * Parse text until a potential encoded word is found. The caller
     * should then attempt to parse an encoded word and, upon failure,
     * consume the next two characters as literal text.
     * 
     * @return the non-empty text; or {@code null} if no text could be
     * found
     * 
     * @todo This won't do. Encoded words must be separated from each
     * other and anything else by whitespace. But should that whitespace
     * be absorbed? Only between encoded words?
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
     * Parsed an encoded word. This has the form
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
        if (sequence("=?") && (csName = atom()) != null && character('?') &&
            (encName = atom()) != null && character('?') &&
            (data = set(CharacterSet.ENCODED_CHARS)) != null &&
            sequence("?=")) {
            Charset cs = Charset.forName(csName.toString());
            if (encName.equals("Q")) return QuotedPrintable.decode(data, cs);
            if (encName.equals("B")) return Base64.decode(data, cs);
        }
        pos = p;
        return null;
    }

    /**
     * Parse a sequence of encoded words. Whitespace between words is
     * discarded.
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
            whitespace(0);
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
     * Parse a word (an atom or a quoted string).
     * 
     * @return the atom; or the string after de-quoting; or {@code null}
     * if no atom or quoted string is parsed
     */
    public String word() {
        CharSequence r = atom();
        if (r == null) return quotedString();
        return r.toString();
    }

    /**
     * Parse the end of the string. Note that, on success, this sets the
     * position to one past the end of the text. Subsequent calls will
     * fail, and a subsequent call to {@link #remaining()} will yield
     * <code>-1</code>.
     * 
     * @return {@code true} if the whole string has been parsed;
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
     * Parse linear whitespace characters. If a minimum number cannot be
     * parsed, none are parsed. CRLF sequences will not match, but
     * should have been folded or recognized as field terminators by
     * now.
     * 
     * @param min minimum number of characters to parse
     * 
     * @return true if the minimum number were parsed
     */
    public boolean whitespace(int min) {
        int p = pos;
        while (p < text.length) {
            if (CharacterSet.LWSP_CHARS.contains(text[p]))
                min--;
            else
                break;
            p++;
        }
        if (min > 0) return false;
        pos = p;
        return true;
    }

    /**
     * Parse characters in a set preceded by whitespace.
     * 
     * @param min minimum number of whitespace characters to parse.
     * 
     * @param set the set of acceptable characters
     * 
     * @return the string of matching characters; or {@code null} if
     * none matched
     */
    public CharSequence whitespaceSet(int min, CharacterSet set) {
        final int p = pos;
        do {
            if (!whitespace(min)) break;
            CharSequence t = set(set);
            if (t != null) return t;
        } while (false);
        pos = p;
        return null;
    }

    /**
     * Parse an atom preceded by whitespace.
     * 
     * @param min minimum number of whitespace characters to parse
     * 
     * @return the atom; or {@code null} if no atom or insufficient
     * whitespace was encountered
     */
    public CharSequence whitespaceAtom(int min) {
        return whitespaceSet(min, CharacterSet.ATOM_CHARS);
    }

    /**
     * Parse a quoted string preceded by whitespace.
     * 
     * @param min minimum number of whitespace characters to parse
     * 
     * @return the string after de-quoting; or {@code null} if no string
     * or insufficient whitespace is encountered
     */
    public String whitespaceQuotedString(int min) {
        int p = pos;
        do {
            if (!whitespace(min)) break;
            String t = quotedString();
            if (t != null) return t;
        } while (false);
        pos = p;
        return null;
    }

    /**
     * Parse a word (an atom or a quoted string) preceded by whitespace.
     * 
     * @param min minimum number of whitespace characters to parse
     * 
     * @return the atom; or the string after de-quoting; or {@code null}
     * if no atom or quoted string or insufficient whitespace is
     * encountered
     */
    public String whitespaceWord(int min) {
        int p = pos;
        do {
            if (!whitespace(min)) break;
            String t = word();
            if (t != null) return t;
        } while (false);
        pos = p;
        return null;
    }

    /**
     * Parse a character preceded by whitespace.
     * 
     * @param min minimum number of whitespace characters to parse
     * 
     * @param c the character to parse
     * 
     * @return true if the character followed the whitespace
     */
    public boolean whitespaceCharacter(int min, char c) {
        int p = pos;
        do {
            if (!whitespace(min)) break;
            if (character(c)) return true;
        } while (false);
        pos = p;
        return false;
    }

    /**
     * Parse a sequence of characters.
     * 
     * @param seq the character sequence
     * 
     * @return {@code true} if the next characters match the sequence
     * exactly; {@code false} otherwise
     */
    public boolean sequence(CharSequence seq) {
        final int len = seq.length();
        if (pos + len > text.length) return false;
        for (int i = 0; i < len; i++)
            if (text[pos + i] != seq.charAt(i)) return false;
        pos += len;
        return true;
    }

    /**
     * Parse a specific character.
     * 
     * @param c the character to parse, normally a separator
     */
    public boolean character(char c) {
        if (pos >= text.length || text[pos] != c) return false;
        pos++;
        return true;
    }

    /**
     * Parse a character from a set of candidates.
     * 
     * @param set the acceptable characters
     * 
     * @return the index into the set of the parsed character, or -1 of
     * none match
     */
    public int character(String set) {
        if (pos >= text.length) return -1;
        int i;
        if ((i = set.indexOf(text[pos])) < 0) return -1;
        pos++;
        return i;
    }

    /**
     * Parse any character.
     * 
     * @return the next character
     * 
     * @throws IllegalStateException if there are no more characters
     */
    public char character() {
        if (pos >= text.length) throw new IllegalStateException("end reached");
        return text[pos++];
    }

    /**
     * Skip a fixed number of characters.
     * 
     * @param count the number of characters to be skipped
     * 
     * @return {@code true} if the required amount could be skipped;
     * {@code false} otherwise
     */
    public boolean skip(int count) {
        if (pos + count > text.length) return false;
        pos += count;
        return true;
    }

    /**
     * Parse a parameter. The parameter has the form <samp>;
     * <var>name</var> = <var>value</var></samp>, where <var>name</var>
     * is an atom, and <var>value</var> is a word.
     * 
     * @return the parsed parameter as a key-value pair; or {@code null}
     * no parameter was parsed
     */
    public Map.Entry<String, String> parameter() {
        final int p = pos;
        CharSequence name;
        String value;
        if (whitespaceCharacter(0, ';') && (name = atom()) != null &&
            whitespaceCharacter(0, '=') && (value = word()) != null)
            return Map.entry(name.toString(), value);
        pos = p;
        return null;
    }

    /**
     * Parse a parameter, submitting it to a consumer. The parameter has
     * the form <samp>; <var>name</var> = <var>value</var></samp>, where
     * <var>name</var> is an atom, and <var>value</var> is a word.
     * 
     * @param dest the destination for the parameter
     * 
     * @return {@code true} if a parameter was parsed; {@code false}
     * otherwise
     */
    public boolean parameter(BiConsumer<? super String, ? super String> dest) {
        final int p = pos;
        CharSequence name;
        String value;
        if (whitespaceCharacter(0, ';') && (name = whitespaceAtom(0)) != null &&
            whitespaceCharacter(0, '=') &&
            (value = whitespaceWord(0)) != null) {
            dest.accept(name.toString(), value);
            return true;
        }
        pos = p;
        return false;
    }

    /**
     * Parse a parameter, placing the result in a map. The parameter has
     * the form <samp>; <var>name</var> = <var>value</var></samp>, where
     * <var>name</var> is an atom, and <var>value</var> is a word.
     * 
     * @param params the destination for the parameter
     * 
     * @return {@code true} if a parameter was parsed; {@code false}
     * otherwise
     */
    public boolean parameter(Map<? super String, ? super String> params) {
        return parameter(params::put);
    }

    /**
     * Parse as many parameters as possible, plus the end of input.
     * Parameters are added to a map, which is cleared first. No
     * parameters are written to the map, nor is it cleared, unless
     * parsing is successful.
     * 
     * @param params where to store parameters
     * 
     * @return {@code true} if at least one parameter was parsed
     */
    public boolean parameters(Map<? super String, ? super String> params) {
        final int p = pos;
        do {
            Map<String, String> tmp = new HashMap<>();
            while (parameter(tmp))
                ;
            whitespace(0);
            if (!end()) break;

            params.clear();
            params.putAll(tmp);
            return true;
        } while (false);
        pos = p;
        return false;
    }

    /**
     * Parse an atom, and as many parameters as possible.
     * 
     * @param params where to store parameters
     * 
     * @return the atom; or {@code null} if not parsed
     */
    public CharSequence
        atomParameters(Map<? super String, ? super String> params) {
        final int p = pos;
        do {
            CharSequence t = atom();
            if (t == null) return null;
            if (parameters(params)) return t;
        } while (false);
        pos = p;
        return null;
    }

    /**
     * Parse whitespace, an atom and as many parameters as possible.
     * 
     * @param min minimum number of whitespace characters to parse
     * 
     * @param params where to store parameters
     * 
     * @return the atom; or {@code null} if not parsed
     */
    public CharSequence
        whitespaceAtomParameters(int min,
                                 Map<? super String, ? super String> params) {
        final int p = pos;
        do {
            if (!whitespace(min)) break;
            CharSequence t = atomParameters(params);
            if (t == null) break;
            return t;
        } while (false);
        pos = p;
        return null;
    }

    /**
     * Parse a media type, including decoded parameters.
     * 
     * @return the media type; or {@code null} if no media type followed
     * by zero or more parameters are found
     */
    public MediaType mediaType() {
        final int p = pos;
        CharSequence major, minor;
        Map<String, String> rawParams = new HashMap<>();
        if ((major = whitespaceAtom(0)) != null && character('/') &&
            (minor = atom()) != null && parameters(rawParams)) {
            Map<String, ParameterValue> decoded =
                ParameterValue.decodeParameters(rawParams);
            return new MediaType(major.toString(), minor.toString(), decoded);
        }
        pos = p;
        return null;
    }

    /**
     * Create a token or a quoted string (if necessary).
     * 
     * @param text the literal text of the token or string
     * 
     * @return the encoded text as either a token or a quoted string
     */
    public static String quoteOptionally(CharSequence text) {
        final int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (!CharacterSet.TOKEN_CHARS.contains(c)) {
                StringBuilder result = new StringBuilder("\"");
                result.append(text.subSequence(0, i)).append('\\').append(c);
                int last = ++i;
                while (i < len) {
                    c = text.charAt(i);
                    if (!CharacterSet.TOKEN_CHARS.contains(c)) {
                        result.append(text.subSequence(last, i)).append('\\')
                            .append(c);
                        last = i;
                    }
                }
                result.append(text.subSequence(last, i)).append('\\').append(c);
                return result.append('"').toString();
            }
        }
        return text.toString();
    }

    /**
     * Check whether a string contains only characters compatible with a
     * token.
     * 
     * @param t the candidate token
     * 
     * @return {@code true} if the candidate is a token; {@code false}
     * otherwise
     */
    public static boolean isAtom(CharSequence t) {
        /* TODO: Are there different rules for the start of a token? */
        final int len = t.length();
        for (int i = 0; i < len; i++) {
            char c = t.charAt(i);
            if (!CharacterSet.TOKEN_CHARS.contains(c)) return false;
        }
        return true;
    }
}
