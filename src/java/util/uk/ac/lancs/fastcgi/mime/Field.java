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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author simpsons
 */
public final class Field {
    private static final Pattern QP_CHAR = Pattern.compile("_+|=([0-9A-F]{2})");

    private static final String QP_CHARS =
        "[][a-zA-Z0-9_-!\"$%^&*()+{}:;@'~#,.<>/?=]";

    private static final String BASE64_CHARS = "[A-Za-z0-9+/]";

    private static final String CHARSET_PATTERN = "[a-zA-Z0-9][-a-zA-Z0-9_]*";

    private static final String BASE64_TAIL1 = BASE64_CHARS + "{3}=";

    private static final String BASE64_TAIL2 = BASE64_CHARS + "{2}==";

    private static final String BASE64_SEQ = BASE64_CHARS + "*=*";

    private static final String BASE64_BLOCK = BASE64_CHARS + "{4}";

    private static final String BASE64_SEQ_STRICT = "(?:" + BASE64_BLOCK
        + ")*(?:" + BASE64_TAIL1 + "|" + BASE64_TAIL2 + ")?";

    private static final int BASE64_PADDING = -1;

    private static final int BASE64_ILLEGAL_ASCII = -3;

    private static final Pattern ESCAPE_SEQUENCE =
        Pattern.compile("=\\?(?<charset>" + CHARSET_PATTERN + ")" + "\\?(?:"
            + "(?:[bB]\\?(?<base64>" + BASE64_CHARS + "*)=*)|"
            + "(?:[qQ]\\?(?<qp>(?:" + QP_CHARS + "|=[0-9A-F]{2})*?))"
            + ")\\?=");

    private static final int BASE64_WHITESPACE = -2;

    private static final int BASE64_NON_ASCII = -4;

    /**
     * Decode a contiguous Quoted-Printable sequence.
     *
     * @param text the Quoted-Printable characters
     *
     * @return a byte array of the decoded sequence
     */
    private static byte[] decodeQuotedPrintable(CharSequence text) {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            int last = 0;
            Matcher m = QP_CHAR.matcher(text);
            while (m.find(last)) {
                result.write(text.subSequence(last, m.start()).toString()
                    .getBytes(StandardCharsets.US_ASCII));
                if (text.charAt(m.start()) == '_') {
                    final int len = m.end() - m.start();
                    for (int i = 0; i < len; i++)
                        result.write(32);
                } else {
                    result.write(Integer.parseInt(m.group(1), 16));
                }
                last = m.end();
            }
            result.write(text.subSequence(last, text.length()).toString()
                .getBytes(StandardCharsets.US_ASCII));
            return result.toByteArray();
        } catch (IOException ex) {
            throw new AssertionError("unreachable", ex);
        }
    }

    /**
     * Decode a byte as a single Base64 character.
     *
     * @param b the byte to decode
     *
     * @return the Base64 value of the byte in the range [0, 63]; or
     * {@link #BASE64_PADDING} if the byte is padding; or
     * {@link #BASE64_WHITESPACE} if the byte is white space; or
     * {@link #BASE64_ILLEGAL_ASCII} if the byte is not a Base64
     * character; or {@link #BASE64_NON_ASCII} if the byte is not
     * US-ASCII
     */
    private static int decodeBase64(byte b) {
        if (b >= 65 && b < 65 + 26) return b - 65;
        if (b >= 97 && b < 97 + 26) return b - (97 - 26);
        if (b >= 48 && b < 48 + 10) return b - (48 - 52);
        if (b < 0) return BASE64_NON_ASCII;
        return switch (b) {
        case 32, 10, 13, 8 -> BASE64_WHITESPACE;
        case 43 -> 62;
        case 47 -> 63;
        case 61 -> BASE64_PADDING;
        default -> BASE64_ILLEGAL_ASCII;
        };
    }

    /**
     * Decode a single Base64 character.
     *
     * @param c the character to decode
     *
     * @return the Base64 value of the character in the range [0, 63];
     * or {@link #BASE64_PADDING} if the byte is padding; or
     * {@link #BASE64_WHITESPACE} if the byte is white space; or
     * {@link #BASE64_ILLEGAL_ASCII} if the byte is not a Base64
     * character; or {@link #BASE64_NON_ASCII} if the byte is not
     * US-ASCII
     */
    private static int decodeBase64(char c) {
        return switch (c) {
        case 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' ->
            c - 'A';
        case 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' ->
            c - 'a' + 26;
        case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> c - '0' + 52;
        case '+' -> 62;
        case '/' -> 63;
        case '=' -> BASE64_PADDING;
        case '\t', '\r', '\n', ' ' -> BASE64_WHITESPACE;
        default -> BASE64_ILLEGAL_ASCII;
        };
    }

    /**
     * Decode a contiguous Base64 sequence.
     *
     * @param text the Base64 characters, excluding padding
     *
     * @return a byte array three quarters of the length of the input
     * containing the decoded bytes
     *
     * @throws IllegalArgumentException if non-Base64 characters are
     * encountered
     */
    private static byte[] decodeBase64(CharSequence text) {
        final int len = text.length();
        final int rlen = len * 3 / 4;
        final byte[] r = new byte[rlen];
        int value = 0;
        int width = 0;
        int p = 0;
        for (int i = 0; i < len; i++) {
            int sextet = decodeBase64(text.charAt(i));
            value <<= 6;
            value |= sextet;
            width += 6;
            while (width >= 8) {
                width -= 8;
                r[p++] = (byte) (value >>> width);
            }
        }
        assert p == rlen;
        return r;
    }

    /**
     * Decode an RFC2047-encoded string. This consists of segments of
     * the form
     * <samp>=?<var>charset</var>?<var>enc</var>?<var>data</var>?=</samp>,
     * which are to be decoded, and other segments that do not match the
     * pattern, and appear in the result unchanged. Either
     * <samp><var>enc</var></samp> is <samp>B</samp> and
     * <samp><var>data</var></samp> is Base64 data, or
     * <samp><var>enc</var></samp> is <samp>Q</samp> and
     * <samp><var>data</var></samp> is Quoted-Printable data. The data
     * is decoded according to the encoding type (which is matched
     * case-insensitively), yielding an octet sequence, which is then
     * converted to a string according to the charset.
     *
     * @param text the input text, normally containing only US-ASCII
     * characters
     *
     * @return the decoded test
     *
     * @throws UnsupportedEncodingException if an unknown charset is
     * encountered
     */
    public static String decode(CharSequence text)
        throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        Matcher m = ESCAPE_SEQUENCE.matcher(text);
        int last = 0;
        while (m.find(last)) {
            result.append(text.subSequence(last, m.start()));
            String csName = m.group("charset");
            String b64Data = m.group("base64");
            final byte[] buf;
            if (b64Data != null) {
                buf = decodeBase64(b64Data);
            } else {
                String qpData = m.group("qp");
                buf = decodeQuotedPrintable(qpData);
            }
            result.append(new String(buf, csName));
            last = m.end();
        }
        result.append(text.subSequence(last, text.length()));
        return result.toString();
    }

    public final String name;

    public final String value;

    private Field(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public static Field ofDecoded(String name, String value) {
        return new Field(name.strip(), value.strip());
    }

}
