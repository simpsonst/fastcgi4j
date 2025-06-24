// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2024, Lancaster University
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

package uk.ac.lancs.fastcgi.http.encoding;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import uk.ac.lancs.fastcgi.misc.Negotiation;

/**
 * Manages compression and other encodings on a response body.
 *
 * @author simpsons
 */
public final class ResponseEncoder {
    private static void populate(Map<String, Map.Entry<Encoding, Float>> dest,
                                 Encoding enc, float quality) {
        dest.put(enc.name(), Map.entry(enc, quality));
    }

    private static final Map<String, Map.Entry<Encoding, Float>> ENCODINGS;

    static {
        Map<String, Map.Entry<Encoding, Float>> temp = new HashMap<>();
        populate(temp, GZIPEncoding.INSTANCE, 1.0f);
        populate(temp, DeflateEncoding.INSTANCE, 0.7f);
        populate(temp, IdentityEncoding.INSTANCE, 0.01f);
        ENCODINGS = Map.copyOf(temp);
    }

    private static final Map<String, Float> COMPRESSION_OFFER =
        ENCODINGS.entrySet().stream().collect(Collectors
            .toMap(Map.Entry::getKey, e -> e.getValue().getValue()));

    /**
     * Provides the context for managing response encodings.
     */
    public interface Context {
        /**
         * Get the raw output stream that will contain the encoded
         * response. This is invoked at most once.
         * 
         * @return the raw output stream
         */
        OutputStream raw();

        /**
         * Get the encoding preference of the client.
         * 
         * @return a mapping from encoding to client preference
         */
        Map<? extends String, ? extends Number> preference();

        /**
         * Set the encoding of the response. This is invoked only if
         * required.
         * 
         * @param names the sequence of decodings to be applied to the
         * encoded response body to restore it
         */
        void setEncoding(List<? extends CharSequence> names);
    }

    private final Context ctxt;

    /**
     * Prepare a response body encoder.
     * 
     * @param ctxt the context to determine the client's encoding
     * preference, obtain the raw output stream and report any applied
     * encodings
     */
    public ResponseEncoder(Context ctxt) {
        this.ctxt = ctxt;
    }

    /**
     * Holds the encoded output stream. If {@code null},
     */
    private OutputStream out = null;

    /**
     * Holds the sequence of encodings to be applied, excluding
     * compression.
     */
    private final List<Encoding> encodings = new ArrayList<>();

    private boolean compressed = false;

    private Encoding compression = null;

    /**
     * Add an encoding to the head of the encoding chain.
     * 
     * @param enc the additional encoding
     * 
     * @throws IllegalStateException if {@link #out()} has been called
     */
    private void prefixEncoding(Encoding enc) {
        /* TODO: Make this method public, when we're sure what we
         * need. */
        if (out != null)
            throw new IllegalStateException("too late to add encoding "
                + enc.name());
        encodings.add(enc);
    }

    /**
     * Add an encoding to the end of the encoding chain.
     * 
     * @param enc the additional encoding
     * 
     * @throws IllegalStateException if {@link #out()} has been called
     */
    private void suffixEncoding(Encoding enc) {
        /* TODO: Make this method public, when we're sure what we
         * need. */
        if (out != null)
            throw new IllegalStateException("too late to add encoding "
                + enc.name());
        encodings.add(0, enc);
    }

    /**
     * Turn on compression if the client accepts it.This is determined
     * by a call to {@link Context#preference()}. If applied, the output
     * stream will be wrapped in a compression filter, and the encoding
     * name is prefixed to the list of encoding names passed to
     * {@link Context#setEncoding(List)}.
     * 
     * <p>
     * In the current implementation, only <samp>gzip</samp> and
     * <samp>deflate</samp> are offered.
     * 
     * @throws IllegalStateException if {@link #out()} has been called
     */
    public void offerCompression() throws IOException {
        if (out != null)
            throw new IllegalStateException("too late to add compression");
        if (compressed) return;
        compressed = true;
        var pref = ctxt.preference();
        String comp =
            Negotiation.resolveAtomPreference(pref, COMPRESSION_OFFER);
        if (comp != null) compression = ENCODINGS.get(comp).getKey();
    }

    /**
     * Get the output stream with encodings applied. On the first call,
     * encodings specified by other calls are applied to the raw stream
     * obtained with {@link Context#raw()}, and the names of applied
     * encodings are passed to {@link Context#setEncoding(List)} in
     * reverse order. Subsequent calls will yield the same stream.
     * Calling this method prevents the calling of other methods that
     * modify encoding.
     * 
     * <p>
     * Methods that modify encodings, and therefore cannot be called
     * after this one, include:
     * 
     * <ul>
     * 
     * <li>{@link #offerCompression()}
     * 
     * </ul>
     * 
     * @return the current head of the output stream chain
     * 
     * @throws IOException if an I/O error occurs in applying an
     * encoding
     */
    public OutputStream out() throws IOException {
        if (out != null) return out;

        /* The encodings' names must specified in the reverse order that
         * they were applied in. */
        OutputStream out = ctxt.raw();
        List<String> names = new ArrayList<>();
        for (Encoding enc : encodings) {
            if (enc == IdentityEncoding.INSTANCE) continue;
            names.add(0, enc.name());
            out = enc.encode(out);
        }

        /* Compression must be applied last, and listed first. */
        if (compression != null) {
            names.add(0, compression.name());
            out = compression.encode(out);
        }

        this.out = out;
        if (!names.isEmpty()) ctxt.setEncoding(names);
        return out;
    }
}
