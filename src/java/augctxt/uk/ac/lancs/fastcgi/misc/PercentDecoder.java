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

package uk.ac.lancs.fastcgi.misc;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decodes percent-encoded strings according to a character encoding.
 *
 * Sequences of percent-encoded bytes are decoded into bytes, and then
 * each byte sequence is converted to characters. Character sequences
 * that do not constitute percent-encoded bytes are not converted. The
 * converted and unconverted sequences are then concatenated.
 * 
 * @todo Is this the right way? Two alternatives both involve converting
 * the unencoded sequences into bytes, concatenating all decoded and
 * converted sequences, and then applying a single character decoding.
 * Converting the encoded sequences into bytes according to the supplied
 * character encoding probably ought to produce the same as the current
 * implementation. Converting the normal characters to (say) US-ASCII
 * first instead would be interestingly different.
 * 
 * @author simpsons
 */
final class PercentDecoder {
    private static final Pattern PCENC = Pattern.compile("(%[0-9A-F]{2})+");

    private static final Pattern SPENC = Pattern.compile("(\\+)+");

    private static int hexval(char c) {
        return switch (c) {
        case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> c - '0';
        case 'A', 'B', 'C', 'D', 'E', 'F' -> c - 'A';
        default -> -1;
        };
    }

    private final Charset charset;

    private byte[] buf = new byte[10];

    /**
     * Create a decoder for a given character encoding.
     * 
     * @param charset the character encoding to interpret bytes as after
     * decoding
     */
    public PercentDecoder(Charset charset) {
        this.charset = charset;
    }

    /**
     * Create a decoder for UTF-8
     */
    public PercentDecoder() {
        this(StandardCharsets.UTF_8);
    }

    /**
     * Decode a character sequence with embedded percent-encoded bytes.
     * Pluses are not converted into spaces.
     * 
     * @param input the input characters
     * 
     * @return the decoded characters
     */
    public String decode(CharSequence input) {
        return decodeInternal(input, StringBuilder::append);
    }

    private static void convertPlusToSpace(StringBuilder result,
                                           CharSequence part) {
        int last = 0;
        for (Matcher m = SPENC.matcher(part); m.find();) {
            result.append(part.subSequence(last, m.start()));
            final int slen = m.end() - m.start();
            for (int i = 0; i < slen; i++)
                result.append(' ');
            last = m.end();
        }
        result.append(part.subSequence(last, part.length()));
    }

    /**
     * Decode a character sequence with embedded percent-encoded bytes
     * and spaces encoded as pluses. Pluses are converted into spaces.
     * 
     * @param input the input characters
     * 
     * @return the decoded characters
     */
    public String decodeParameter(CharSequence input) {
        return decodeInternal(input, PercentDecoder::convertPlusToSpace);
    }

    private String
        decodeInternal(CharSequence input,
                       BiConsumer<StringBuilder, CharSequence> normal) {
        StringBuilder result = new StringBuilder();
        int last = 0;
        for (Matcher m = PCENC.matcher(input); m.find();) {
            normal.accept(result, input.subSequence(last, m.start()));
            final int slen = (m.end() - m.start()) / 3;
            if (slen > buf.length) {
                final int nlen = slen + 12;
                buf = new byte[nlen];
            }
            for (int j = 0, i = m.start(); i < m.end(); j++, i += 3)
                buf[j] = (byte) ((hexval(input.charAt(i + 1)) << 4) |
                    hexval(input.charAt(i + 2)));
            result.append(new String(buf, 0, slen, charset));
            last = m.end();
        }
        result.append(input.subSequence(last, input.length()));
        return result.toString();
    }
}
