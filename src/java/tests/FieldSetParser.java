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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import uk.ac.lancs.io.UnclosedInputStream;

/**
 * Parses header or trailer fields from a byte stream. Lines beginning
 * with white space are treated as continuations. An empty line
 * terminates the input. One byte at a time is read to detect CRLF line
 * terminators.
 * 
 * <p>
 * Each line is then interpreted as US-ASCII. This avoids reading bytes
 * beyond the field set, which likely require different interpretation.
 * For efficiency, the base stream should be buffered to compensate for
 * byte-by-byte processing.
 * 
 * @author simpsons
 */
public class FieldSetParser {
    private final InputStream in;

    private final Consumer<? super CharSequence> firstLineConsumer;

    private final BiConsumer<? super CharSequence,
                             ? super CharSequence> fieldConsumer;

    private FieldSetParser(InputStream in,
                           BiConsumer<? super CharSequence,
                                      ? super CharSequence> fieldConsumer,
                           Consumer<? super CharSequence> firstLineConsumer) {
        this.firstLineConsumer = firstLineConsumer;
        this.fieldConsumer = fieldConsumer;
        this.in = new UnclosedInputStream(in);
    }

    /**
     * Create a parser from a base stream and separate actions.
     * 
     * @param in the base stream
     * 
     * @param fieldConsumer to be invoked each time a name and value are
     * parsed
     * 
     * @param firstLineConsumer to be invoked for the first line; or
     * {@code null} if no first line is expected (only fields)
     * 
     * @return the parser
     */
    public static FieldSetParser
        of(InputStream in,
           BiConsumer<? super CharSequence, ? super CharSequence> fieldConsumer,
           Consumer<? super CharSequence> firstLineConsumer) {
        return new FieldSetParser(in, fieldConsumer, firstLineConsumer);
    }

    /**
     * Create a parser for a field set with no first line.
     * 
     * @param in the base stream
     * 
     * @param dest whose
     * {@link FieldSet#acceptField(CharSequence, CharSequence)} is to be
     * invoked each time a name and value are parsed
     * 
     * @return the parser
     */
    public static FieldSetParser of(InputStream in, FieldSet dest) {
        return new FieldSetParser(in, dest::acceptField, null);
    }

    /**
     * Create a parser for a field set with a first line.
     * 
     * @param in the base stream
     * 
     * @param dest whose
     * {@link Header#acceptField(CharSequence, CharSequence)} is to be
     * invoked each time a name and value are parsed, and whose
     * {@link Header#acceptFirstLine(CharSequence)} is to be invoked to
     * parse the first line
     * 
     * @return the parser
     */
    public static FieldSetParser of(InputStream in, Header dest) {
        return new FieldSetParser(in, dest::acceptField, dest::acceptFirstLine);
    }

    private boolean ended = false;

    private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

    private static String escaped(CharSequence txt) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < txt.length(); i++) {
            char c = txt.charAt(i);
            switch (c) {
            default -> {
                if (c < 32 || c >= 127)
                    out.append(String.format("\\x%02x", (int) c));
                else
                    out.append(c);
            }

            case '\n' -> out.append("\\n");

            case '\t' -> out.append("\\t");

            case '\r' -> out.append("\\r");
            }
        }
        return out.toString();
    }

    private String readLine() throws IOException {
        if (ended) return null;
        int c;
        boolean lcr = false;
        while ((c = in.read()) >= 0 && (c != '\n' || !lcr)) {
            lcr = c == '\r';
            buf.write((char) c);
        }

        /* Delete the CRLF if present, or record the end of the stream
         * if not, */
        if (c < 0) {
            ended = true;
            throw new EOFException("incomplete line");
        }
        byte[] array = buf.toByteArray();
        String r =
            new String(array, 0, array.length - 1, StandardCharsets.US_ASCII);
        buf.reset();
        return r;
    }

    private String cont = null;

    private void processCompleteLine() {
        if (cont == null) return;
        int colon = cont.indexOf(':');
        if (colon < 0) return;
        String name = cont.substring(0, colon).trim();
        String value = cont.substring(colon + 1).trim();
        fieldConsumer.accept(name, value);
    }

    public void readAll() throws IOException {
        if (ended) return;
        if (firstLineConsumer != null) {
            var firstLine = readLine();
            if (firstLine == null) throw new EOFException("first line missing");
            firstLineConsumer.accept(firstLine);
        }
        while (!ended) {
            String line = readLine();
            if (line.isEmpty()) break;
            switch (line.charAt(0)) {
            case ' ', '\t':
                cont += line;
                break;

            default:
                processCompleteLine();
                cont = line;
                break;
            }
        }
        processCompleteLine();
    }
}
