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

/**
 * Identifies characters belonging to a set.
 *
 * @author simpsons
 */
public interface CharacterSet {
    /**
     * Test whether a character is in the set.
     *
     * @param c the character to test
     *
     * @return {@code true} if the character is in the set;
     * {@code false} otherwise
     */
    boolean contains(char c);

    /**
     * Identifies characters suitable for header field names. These are
     * printable US-ASCII characters, excluding space U+0020 and colon
     * U+003A.
     */
    static CharacterSet FIELD_NAME_CHARS =
        c -> c >= '!' && c <= '~' && c != ':';

    /**
     * Identifies separator characters in header field values. These
     * include a range of US-ASCII punctuation,
     * <samp>()[]&#123;&#125;:;,&lt;&gt;\/?&#64;=</samp>, as well as
     * spaces U+0020 and tabs U+0009.
     */
    static CharacterSet SEPARATORS = c -> switch (c) {
    case '(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?',
        '=', '{', '}', ' ', '\t' -> true;
    default -> false;
    };

    /**
     * Identifies RFC822 ‘specials’. These include a range of US-ASCII
     * punctuation, <samp>()[]:;,&lt;&gt;\&#64;</samp>.
     */
    static CharacterSet SPECIALS = c -> switch (c) {
    case '(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '.', '[', ']' ->
        true;
    default -> false;
    };

    /**
     * Identifies characters that can form an atom. These are visible
     * US-ASCII characters excluding the {@linkplain #SPECIALS
     * specials}.
     */
    static CharacterSet ATOM_CHARS =
        c -> c >= '!' && c <= '~' && !SPECIALS.contains(c);

    /**
     * Identifies US-ASCII alphabetic characters. These are U+0041
     * through U+005A, and U+0061 through U+007A.
     */
    static CharacterSet ALPHAS =
        c -> (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');

    /**
     * Identifies characters permitted in tokens. This includes all
     * printable US-ASCII characters except space U+0020 and
     * {@linkplain #SEPARATORS separators}.
     */
    public static final CharacterSet TOKEN_CHARS =
        c -> !SEPARATORS.contains(c) && c >= '!' && c <= '~';

    /**
     * Identifies US-ASCII decimal digits. These are U+0030 through
     * U+0039.
     */
    public static final CharacterSet DIGITS = c -> c >= '0' && c <= '9';

    /**
     * Identifies US-ASCII hexadecimal digits. These are U+0030 through
     * U+0039, U+0041 through U+0046, and U+0061 through U+0066.
     */
    public static final CharacterSet HEX_DIGITS = c -> DIGITS.contains(c) ||
        (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');

    /**
     * Identifies linear white-space characters. These are space U+0020
     * and tab U+0009.
     */
    static CharacterSet LWSP_CHARS = c -> c == ' ' || c == '\t';
}
