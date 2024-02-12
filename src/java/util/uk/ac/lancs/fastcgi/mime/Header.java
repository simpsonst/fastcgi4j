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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Holds a MIME message header.
 *
 * @author simpsons
 */
public final class Header {
    /**
     * Create a header from a byte stream. The stream is left open at
     * the first byte after the header.
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
    public static Header of(InputStream in) throws IOException {
        Map<String, List<String>> result =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String name = null;
        String value = null;
        byte[] buf = new byte[1024];
        int pos = 0;
        int c;
        int colon = -1;
        int ws = -1;
        while ((c = in.read()) >= 0) {
            if (c >= ASCII.DEL) throw new CharacterCodingException();
            if (c == ASCII.LF && pos > 0 && buf[pos - 1] == ASCII.CR) {
                if (pos == 1) {
                    /* This is the end of the header. Store the previous
                     * field. */
                    if (name != null) {
                        assert value != null;
                        result.computeIfAbsent(name, k -> new ArrayList<>())
                            .add(value);
                    }
                    break;
                }

                if (buf[0] == ASCII.SPACE || buf[0] == ASCII.TAB) {
                    /* This is a continuation of the previous line. We
                     * include the leading space or tab. */
                    String cont =
                        new String(buf, 0, pos - 1, StandardCharsets.US_ASCII);
                    if (value == null) throw new CharacterCodingException();
                    value += cont;
                    colon = -1;
                    ws = -1;
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
                    final int st = ws > colon ? ws : colon + 1;
                    value = new String(buf, st, pos - 1 - st,
                                       StandardCharsets.US_ASCII);
                    colon = -1;
                    ws = -1;
                }
                pos = 0;
            } else if (pos > 0 && buf[pos - 1] == ASCII.CR) {
                /* A previous CR must be followed by LF. */
                throw new CharacterCodingException();
            } else if (c < ASCII.SPACE && c != ASCII.TAB && c != ASCII.CR) {
                /* Other control characters are not permitted. */
                throw new CharacterCodingException();
            } else {
                /* Add the byte to the buffer. */
                if (pos == buf.length) {
                    /* Enlarge the buffer. */
                    int nl = buf.length + 1024;
                    if (nl < buf.length) nl = Integer.MAX_VALUE;
                    buf = Arrays.copyOf(buf, nl);
                }
                if (colon < 0) {
                    if (c == ASCII.COLON) colon = pos;
                } else {
                    if (ws < 0 && c != ASCII.SPACE && c != ASCII.TAB &&
                        c != ASCII.CR) ws = pos;
                }
                buf[pos++] = (byte) c;
            }
        }
        return of(result);
    }

    private interface Datum {
        <T> T getTyped(Format<T> type);

        List<? extends CharSequence> getRaw();
    }

    private static final class TypedDatum implements Datum {
        private List<? extends CharSequence> parts;

        private final Object data;

        private final Format<?> format;

        private final Map<Format<?>, Object> other = new ConcurrentHashMap<>();

        TypedDatum(Object data, Format<?> format) {
            this.data = data;
            this.format = format;
            other.put(format, data);
        }

        private final Lock lock = new ReentrantLock();

        @Override
        public <T> T getTyped(Format<T> type) {
            return type
                .cast(other.computeIfAbsent(type, t -> t.decode(getRaw())));
        }

        @Override
        public List<? extends CharSequence> getRaw() {
            lock.lock();
            try {
                if (parts != null) return parts;
                parts = format.encode(data);
                return parts;
            } finally {
                lock.unlock();
            }
        }
    }

    private static final class RawDatum implements Datum {
        private final List<String> parts;

        private final Map<Format<?>, Object> data = new ConcurrentHashMap<>();

        RawDatum(Collection<? extends String> parts) {
            this.parts = List.copyOf(parts);
        }

        @Override
        public <T> T getTyped(Format<T> format) {
            return format
                .cast(data.computeIfAbsent(format, t -> t.decode(parts)));
        }

        @Override
        public List<? extends CharSequence> getRaw() {
            return parts;
        }
    }

    private final Map<String, Datum> fields;

    /**
     * Get the set of all field names.
     * 
     * @return an immutable set view of all field names
     */
    public Set<String> names() {
        return fields.keySet();
    }

    private Header(Map<String, Datum> fields) {
        this.fields = fields;
    }

    /**
     * Create a header from an index of value sequences.
     * 
     * @param fields the header names and values
     */
    public static Header
        of(Map<? extends String,
               ? extends Collection<? extends String>> fields) {
        Map<String, Datum> data = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (var ent : fields.entrySet()) {
            String name = ent.getKey();
            var rawVal = ent.getValue();
            if (rawVal.isEmpty()) continue;
            Datum datum = new RawDatum(rawVal);
            data.put(name, datum);
        }
        return new Header(Collections.unmodifiableMap(data));
    }

    /**
     * Get a header field.
     * 
     * @param <T> the field type
     * 
     * @param name the field name
     * 
     * @param format the format
     * 
     * @return the value of the field, possibly {@code null} if not
     * present
     */
    public <T> T get(String name, Format<T> format) {
        Datum datum = fields.get(name);
        if (datum == null) return format.decode(Collections.emptyList());
        return datum.getTyped(format);
    }

    private static final Header EMPTY = new Header(Collections.emptyMap());

    /**
     * Get an empty header.
     * 
     * @return an empty header
     */
    public static Header empty() {
        return EMPTY;
    }

    /**
     * Records modifications to a header before applying them.
     */
    public final class Modification {
        final Collection<String> removals =
            new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        static final class Entry {
            Format<?> format;

            Object data;

            Entry() {}

            Entry(Format<?> format, Object data) {
                this.format = format;
                this.data = data;
            }

            void merge(Format<?> format, Object newValue) {
                if (!format.equals(this.format))
                    throw new IllegalArgumentException("cannot append " + format
                        + " to " + this.format);
                this.data = format.coalesce(this.data, newValue);
            }

            void set(Format<?> format, Object newValue) {
                this.format = format;
                this.data = newValue;
            }
        }

        final Map<String, Entry> replacements =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        Modification() {}

        private Datum modified(String key) {
            if (removals.contains(key)) {
                Entry ent = replacements.get(key);
                if (ent == null) return null;
                return new TypedDatum(ent.data, ent.format);
            }
            Datum old = fields.get(key);
            Entry ent = replacements.get(key);
            if (ent == null) return old;
            Object nv = old == null ? ent.data :
                ent.format.coalesce(old.getTyped(ent.format), ent.data);
            return new TypedDatum(nv, ent.format);
        }

        /**
         * Apply the modifications.
         * 
         * @return a new header identical to the original except for the
         * modifications
         */
        public Header apply() {
            /* Get a union of names of all fields in the original, and
             * of all new ones. */
            Collection<String> keys =
                new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            keys.addAll(Stream
                .concat(fields.keySet().stream(),
                        replacements.keySet().stream())
                .collect(Collectors.toList()));
            Map<String, Datum> newFields =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (var k : keys) {
                var v = modified(k);
                if (v == null) continue;
                newFields.put(k, v);
            }
            return new Header(Collections.unmodifiableMap(newFields));
        }

        /**
         * Remove all values of a field. No values from the original
         * header or added earlier with {@link #add(String, String)} or
         * {@link #put(String, String)} will be preserved.
         * 
         * @param key the field name
         * 
         * @return this object
         */
        public Modification remove(String key) {
            key = key.trim();
            replacements.remove(key);
            removals.add(key);
            return this;
        }

        /**
         * Add a field. Existing fields with the same name will be
         * preserved by merging this latest value into them.
         * 
         * @param <T> the field type
         * 
         * @param key the field name
         * 
         * @param format the format, which must match the format of
         * existing values
         * 
         * @param value the additional value
         * 
         * @return this object
         */
        public <T> Modification add(String key, Format<T> format, T value) {
            key = key.trim();
            Entry cur = replacements.computeIfAbsent(key, k -> new Entry());
            cur.merge(format, value);
            return this;
        }

        /**
         * Set a field. No values from the original header or added
         * earlier will be preserved.
         * 
         * @param <T> the field type
         * 
         * @param key the field name
         * 
         * @param value the field value
         * 
         * @return this object
         */
        public <T> Modification set(String key, Format<T> format, T value) {
            key = key.trim();
            removals.remove(key);
            replacements.put(key, new Entry(format, value));
            return this;
        }
    }

    /**
     * Prepare to modify the header. Note that, in fact, a new header is
     * created, and this header remains unmodifiable.
     * 
     * @return a fresh modification
     * 
     * @constructor
     */
    public Modification modify() {
        return new Modification();
    }

    /**
     * @undocumented
     */
    public static void main(String[] args) throws Exception {
        Header hdr = Header.of(System.in);
        for (String name : hdr.names())
            System.out.printf("%s: %s%n", escape(name),
                              escape(hdr.get(name, Format.LAST_STRING_FORMAT)));
        System.out.println("Remaining data:");
        System.in.transferTo(System.out);
    }

    private static String escape(CharSequence text) {
        if (text == null) return null;
        StringBuilder result = new StringBuilder();
        final int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            switch (c) {
            case '\r' -> result.append("\\r");

            case '\t' -> result.append("\\t");

            case '\n' -> result.append("\\n");

            case '\\' -> result.append("\\\\");

            default -> result.append(c);
            }
        }
        return result.toString();
    }
}
