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

package uk.ac.lancs.fastcgi.augment;

import java.util.Collections;
import java.util.Map;
import uk.ac.lancs.http.encoding.Decoder;
import uk.ac.lancs.http.encoding.OutputEncoding;

/**
 * Provides re-usable context to HTTP responder sessions.
 * 
 * @author simpsons
 */
public interface HttpResponderContext {
    /**
     * Get the set of content encodings for requests, indexed by
     * case-insensitive name. {@link HttpResponderSession#in()} and
     * {@link HttpResponderSession#requestEncodings()} use this to
     * remove content encodings from the request body.
     * 
     * @return an immutable map from decoder name to implementation
     * 
     * @implNote By default, an empty map is returned.
     */
    default Map<String, Decoder> contentDecoders() {
        return Collections.emptyMap();
    }

    /**
     * Get the set of transfer encodings for requests, indexed by
     * case-insensitive name. {@link HttpResponderSession#in()} uses
     * this to remove transfer encodings from the request body.
     * 
     * @return an immutable map from decoder name to implementation
     * 
     * @implNote By default, all available transfer encodings are
     * returned. These can be configured by setting system properties
     * beginning with the prefix <samp>{@value "%s"
     * HttpResponderSession#ENCODINGS_PREFIX}</samp>.
     */
    default Map<String, Decoder> transferDecoders() {
        return HttpResponderSession.ALL_AVAILABLE_TRANSFER_DECODERS;
    }

    /**
     * Get the set of transfer encodings for responses, indexed by
     * case-sensitive name. {@link HttpResponderSession#out()} uses this
     * to determine which transfer encodings should be applied to the
     * response body.
     * 
     * @return an immutable map from encoder name to implementation and
     * source-quality
     * 
     * @implNote By default, all available transfer encodings are
     * returned. These can be configured by setting system properties
     * beginning with the prefix <samp>{@value "%s"
     * HttpResponderSession#ENCODINGS_PREFIX}</samp>.
     */
    default Map<String, Map.Entry<? extends OutputEncoding, ? extends Number>>
        transferEncoders() {
        return HttpResponderSession.ALL_AVAILABLE_TRANSFER_ENCODERS;
    }

    /**
     * Get the set of content encodings for responses, indexed by
     * case-sensitive name. {@link HttpResponderSession#out()} uses this
     * to determine which content encodings should be applied to the
     * response body.
     * 
     * @apiNote Applications are recommended not to override this method
     * (so the default is that no content encoding is applied), but to
     * configure encoding on a per-request basis through
     * {@link HttpResponderSession#encodingControl()}.
     * 
     * @return an immutable map from encoder name to implementation and
     * source-quality
     * 
     * @implNote By default, an empty map is returned.
     */
    default Map<String, Map.Entry<? extends OutputEncoding, ? extends Number>>
        contentEncoders() {
        return Collections.emptyMap();
    }
}
