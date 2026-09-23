// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2026, Lancaster University
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import uk.ac.lancs.http.Negotiation;
import uk.ac.lancs.http.field.FieldNames;

/**
 * Works out how to encode a response body, based on encoding
 * availability and on client preference.
 * 
 * <p>
 * When a fresh planner is created, several inputs can be specified, or
 * left as defaults. Setting any of these inputs invalidates the
 * planner, which also starts in an invalid state. The inputs are
 * grouped into three parts:
 * 
 * <ul>
 * 
 * <li>
 * <p>
 * {@link #offerContentEncodings(Map)} and
 * {@link #preferContentEncodings(Map)} determine the content encodings
 * to be used.
 * 
 * <li>
 * <p>
 * {@link #offerTransferEncodings(Map)} and
 * {@link #preferTransferEncodings(Map)} determine the transfer
 * encodings. However, these might also be influenced by content
 * encodings, which can make some transfer encodings superfluous.
 * 
 * <li>
 * <p>
 * {@link #setInitialCompressionFraction(float)} and
 * {@link #setCompressionFactorThreshold(float)} help to prevent
 * multiple compression encodings from being applied. As encodings are
 * decided upon, the degree of compression is tracked, and further
 * compression can then be decided against. These methods set the
 * initial state of this tracking, and control the sensitivity to
 * further compression.
 * 
 * <p>
 * Each {@linkplain OutputEncoding output encoding} specifies the
 * {@linkplain OutputEncoding#compressionFactor() compression factor} of
 * a stream after being applied to it. Another encoding should not be
 * applied if it has a higher value, and indeed it must be significantly
 * lower to be worthwhile. Dividing the difference between the new value
 * and the old by the old value gives a compression factor in the range
 * [0, 1]. If this would be below a threshold, the additional encoding
 * is not considered worthwhile. The default threshold is
 * {@value #DEFAULT_COMPRESSION_FACTOR_THRESHOLD}, and it can be set
 * with {@link #setCompressionFactorThreshold(float)}.
 * 
 * <p>
 * If the application is providing content that is already well
 * compressed, it should set the initial compression factor to a lower
 * value to suppress further compression. The default is
 * {@value #DEFAULT_COMPRESSION_FRACTION}, and it can be set with
 * {@link #setInitialCompressionFraction(float)}.
 * 
 * </ul>
 * 
 * <p>
 * {@link #offerContentEncodings(Map)} and
 * {@link #offerTransferEncodings(Map)} should be determined by the
 * application's own configuration. They describe what encoding
 * implementations are available, and which are preferred. In contrast,
 * {@link #preferContentEncodings(Map)} and
 * {@link #preferTransferEncodings(Map)} should be derived from the
 * client's preferences, typically the <samp>{@value "%s"
 * FieldNames#ACCEPT_ENCODING}</samp> and <samp>{@value "%s"
 * FieldNames#TE}</samp> header fields.
 * 
 * <p>
 * When the planner resolves its inputs, it goes into a valid state.
 * Calling {@link #transferPlan()} or {@link #contentPlan()} ensures
 * that the planner is in a valid state, and returns part of that state.
 * 
 * @author simpsons
 */
public class ResponseEncodingPlanner implements ResponseEncodingControl {
    private Map<? extends String,
                ? extends Map.Entry<? extends OutputEncoding,
                                    ? extends Number>> contentOffer =
                                        Collections.emptyMap();

    private Map<? extends String,
                ? extends Map.Entry<? extends OutputEncoding,
                                    ? extends Number>> transferOffer =
                                        Collections.emptyMap();

    private Map<? extends String, ? extends Number> contentPreference =
        Collections.emptyMap();

    private Map<? extends String, ? extends Number> transferPreference =
        Collections.emptyMap();

    /**
     * The default threshold for determining whether to apply another
     * level of compression, namely <code>{@value}</code>, overridden by
     * {@link #setCompressionFactorThreshold(float)
     */
    public static final float DEFAULT_COMPRESSION_FACTOR_THRESHOLD = 0.7f;

    private float compressionFactorThreshold =
        DEFAULT_COMPRESSION_FACTOR_THRESHOLD;

    /**
     * The default initial compression fraction, namely
     * <code>{@value}</code>, overridden by
     * {@link #setInitialCompressionFraction(float)
     */
    public static final float DEFAULT_COMPRESSION_FRACTION = 1.0f;

    private float initialCompressionFraction = DEFAULT_COMPRESSION_FRACTION;

    private void invalidate() {
        transferPlan = contentPlan = null;
    }

    @Override
    public void setCompressionFactorThreshold(float threshold) {
        if (threshold < 0.0f || threshold > 1.0f)
            throw new IllegalArgumentException("compression factor threshold "
                + threshold + " outside [0,1]");
        compressionFactorThreshold = threshold;
        invalidate();
    }

    /**
     * Determine whether further theoretical compression is worthwhile.
     * 
     * @param currentValue the current compression fraction
     * 
     * @param newValue the new compression fraction of a proposed
     * encoding
     * 
     * @param factorThreshold the minimum fraction of the compression
     * fraction that it must be reduced by to make further compression
     * worthwhile
     * 
     * @return {@code true} if the extra compression is worthwhile;
     * {@code false} otherwise
     */
    private static boolean worthCompressing(float currentValue, float newValue,
                                            float factorThreshold) {
        return (currentValue - newValue) / currentValue >= factorThreshold;
    }

    /**
     * Determine whether applying another encoding is worthwhile.
     * 
     * @param currentValue the current compression fraction
     * 
     * @param enc the proposed encoding
     * 
     * @return {@code true} if the extra compression provided by the
     * encoding is worthwhile; {@code false} otherwise
     */
    private boolean worthCompressing(float currentValue, OutputEncoding enc) {
        return worthCompressing(currentValue, enc.compressionFactor(),
                                compressionFactorThreshold);
    }

    @Override
    public void setInitialCompressionFraction(float c) {
        if (c < 0.0f || c > 1.0f)
            throw new IllegalArgumentException("compression fraction " + c
                + " outside [0,1]");
        this.initialCompressionFraction = c;
        invalidate();
    }

    /**
     * Specify the transfer encoding implementations available. The
     * description is indexed by name as used in <samp>{@value "%s"
     * FieldNames#TE}</samp> and <samp>{@value "%s"
     * FieldNames#TRANSFER_ENCODING}</samp> fields. Each name maps to a
     * pair of values: the encoding implementation, and the quality of
     * the encoding (akin to source-quality as defined by <a href=
     * "https://www.rfc-editor.org/info/rfc2295/#section-5.3">RFC2295</a>.
     * 
     * <p>
     * By default, an empty map is assumed.
     * 
     * @param offer a description of available transfer encodings
     */
    public void
        offerTransferEncodings(Map<? extends String,
                                   ? extends Map.Entry<? extends OutputEncoding,
                                                       ? extends Number>> offer) {
        this.transferOffer = offer;
        invalidate();
    }

    @Override
    public void
        offerContentEncodings(Map<? extends String,
                                  ? extends Map.Entry<? extends OutputEncoding,
                                                      ? extends Number>> offer) {
        this.contentOffer = offer;
        invalidate();
    }

    /**
     * Specify the client's preferences for transfer encoding. Each
     * preference is indexed by encoding name as used in
     * <samp>{@value "%s" FieldNames#TE}</samp> and <samp>{@value "%s"
     * FieldNames#TRANSFER_ENCODING}</samp> fields. Each name maps to
     * the client's quality preference for that encoding.
     * 
     * <p>
     * By default, an empty map is assumed.
     * 
     * @param preference a description of the client's transfer-encoding
     * preference
     */
    public void preferTransferEncodings(Map<? extends String,
                                            ? extends Number> preference) {
        this.transferPreference = preference;
        invalidate();
    }

    /**
     * Specify the client's preferences for content encoding. Each
     * preference is indexed by encoding name as used in
     * <samp>{@value "%s" FieldNames#ACCEPT_ENCODING}</samp> and
     * <samp>{@value "%s" FieldNames#CONTENT_ENCODING}</samp> fields.
     * Each name maps to the client's quality preference for that
     * encoding.
     * 
     * <p>
     * By default, an empty map is assumed.
     * 
     * @param preference a description of the client's content-encoding
     * preference
     */
    public void preferContentEncodings(Map<? extends String,
                                           ? extends Number> preference) {
        this.contentPreference = preference;
        invalidate();
    }

    /**
     * Compare offered encodings against preferred ones, and yield an
     * sequence of compatible encoding names in decreasing order of
     * suitability.
     * 
     * @param offer a description of encoding implementations available
     * 
     * @param preference a description of a client's preferences
     * 
     * @return a list of decreasingly suitable encoding names
     */
    private static List<String>
        listCandidates(Map<? extends String,
                           ? extends Map.Entry<? extends OutputEncoding,
                                               ? extends Number>> offer,
                       Map<? extends String, ? extends Number> preference) {
        var offerx = offer.entrySet().stream()
            .collect(Collectors
                .toMap(Map.Entry::getKey,
                       e -> e.getValue().getValue().floatValue()));
        return Negotiation.resolveAtomPreferences(preference, offerx);
    }

    /**
     * Apply a candidate encoding if compression is worthwhile.
     * 
     * @param cand the name of the candidate encoding
     * 
     * @param fraction the current compression fraction
     * 
     * @param plan a record of encodings to be appended to if the
     * encoding is applied
     * 
     * @param offer a description of encoding implementations available,
     * used only to map the encoding name to an implementation
     * 
     * @return the new compression fraction, which should be no larger
     * than the input value
     */
    private float
        applyCandidate(String cand, float fraction,
                       List<Map.Entry<String, OutputEncoding>> plan,
                       Map<? extends String,
                           ? extends Map.Entry<? extends OutputEncoding,
                                               ? extends Number>> offer) {
        var enc = offer.get(cand).getKey();
        if (!worthCompressing(fraction, enc)) return fraction;
        plan.add(Map.entry(cand, enc));
        assert fraction >= enc.compressionFactor();
        return enc.compressionFactor();
    }

    /**
     * If no plan is already calculated, calculate a new one from the
     * current settings.
     */
    private void resolve() {
        if (transferPlan != null) return;

        /* Keep track of how compressible the stream is after each
         * encoding. */
        var factor = initialCompressionFraction;

        List<Map.Entry<String, OutputEncoding>> contentPlan = new ArrayList<>();
        List<Map.Entry<String, OutputEncoding>> transferPlan =
            new ArrayList<>();

        /* Work out what content encoding is required. */
        List<String> contentCands =
            listCandidates(contentOffer, contentPreference);
        for (String cand : contentCands)
            factor = applyCandidate(cand, factor, contentPlan, contentOffer);

        /* Work out what transfer encoding is required. */
        List<String> transferCands =
            listCandidates(transferOffer, transferPreference);
        for (String cand : transferCands)
            factor = applyCandidate(cand, factor, transferPlan, transferOffer);

        this.contentPlan = new EncodingPlan(contentPlan);
        this.transferPlan = new EncodingPlan(transferPlan);
    }

    private EncodingPlan transferPlan = null;

    private EncodingPlan contentPlan = null;

    /**
     * Get the transfer-encoding plan. If the plan is invalidated, a new
     * one is calculated.
     * 
     * @return the requested plan
     */
    public EncodingPlan transferPlan() {
        resolve();
        return transferPlan;
    }

    /**
     * Get the content-encoding plan. If the plan is invalidated, a new
     * one is calculated.
     * 
     * @return the requested plan
     */
    public EncodingPlan contentPlan() {
        resolve();
        return contentPlan;
    }
}
