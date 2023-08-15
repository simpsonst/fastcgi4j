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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides constants and basic functions for processing
 * quoted-printable data.
 * 
 * @author simpsons
 */
class QuotedPrintable {
    private QuotedPrintable() {}

    private static final Pattern ESCAPE_SEQUENCE =
        Pattern.compile("=([0-9A-F]{2})|_");

    static int hexval(byte b) {
        return switch (b) {
        case 48, 49, 50, 51, 52, 53, 54, 55, 56, 57 -> b - 48;
        case 65, 66, 67, 68, 69, 70 -> b - 55;
        default -> -1;
        };
    }

    private static int hexval(char c) {
        return switch (c) {
        case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> c - '0';
        case 'A', 'B', 'C', 'D', 'E', 'F' -> c - 'A' + 10;
        default -> -1;
        };
    }

    private static int hexval(CharSequence seq) {
        assert seq.length() == 3;
        assert seq.charAt(0) == '=';
        int n1 = hexval(seq.charAt(1));
        int n2 = hexval(seq.charAt(2));
        assert n1 >= 0 && n1 <= 15;
        assert n2 >= 0 && n2 <= 15;
        return (n1 << 4) | n2;
    }

    /**
     * Decode a Quoted-Printable stream, closing it after use.
     * 
     * @param in the encoded stream
     * 
     * @return a decoded stream which closes the encoded stream when it
     * is closed
     */
    public static InputStream decodeCloseAfter(InputStream in) {
        return new QuotedPrintableDecodingInputStream(in, true);
    }

    /**
     * Decode a Quoted-Printable stream, leaving it open after use.
     * 
     * @param in the encoded stream
     * 
     * @return a decoded stream which does not close the encoded stream
     * when it is closed
     */
    public static InputStream decodeLeaveOpen(InputStream in) {
        return new QuotedPrintableDecodingInputStream(in, false);
    }

    /**
     * Decode a contiguous Quoted-Printable sequence, as found in an
     * RFC2047 encoded word. All characters are first converted into
     * bytes, then decoded according to a specified character encoding.
     * A sequence of the form <samp>=<var>XX</var></samp> is converted
     * to a single byte whose value is <var>XX</var> in hexadecimal. An
     * underscore <samp>_</samp> is converted to US-ASCII space (32).
     * Other characters are converted to US-ASCII bytes.
     * 
     * @see <a href=
     * "https://datatracker.ietf.org/doc/html/rfc2047.html">RFC2047:
     * Message Header Extensions</a>
     *
     * @param text the quoted printable characters
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
        byte[] res = new byte[text.length()];
        int pos = 0;
        int last = 0;
        for (Matcher m = ESCAPE_SEQUENCE.matcher(text); m.find();) {
            while (last < m.start())
                res[pos++] = (byte) text.charAt(last++);
            String hex = m.group(1);
            if (hex == null)
                res[pos++] = ASCII.SPACE;
            else
                res[pos++] = (byte) hexval(hex);
        }
        final int lim = text.length();
        while (last < lim)
            res[pos++] = (byte) text.charAt(last++);
        assert last == lim;
        return new String(res, 0, pos, charset);
    }
}