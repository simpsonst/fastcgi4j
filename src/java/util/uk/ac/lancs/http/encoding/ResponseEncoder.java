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

package uk.ac.lancs.http.encoding;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import uk.ac.lancs.mime.Negotiation;

/**
 * Manages compression and other encodings on a response body.
 *
 * @author simpsons
 */
public final class ResponseEncoder {
    /**
     * Provides the context for managing response encodings. This
     * abstracts the presentation of an HTTP request down to the few
     * operations that are required:
     * 
     * <ol>
     * 
     * <li>extraction of the client's preferences (usually from the
     * <samp>Accept-Encoding</samp> and <samp>TE</samp> header fields,
     * to perform content negotiation;
     * 
     * <li>setting of the response header fields
     * <samp>Content-Encoding</samp> and <samp>Transfer-Encoding</samp>
     * as the result of content negotiation and any explicit application
     * of content encodings by the application; and
     * 
     * <li>acquisition of the raw output stream, so the encodings can be
     * applied.
     * 
     * </ol>
     */
    public interface Context {
        /**
         * Get the raw output stream that will contain the encoded
         * response.
         * 
         * @apiNote This is invoked at most once when {@link #out()} is
         * called, and not before {@link #setContentEncoding(List)} or
         * {@link #setTransferEncoding(List)}.
         * 
         * @return the raw output stream
         */
        OutputStream raw();

        /**
         * Get the content-encoding preference of the client.
         * 
         * @return a mapping from content encoding to client preference
         */
        Map<? extends String, ? extends Number> contentPreference();

        /**
         * Get the transfer-encoding preference of the client.
         * 
         * @return a mapping from transfer encoding to client preference
         */
        Map<? extends String, ? extends Number> transferPreference();

        /**
         * Set the content encoding of the response.
         * 
         * @apiNote This is not necessarily invoked if not required.
         * 
         * @param names the sequence of decodings to be applied to the
         * encoded response body to restore it
         */
        void setContentEncoding(List<? extends CharSequence> names);

        /**
         * Set the transfer encoding of the response.
         * 
         * @apiNote This is not necessarily invoked if not required.
         * 
         * @param names the sequence of decodings to be applied to the
         * encoded response body to restore it
         */
        void setTransferEncoding(List<? extends CharSequence> names);
    }

    private final Map<? extends String,
                      ? extends Map.Entry<? extends OutputEncoding,
                                          ? extends Number>> contentAvailable;

    private final Map<? extends String,
                      ? extends Map.Entry<? extends OutputEncoding,
                                          ? extends Number>> transferAvailable;

    private final Context ctxt;

    /**
     * Prepare a response body encoder. The application provides
     * descriptions of the content and transfer encodings it offers for
     * content negotiation, and a minimal representation of the HTTP
     * request being processed.
     * 
     * @param contentAvailable the set of content encodings available,
     * indexed by name, and mapped to their implementation and quality
     * setting
     * 
     * @param transferAvailable the set of transfer encodings available,
     * indexed by name, and mapped to their implementation and quality
     * setting
     * 
     * @param ctxt the context to determine the client's
     * content/transfer encoding preferences, obtain the raw output
     * stream and report any applied encodings
     */
    public ResponseEncoder(Map<? extends String,
                               ? extends Map.Entry<? extends OutputEncoding,
                                                   ? extends Number>> contentAvailable,
                           Map<? extends String,
                               ? extends Map.Entry<? extends OutputEncoding,
                                                   ? extends Number>> transferAvailable,
                           Context ctxt) {
        this.contentAvailable = contentAvailable;
        this.transferAvailable = transferAvailable;
        this.ctxt = ctxt;
    }

    /**
     * Holds the encoded output stream. If {@code null}, the stream has
     * not been requested through {@link #out()}.
     */
    private OutputStream out = null;

    /**
     * Holds the sequence of content encodings to be applied, excluding
     * compression.
     */
    private final List<Map.Entry<OutputEncoding, String>> contentEncodings =
        new ArrayList<>();

    /**
     * Append an encoding with a designated label.
     * 
     * @param enc the encoding
     * 
     * @param label the label; or {@code null} if the encoding's
     * canonical name is to be used
     */
    public void suffix(OutputEncoding enc, CharSequence label) {
        var entry = Map.entry(enc, label == null ? null : label.toString());
        contentEncodings.add(entry);
    }

    /**
     * Prefix an encoding with a designated label.
     * 
     * @param enc the encoding
     * 
     * @param label the label; or {@code null} if the encoding's
     * canonical name is to be used
     */
    public void prefix(OutputEncoding enc, CharSequence label) {
        var entry = Map.entry(enc, label == null ? null : label.toString());
        contentEncodings.add(0, entry);
    }

    /**
     * Append an encoding with its canonical name.
     * 
     * @param enc the encoding
     */
    public void suffix(OutputEncoding enc) {
        suffix(enc, enc.name());
    }

    /**
     * Prefix an encoding with its canonical name.
     * 
     * @param enc the encoding
     */
    public void prefix(OutputEncoding enc) {
        prefix(enc, enc.name());
    }

    /**
     * Append several encodings with designated labels.
     * 
     * @param encs the sequence of encodings to apply (as keys) and
     * their designated labels (as values; or {@code null} to use an
     * encoding's canonical name)
     */
    public void
        suffixWithLabels(List<? extends Map.Entry<? extends OutputEncoding,
                                                  ? extends CharSequence>> encs) {
        for (var entry : encs)
            suffix(entry.getKey(), entry.getValue());
    }

    /**
     * Prefix several encodings with designated labels.
     * 
     * @param encs the sequence of encodings to apply (as keys) and
     * their designated labels (as values; or {@code null} to use an
     * encoding's canonical name)
     */
    public void
        prefixWithLabels(List<? extends Map.Entry<? extends OutputEncoding,
                                                  ? extends CharSequence>> encs) {
        for (var entry : encs.reversed())
            prefix(entry.getKey(), entry.getValue());
    }

    /**
     * Append several encodings with their canonical names.
     * 
     * @param encs the sequence of encodings to be applied
     */
    public void suffix(List<? extends OutputEncoding> encs) {
        for (var enc : encs)
            suffix(enc);
    }

    /**
     * Prefix several encodings with their canonical names.
     * 
     * @param encs the sequence of encodings to be applied
     */
    public void prefix(List<? extends OutputEncoding> encs) {
        for (var enc : encs.reversed())
            prefix(enc);
    }

    /**
     * Determine whether any of the content encodings applies
     * compression.
     * 
     * @return {@code true} if an applied content encoding applies
     * compression; {@code false} otherwise
     */
    private boolean isContentCompressed() {
        for (var entry : contentEncodings) {
            var enc = entry.getKey();
            if (enc.compressionQuality() != null) return true;
        }
        return false;
    }

    private String suggestContentCompression() {
        var offer = contentAvailable.entrySet().stream()
            .collect(Collectors
                .toMap(Map.Entry::getKey,
                       e -> e.getValue().getValue().floatValue()));
        var pref = ctxt.contentPreference();
        return Negotiation.resolveAtomPreference(pref, offer);
    }

    private String suggestTransferCompression() {
        var offer = transferAvailable.entrySet().stream()
            .collect(Collectors
                .toMap(Map.Entry::getKey,
                       e -> e.getValue().getValue().floatValue()));
        var pref = ctxt.transferPreference();
        return Negotiation.resolveAtomPreference(pref, offer);
    }

    /**
     * Determine whether the
     * 
     * @param entry
     * 
     * @return
     */
    private static String chooseLabel(Map.Entry<? extends OutputEncoding,
                                                ? extends CharSequence> entry) {
        var label = entry.getValue();
        if (label == null) return entry.getKey().name();
        return label.toString();
    }

    /**
     * Get the output stream to which encodings will be applied. On the
     * first call, encodings specified by other calls are applied to the
     * raw stream obtained with {@link Context#raw()}, and the names of
     * applied encodings are passed to
     * {@link Context#setContentEncoding(List)} in reverse order.
     * Subsequent calls will yield the same stream. Calling this method
     * prevents the calling of other methods that modify encoding.
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

        List<Map.Entry<OutputEncoding, String>> transferEncodings =
            new ArrayList<>(1);

        /* Check whether compression is applied to any of the content
         * encodings. */
        if (!isContentCompressed()) {
            /* Use content negotiation to determine whether suffix the
             * content encoding with a compression stage. */
            String ccomp = suggestContentCompression();
            if (ccomp != null) {
                /* Suitable content compression was found, so apply
                 * it. */
                suffix(contentAvailable.get(ccomp).getKey(), ccomp);
            } else {
                /* Look for suitable transfer compression. */
                String tcomp = suggestTransferCompression();
                if (tcomp != null) {
                    var entry =
                        Map.entry(transferAvailable.get(tcomp).getKey(), tcomp);
                    transferEncodings.add(entry);
                }
            }
        }

        List<String> contentLabels =
            contentEncodings.stream().filter(e -> e.getKey().listed())
                .map(ResponseEncoder::chooseLabel).toList();
        if (!contentLabels.isEmpty()) ctxt.setContentEncoding(contentLabels);

        List<String> transferLabels = transferEncodings.stream()
            .map(ResponseEncoder::chooseLabel).toList();
        if (!transferLabels.isEmpty()) ctxt.setTransferEncoding(transferLabels);

        OutputStream base = ctxt.raw();
        for (var entry : transferEncodings.reversed())
            base = entry.getKey().encode(base);
        for (var entry : contentEncodings.reversed())
            base = entry.getKey().encode(base);
        this.out = base;

        return this.out;
    }
}
