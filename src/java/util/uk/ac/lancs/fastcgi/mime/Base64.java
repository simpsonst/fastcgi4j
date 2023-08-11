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

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Provides constants and basic functions for processing Base64 data.
 *
 * @author simpsons
 */
final class Base64 {
    private Base64() {}

    /**
     * Indicates a byte or character classed as Base64 padding.
     */
    public static final int PADDING = -1;

    /**
     * Indicates a byte that represents a US-ASCII character, or a
     * character that is present in US-ASCII, but which cannot be
     * interpreted as a significant Base64 character, padding or
     * whitespace.
     */
    public static final int ILLEGAL_ASCII = -3;

    /**
     * Indicates a byte or character classed as whitespace.
     */
    public static final int WHITESPACE = -2;

    /**
     * Indicates a byte that doesn't represent a US-ASCII character.
     */
    public static final int NON_ASCII = -4;

    /**
     * Decode a byte as a single Base64 character.
     *
     * @param b the byte to decode
     *
     * @return the Base64 value of the byte in the range [0, 63]; or
     * {@link #PADDING} if the byte is padding; or {@link #WHITESPACE}
     * if the byte is whitespace; or {@link #ILLEGAL_ASCII} if the byte
     * is ASCII but is not a Base64 character; or {@link #NON_ASCII} if
     * the byte is not US-ASCII
     */
    public static int decode(byte b) {
        if (b >= 65 && b < 65 + 26) return b - 65;
        if (b >= 97 && b < 97 + 26) return b - (97 - 26);
        if (b >= 48 && b < 48 + 10) return b - (48 - 52);
        if (b < 0) return NON_ASCII;
        return switch (b) {
        case 32, 10, 13, 8 -> WHITESPACE;
        case 43 -> 62;
        case 47 -> 63;
        case 61 -> PADDING;
        default -> ILLEGAL_ASCII;
        };
    }

    /**
     * Decode a single Base64 character.
     *
     * @param c the character to decode
     *
     * @return the Base64 value of the character in the range [0, 63];
     * or {@link #PADDING} if the character is padding; or
     * {@link #WHITESPACE} if the character is whitespace; or
     * {@link #ILLEGAL_ASCII} if the character is ASCII but not a Base64
     * character; or {@link #NON_ASCII} if the character is not US-ASCII
     */
    public static int decode(char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return c - 'a' + 26;
        if (c >= '0' && c <= '9') return c - '0' + 52;
        return switch (c) {
        case '+' -> 62;
        case '/' -> 63;
        case '=' -> PADDING;
        case '\t', '\r', '\n', ' ' -> WHITESPACE;
        default -> ILLEGAL_ASCII;
        };
    }

    /**
     * Decode a Base64-encoded stream, closing it after use.
     * 
     * @param in the encoded stream
     * 
     * @return a decoded stream which closes the encoded stream when it
     * is closed
     */
    public static InputStream decodeCloseAfter(InputStream in) {
        return new Base64DecodingInputStream(in, true);
    }

    /**
     * Decode a Base64-encoded stream, leaving it open after use.
     * 
     * @param in the encoded stream
     * 
     * @return a decoded stream which does not close the encoded stream
     * when it is closed
     */
    public static InputStream decodeLeaveOpen(InputStream in) {
        return new Base64DecodingInputStream(in, false);
    }

    /**
     * Decode a contiguous Base64 sequence, as found in an RFC2047
     * encoded word. All characters are first converted into bytes, then
     * decoded according to a specified character encoding.
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc2047.html">RFC2047:
     * Message Header Extensions</a>
     *
     * @param text the Base64 characters, excluding padding
     * 
     * @param charset the encoding to convert the generated bytes back
     * into characters
     *
     * @return the decoded characters
     *
     * @throws IllegalArgumentException if non-Base64 characters are
     * encountered
     */
    public static String decode(CharSequence text, Charset charset) {
        final int len = text.length();
        final int rlen = len * 3 / 4;
        final byte[] r = new byte[rlen];
        int value = 0;
        int width = 0;
        int p = 0;
        for (int i = 0; i < len; i++) {
            int sextet = decode(text.charAt(i));
            switch (sextet) {
            case PADDING -> {
                continue;
            }
            case WHITESPACE, ILLEGAL_ASCII, NON_ASCII ->
                throw new IllegalArgumentException("bad base64: "
                    + text.charAt(i));
            }
            value <<= 6;
            value |= sextet;
            width += 6;
            while (width >= 8) {
                width -= 8;
                r[p++] = (byte) (value >>> width);
            }
        }
        return new String(r, 0, p, charset);
    }
}
