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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Expresses an immutable MIME content disposition.
 * 
 * @author simpsons
 */
public final class Disposition {
    private final String type;

    private final String name;

    private final String filename;

    private Disposition(String type, String name, String filename) {
        this.type = type;
        this.name = name;
        this.filename = filename;
    }

    /**
     * Get the disposition type.
     * 
     * @return the disposition type
     */
    public String type() {
        return type;
    }

    /**
     * Get the disposition name.
     * 
     * @return the disposition name; or {@code null} if not specified
     */
    public String name() {
        return name;
    }

    /**
     * Get the disposition filename.
     * 
     * @return the disposition filename;or {@code null} if not specified
     */
    public String filename() {
        return filename;
    }

    /**
     * Create a disposition from a string, such as a header field.
     * 
     * @param text the string to parse
     * 
     * @return the disposition; or {@code null} if the text is
     * {@code null}
     */
    public static Disposition fromString(CharSequence text) {
        if (text == null) return null;
        Tokenizer tok = new Tokenizer(text);
        Map<String, String> params = new HashMap<>();
        CharSequence type = tok.whitespaceAtomParameters(0, params);
        if (type == null)
            throw new IllegalArgumentException("not disposition: " + text);
        return new Disposition(type.toString(), params.get("name"),
                               params.get("filename"));
    }

    /**
     * Create a form-data disposition.
     * 
     * @param name the field name
     * 
     * @return the requested disposition
     */
    public static Disposition formData(String name) {
        return new Disposition("form-data", name, null);
    }

    /**
     * Create a form-data disposition with a filename.
     * 
     * @param name the field name
     * 
     * @param filename the filename
     * 
     * @return the requested disposition
     */
    public static Disposition fileFormData(String name, String filename) {
        return new Disposition("form-data", name, filename);
    }

    /**
     * Get a string representation of this disposition. This is suitable
     * as a MIME header field.
     * 
     * @return the string representation of this disposition
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(type);
        if (name != null)
            result.append("; name=").append(Tokenizer.quoteOptionally(name));
        if (filename != null) result.append("; filename=")
            .append(Tokenizer.quoteOptionally(name));
        return result.toString();
    }

    private static Disposition decode(List<? extends CharSequence> seq) {
        if (seq.isEmpty()) return null;
        CharSequence last = seq.get(seq.size() - 1);
        return fromString(last);
    }

    private static List<String> encode(Disposition obj) {
        if (obj == null) return Collections.emptyList();
        return Collections.singletonList(obj.toString());
    }

    /**
     * Converts between internal and external representations of MIME
     * content dispositions. The encoder translates {@code null} into an
     * empty sequence, and other values into a singleton. The decoder
     * translates an empty sequence into {@code null}, and yields only
     * the last element of non-empty sequences.Coalescing ignores the
     * former value.
     */
    public static final Format<Disposition> FORMAT =
        Format.of("content-disposition", Disposition.class, Disposition::decode,
                  Disposition::encode, (a, b) -> b);
}
