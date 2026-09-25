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

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import uk.ac.lancs.http.field.FieldNames;
import uk.ac.lancs.scc.jardeps.Service;

/**
 * Provides identity encoding. This goes by the sole name
 * <samp>{@value "%s" #NAME}</samp>, and is used for both content
 * encoding. It is not listed in <samp>{@value "%s"
 * FieldNames#CONTENT_ENCODING}</samp>. It is also not available for
 * transfer encoding.
 * 
 * <p>
 * Properties are recognized with the following forms:
 * 
 * <pre>
 * <var>pfx</var>identity.<var>ctxt</var>.quality;
 * <var>pfx</var>identity.quality;
 * </pre>
 * 
 * <p>
 * <var>ctxt</var> is {@link EncodingContext#key}. This and
 * <var>pfx</var> are given to the provider on invocation.
 * 
 * <p>
 * <samp>{@value "%s" #QUALITY_PROP}</samp> is a real number in the
 * range [0.0, 1.0], and defaults to {@value "%g" #DEFAULT_QUALITY}. It
 * currently does not apply to input.
 * 
 * @author simpsons
 */
@Service(EncodingProvider.class)
public class IdentityProvider implements EncodingProvider {
    private static final String OUTPFX = "out";

    /**
     * The canonical name of this encoding, namely {@value}
     */
    public static final String NAME = "identity";

    private static final String QUALITY_PROP = "quality";

    private static final float DEFAULT_QUALITY = 0.01f;

    private static final Collection<String> NAMES = Set.of(NAME);

    /**
     * A sole instance of the identity encoding that can be used for all
     * input
     */
    public static final Decoder INPUT_INSTANCE = (in) -> in;

    @Override
    public void getForInput(Map<? super String, ? super Decoder> into,
                            EncodingContext ctxt, Properties props,
                            CharSequence... pfxs) {
        switch (ctxt) {
        case CONTENT:
            for (var name : NAMES)
                into.put(name, INPUT_INSTANCE);
            break;
        }
    }

    /**
     * Get an identity encoder with a given name.
     * 
     * @param name the name, which may be {@code null}
     * 
     * @return the requested encoder
     */
    public static Encoder encoder(CharSequence name) {
        var n = Objects.toString(name, null);
        return new Encoder() {
            @Override
            public OutputStream encode(OutputStream out) {
                return out;
            }

            @Override
            public String name() {
                return n;
            }
        };
    }

    private static OutputEncoding internalEncoding(String name, float factor) {
        var encoder = encoder(name);
        return new OutputEncoding() {
            @Override
            public float compressionFactor() {
                return factor;
            }

            @Override
            public Encoder encoder() {
                return encoder;
            }
        };
    }

    private static final OutputEncoding ANONYMOUS_ENCODING_INSTANCE =
        internalEncoding(null, 1.0F);

    /**
     * Get an identity encoding with a given name and compression
     * factor.
     * 
     * <p>
     * This is intended for use with
     * {@link ResponseEncodingControl#force(List)} for responses that
     * are already encoded with
     * 
     * @param name the declared name of the encoding
     * 
     * @param factor the compression factor
     * 
     * @return the requested encoding
     */
    public static OutputEncoding encoding(CharSequence name, float factor) {
        Objects.requireNonNull(name, "name");
        return internalEncoding(name.toString(), factor);
    }

    /**
     * Get an anonymous identity encoding with a given compression
     * factor.
     * 
     * <p>
     * This is intended for use with
     * {@link ResponseEncodingControl#force(List)} for media types that
     * are already well compressed. An anonymous encoding won't be
     * listed, an identity encoding will not transform the input, and
     * the sub-unit compression factor will suppress further
     * (negotiated) compression, an encoding of which qualities this
     * method can return.
     * 
     * @param factor the compression factor
     * 
     * @return the requested encoding
     */
    public static OutputEncoding encoding(float factor) {
        return internalEncoding(null, factor);
    }

    @Override
    public void getForOutput(
                             Map<? super String,
                                 ? super Map.Entry<? extends OutputEncoding,
                                                   ? extends Number>> into,
                             EncodingContext ctxt, Properties props,
                             CharSequence... pfxs) {
        switch (ctxt) {
        case CONTENT:
            var qual = Utils.getDefault(props, QUALITY_PROP, DEFAULT_QUALITY,
                                        Float::parseFloat,
                                        Utils.multiplyForEncoding(NAME, OUTPFX,
                                                                  ctxt, pfxs));
            for (var name : NAMES)
                into.put(name, Map.entry(ANONYMOUS_ENCODING_INSTANCE, qual));
            break;
        }
    }
}
