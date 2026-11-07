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

package uk.ac.lancs.http;

import java.util.function.BiConsumer;

/**
 * Recognizes the <samp>{@value "%s" #QUALITY_PARAMETER}</samp>
 * parameter as specifying a quality value for a preference.
 */
public class QualitySelector implements BiConsumer<CharSequence, String> {
    private static final float DEFAULT_QUALITY = 1.0f;

    /**
     * Identifies the optional parameter that specifies a quality value
     * for a preference. That parameter is
     * <code class="java">{@value "%s"}</code>.
     */
    public static final String QUALITY_PARAMETER = "q";

    private float quality = DEFAULT_QUALITY;

    /**
     * Reset the quality to the default {@value #DEFAULT_QUALITY}.
     */
    public void reset() {
        quality = DEFAULT_QUALITY;
    }

    /**
     * Get the current quality value.
     *
     * @return the current quality value.
     */
    public float get() {
        return quality;
    }

    /**
     * Recognize the quality parameter <samp>{@value "%s"
     * #QUALITY_PARAMETER}</samp>, and set the quality to the provided
     * value.
     *
     * @param t the token
     *
     * @param u the value
     */
    @Override
    public void accept(CharSequence t, String u) {
        if (QUALITY_PARAMETER.contentEquals(t)) quality = Float.parseFloat(u);
    }

}
