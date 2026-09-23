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

import java.util.Map;
import uk.ac.lancs.http.field.FieldNames;

/**
 * Allows an application to specify content encoding compatible with
 * client preferences.
 *
 * @author simpsons
 */
public interface ResponseEncodingControl {
    /**
     * Specify the content encoding implementations available. The
     * description is indexed by name as used in <samp>{@value "%s"
     * FieldNames#ACCEPT_ENCODING}</samp> and <samp>{@value "%s"
     * FieldNames#CONTENT_ENCODING}</samp> fields. Each name maps to a
     * pair of values: the encoding implementation, and the quality of
     * the encoding (akin to source-quality as defined by <a href=
     * "https://www.rfc-editor.org/info/rfc2295/#section-5.3">RFC2295</a>.
     *
     * <p>
     * By default, an empty map is assumed.
     *
     * @param offer a description of available content encodings
     */
    void offerContentEncodings(Map<? extends String,
                                   ? extends Map.Entry<? extends OutputEncoding,
                                                       ? extends Number>> offer);

    /**
     * Set the compression factor threshold. The default is
     * {@value ResponseEncodingPlanner#DEFAULT_COMPRESSION_FACTOR_THRESHOLD}.
     *
     * @param threshold the new threshold
     *
     * @throws IllegalArgumentException if the threshold is outside the
     * range [0, 1]
     */
    void setCompressionFactorThreshold(float threshold);

    /**
     * Set the initial compression fraction. The default is
     * {@value ResponseEncodingPlanner#DEFAULT_COMPRESSION_FRACTION}.
     *
     * @param c the new initial compression fraction
     *
     * @throws IllegalArgumentException if the compression fraction is
     * outside the range [0, 1]
     */
    void setInitialCompressionFraction(float c);
}
