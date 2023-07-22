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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Holds a MIME message header.
 *
 * @author simpsons
 */
public final class Header {
    /**
     * Parse a MIME header from a stream. The stream position will be
     * left just after the terminating CRLF.
     *
     * @param in the source stream
     *
     * @return a collection of lists of raw field values, indexed by
     * case-insensitive field names
     *
     * @throws CharacterCodingException if a non-US-ASCII character, an
     * illegal control character, or an isolated CR or LF is
     * encountered; if the first line is a continuation; if a
     * non-continuation line does not contain a colon
     *
     * @throws IOException if an error occurs reading the source stream
     */
    private static Map<String, List<String>> parseFields(InputStream in)
        throws IOException {
        Map<String, List<String>> result =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String name = null;
        String value = null;
        byte[] buf = new byte[1024];
        int pos = 0;
        int c;
        int colon = -1;
        while ((c = in.read()) >= 0) {
            if (c >= ASCII.DEL) throw new CharacterCodingException();
            if (c == ASCII.LF) {
                if (pos == 0 || buf[pos - 1] != ASCII.CR)
                    throw new CharacterCodingException();
                if (pos == 0) {
                    /* This is the end of the header. Store the previous
                     * field. */
                    if (name != null) {
                        assert value != null;
                        result.computeIfAbsent(name, k -> new ArrayList<>())
                            .add(value);
                    }
                } else if (buf[0] == ASCII.SPACE || buf[0] == ASCII.TAB) {
                    /* This is a continuation of the previous line. We
                     * include the leading space or tab. */
                    if (value == null) throw new CharacterCodingException();
                    value +=
                        new String(buf, 0, pos - 1, StandardCharsets.US_ASCII);
                } else {
                    /* Store the previous field. */
                    if (name != null) {
                        assert value != null;
                        result.computeIfAbsent(name, k -> new ArrayList<>())
                            .add(value);
                    }
                    /* Start a new field. */
                    if (colon == -1) throw new CharacterCodingException();
                    name = new String(buf, 0, colon).trim();
                    value = new String(buf, colon + 1, pos - 1,
                                       StandardCharsets.US_ASCII);
                }
                pos = 0;
            } else if (pos > 0 && buf[pos - 1] == ASCII.CR) {
                /* A previous CR must be followed by LF. */
                throw new CharacterCodingException();
            } else if (c < ASCII.SPACE && c != ASCII.TAB) {
                /* Other control characters are not permitted. */
                throw new CharacterCodingException();
            } else {
                /* Add the byte to the buffer. */
                if (pos == buf.length) {
                    /* Enlarge the buffer. */
                    int nl = buf.length + 1024;
                    if (nl < buf.length) nl = Integer.MAX_VALUE;
                    byte[] nb = new byte[nl];
                    System.arraycopy(buf, 0, nb, 0, nl);
                    buf = nb;
                }
                if (c == ASCII.COLON && colon < 0) colon = pos;
                buf[pos++] = (byte) c;
            }
        }
        return result;
    }

    /**
     * Create a header from a byte stream.
     * 
     * @param in the source stream
     *
     * @throws CharacterCodingException if a non-US-ASCII character, an
     * illegal control character, or an isolated CR or LF is
     * encountered; if the first line is a continuation; if a
     * non-continuation line does not contain a colon
     *
     * @throws IOException if an error occurs reading the source stream
     */
    public Header(InputStream in) throws IOException {
        this(parseFields(in));
    }

    private final Map<String, List<String>> fields;

    /**
     * Create a header from an index of value sequences.
     * 
     * @param fields the header names and values
     */
    public Header(Map<? extends String,
                      ? extends Collection<? extends String>> fields) {
        Map<String, List<String>> data = new HashMap<>();
        for (var ent : fields.entrySet()) {
            String name = ent.getKey().toLowerCase(Locale.ROOT);
            List<String> value = List.copyOf(ent.getValue());
            if (value.isEmpty()) continue;
            data.put(name, value);
        }
        this.fields = Map.copyOf(data);
    }

    /**
     * Get the last value of a field.
     * 
     * @param name the field name
     * 
     * @return the last value; or {@code null} if not present
     */
    public String get(String name) {
        List<String> seq = fields.get(name.toLowerCase(Locale.ROOT));
        if (seq == null) return null;
        int len = seq.size();
        return seq.get(len - 1);
    }

    /**
     * Get the last value of a field, or a default value.
     * 
     * @param name the field name
     * 
     * @param assumed the default value
     * 
     * @return the last value; or the default if not present
     */
    public String get(String name, String assumed) {
        String value = get(name);
        if (value == null) return assumed;
        return value;
    }

    /**
     * Get the last value of a field as a media type.
     * 
     * @param name the field name
     * 
     * @return the qualified media type
     * 
     * @throws IllegalArgumentException if the field is present but does
     * not parse as a qualified media type
     */
    public MediaType getMediaType(String name) {
        String value = get(name, "text/plain; charset=US-ASCII");
        return MediaType.fromString(value);
    }

    /**
     * Get the content type. This uses the last value of the field
     * <samp>Content-Type</samp>, and parses it as a qualified media
     * type.
     * 
     * @return the qualified media type; or <samp>text/plain</samp> if
     * not present, with a character encoding of <samp>US-ASCII</samp>
     * 
     * @throws IllegalArgumentException if the field is present but does
     * not parse as a qualified media type
     */
    public MediaType getMediaType() {
        return getMediaType("Content-Type");
    }

    /**
     * Get the concatenated values of a field. Values are joined with a
     * comma and a space.
     * 
     * @param name the field name
     * 
     * @return the concatenated value; an empty string if the field is
     * not present
     */
    public String getList(String name) {
        List<String> seq = fields.get(name.toLowerCase(Locale.ROOT));
        if (seq == null) return "";
        return seq.stream().collect(Collectors.joining(", "));
    }
}
