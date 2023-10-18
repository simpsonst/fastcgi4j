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

package uk.ac.lancs.fastcgi.misc;

import java.util.Locale;
import java.util.Map;
import uk.ac.lancs.fastcgi.mime.MediaGroup;
import uk.ac.lancs.fastcgi.mime.MediaType;

/**
 * Provides procedures for negotiating content type, character encoding,
 * locale, compression, etc.
 *
 * @author simpsons
 */
public final class Negotiation {
    /**
     * Test whether a locale is a non-strict subset of another. For
     * example, <samp>en-GB</samp> is a subset of <samp>en</samp>, but
     * not vice versa. Note that this method can serve as a
     * <code>{@linkplain Containator}&lt;Locale, Locale&gt;</code>.
     *
     * @param pref the locale preference
     *
     * @param subj the locale subject
     *
     * @return {@code 4} if the locales are an exact match down to
     * variant;
     *
     * {@code 3} if the locales are an exact match down to script, and
     * the preference does not specify a variant;
     *
     * {@code 2} if the locales are an exact match down to country, and
     * the preference does not specify a script;
     *
     * {@code 1} if the locales are an exact match down to language, and
     * the preference does not specify a country;
     *
     * {@code 0} otherwise
     * 
     * @todo <a href=
     * "https://datatracker.ietf.org/doc/html/rfc4647">RFC4647</a>
     * defines wildcards, which complicates matters considerably.
     */
    private static int contains(Locale pref, Locale subj) {
        final String lang = pref.getLanguage();
        if (!lang.equalsIgnoreCase(subj.getLanguage())) return 0;
        final String country = pref.getCountry();
        if (country.isEmpty()) return subj.getCountry().isEmpty() ? 4 : 1;
        if (!country.equalsIgnoreCase(subj.getCountry())) return 0;
        final String script = pref.getScript();
        if (script.isEmpty()) return subj.getScript().isEmpty() ? 4 : 2;
        if (!script.equalsIgnoreCase(subj.getScript())) return 0;
        final String variant = pref.getVariant();
        if (variant.isEmpty()) return subj.getVariant().isEmpty() ? 4 : 3;
        return variant.equalsIgnoreCase(subj.getVariant()) ? 4 : 0;
    }

    /**
     * Checks whether a subject matches a preference.
     * 
     * @param <P> the preference type
     * 
     * @param <T> the subject type
     * 
     * @todo With a name like this, you'd want to keep it private and
     * nested.
     */
    @FunctionalInterface
    private interface Containator<P, T> {
        /**
         * Check whether a subject matches a preference, and how well.
         * 
         * @param pref the preference
         * 
         * @param subj the subject
         * 
         * @return {@code 0} if the subject does not match the
         * preference; positive if it does, with higher values
         * indicating a more precise match
         */
        int contains(P pref, T subj);
    }

    /**
     * Selects the quality of the most suitable preference from a map,
     * based on an offered subject.
     * 
     * @param <P> the preference type
     * 
     * @param <T> the subject type
     * 
     * @todo With a name like this, you'd want to keep it private and
     * nested.
     */
    @FunctionalInterface
    public interface Qualitator<P, T> {
        /**
         * Get the quality of the most suitable preference.
         * 
         * @param pref the set of preferences
         * 
         * @param subj the sought subject
         * 
         * @return the quality of the preference; or {@code null} if
         * there is no match
         */
        Number quality(Map<? extends P, ? extends Number> pref, T subj);
    }

    /**
     * Get the best subject for a set of preferences. A mapping from
     * preference to quality must be provided, and a subject to match
     * against the available preferences. A function returning a
     *
     * @param <P> the preference type
     *
     * @param <T> the subject type
     *
     * @param containator a function testing whether a preference
     * contains a subject, returning {@code null} if not, or an
     * increasing score for how exact the match is
     *
     * @param pref the set of preferences
     *
     * @param sought the subject
     *
     * @return the most apt quality for the subject; or {@code null} if
     * there is no match
     */
    private static <P, T> Number
        getBestFit(Containator<P, T> containator,
                   Map<? extends P, ? extends Number> pref, T sought) {
        Number best = null;
        int bestFit = 0;
        for (Map.Entry<? extends P, ? extends Number> pe : pref.entrySet()) {
            int fit = containator.contains(pe.getKey(), sought);
            if (fit < 1) continue;
            if (best == null || fit > bestFit) {
                best = pe.getValue();
                bestFit = fit;
            }
        }
        return best;
    }

    /**
     * Make a qualitator from a containator.
     * 
     * @param <P> the preference type
     * 
     * @param <T> the subject type
     * 
     * @param containator a function determining whether an offer
     * matches a map key, and how precisely
     * 
     * @return the required function
     */
    private static <P, T> Qualitator<P, T>
        makeQualitator(Containator<P, T> containator) {
        return (p, o) -> getBestFit(containator, p, o);
    }

    /**
     * Get the best preference from offered subjects.
     * 
     * <p>
     * Subjects and preferences need not have the same type. A
     * preference may indicate a set of subjects, and more than one
     * preference may match the same subject. A {@link Qualitator}
     * function yields the most apt preference, if there is one. For
     * example, two preferences for a media type might be
     * <samp>image/*</samp> and <samp>image/jpeg</samp>. An offered
     * media type of <samp>image/jpeg</samp> matches both, but the
     * latter is more apt.
     * 
     * <p>
     * Preferences are presented with corresponding quality values in
     * the range <samp>0f</samp> to <samp>1f</samp>, with higher
     * qualities indicating greater preference. Offers are similarly
     * presented, with higher qualities indicating greater capability on
     * the part of the provider (e.g., higher fidelity, better
     * compression, etc).
     * 
     * <p>
     * For each offer, a quality is extracted from the preferences that
     * best matches it. An offer is rejected if there is no match. If
     * there is a match, the product of qualities of the best matching
     * preference and the offer yield a score for the offer. An offer
     * with the highest score is returned.
     * 
     * @param <P> the preference type
     * 
     * @param <T> the subject type
     * 
     * @param qualitator a means of extracting the most apt quality for
     * an offer
     * 
     * @param pref the preferences and their qualities
     * 
     * @param offer the offered subjects and their qualities
     * 
     * @return the best offer; or {@code null} if none are suitable
     */
    public static <P, T> T
        resolvePreference(Qualitator<P, T> qualitator,
                          Map<? extends P, ? extends Number> pref,
                          Map<? extends T, ? extends Number> offer) {
        T best = null;
        float bestScore = 0.0F;
        for (Map.Entry<? extends T, ? extends Number> oe : offer.entrySet()) {
            T k = oe.getKey();
            Number pv = qualitator.quality(pref, k);
            if (pv == null) continue;
            float sc = pv.floatValue() * oe.getValue().floatValue();
            if (sc > bestScore) {
                best = k;
                bestScore = sc;
            }
        }
        return best;
    }

    /**
     * Get the best locale for a given set of preferences.
     * 
     * @param pref the preferences and their qualities
     * 
     * @param offer the offered subjects and their qualities
     * 
     * @return the best offer; or {@code null} if none are suitable
     */
    public static Locale
        resolveLocalePreference(Map<? extends Locale, ? extends Number> pref,
                                Map<? extends Locale, ? extends Number> offer) {
        return resolvePreference(makeQualitator(Negotiation::contains), pref,
                                 offer);
    }

    /**
     * Get the best media type for a given set of preferences.
     * 
     * @param pref the preferences and their qualities
     * 
     * @param offer the offered subjects and their qualities
     * 
     * @return the best offer; or {@code null} if none are suitable
     */
    public static MediaType
        resolveMediaTypePreference(Map<? extends MediaGroup,
                                       ? extends Number> pref,
                                   Map<? extends MediaType,
                                       ? extends Number> offer) {
        return resolvePreference(makeQualitator(MediaGroup::contains), pref,
                                 offer);
    }

    private static int atomContains(String pref, String subj) {
        if (pref.equals("*")) return 1;
        return pref.equals(subj) ? 2 : 0;
    }

    /**
     * Get the best string for a given set of preferences. This
     * recognizes <samp>*</samp> as a wildcard.
     * 
     * @param pref the preferences and their qualities
     * 
     * @param offer the offered subjects and their qualities
     * 
     * @return the best offer; or {@code null} if none are suitable
     */
    public static String
        resolveAtomPreference(Map<? extends String, ? extends Number> pref,
                              Map<? extends String, ? extends Number> offer) {
        return resolvePreference(makeQualitator(Negotiation::atomContains),
                                 pref, offer);
    }

    /**
     * Get the best exact string for a given set of preferences.
     * 
     * @param pref the preferences and their qualities
     * 
     * @param offer the offered subjects and their qualities
     * 
     * @return the best offer; or {@code null} if none are suitable
     */
    public static String
        resolveStringPreference(Map<? extends String, ? extends Number> pref,
                                Map<? extends String, ? extends Number> offer) {
        return resolvePreference((p, o) -> p.get(o), pref, offer);
    }

    private Negotiation() {}
}
