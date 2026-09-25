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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import uk.ac.lancs.scc.jardeps.Service;

/**
 * Provides GZIP encoding. This goes by the canonical name
 * <samp>{@value "%s" #NAME}</samp> and also <samp>{@value "%s"
 * #OTHER_NAME}</samp>, and is used for both content and transfer
 * encoding.
 * 
 * <p>
 * Properties are recognized with the following forms:
 * 
 * <pre>
 * <var>pfx</var>gzip.<var>dir</var>.<var>ctxt</var>.quality;
 * <var>pfx</var>gzip.<var>ctxt</var>.quality;
 * <var>pfx</var>gzip.quality;
 * </pre>
 * 
 * <p>
 * <var>ctxt</var> is {@link EncodingContext#key}. This and
 * <var>pfx</var> are given to the provider on invocation.
 * <var>dir</var> is <samp>in</samp> or <samp>out</samp>.
 * 
 * <p>
 * <samp>{@value "%s" #QUALITY_PROP}</samp> is a real number in the
 * range [0.0, 1.0], and defaults to {@value "%g" #DEFAULT_QUALITY}. It
 * currently does not apply to input.
 * 
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc1952"
 * title="GZIP file format specification version 4.3">RFC1952</a>
 * 
 * @author simpsons
 */
@Service(EncodingProvider.class)
public class GZIPProvider implements EncodingProvider {
    private static final String OUTPFX = "out";

    private static final String NAME = "gzip";

    private static final String OTHER_NAME = "x-" + NAME;

    private static final String QUALITY_PROP = "quality";

    private static final float DEFAULT_QUALITY = 1.0f;

    private static final Collection<String> NAMES = Set.of(NAME, OTHER_NAME);

    private static final Decoder INPUT_INSTANCE = GZIPInputStream::new;

    @Override
    public void getForInput(Map<? super String, ? super Decoder> into,
                            EncodingContext ctxt, Properties props,
                            CharSequence... pfxs) {
        for (var name : NAMES)
            into.put(name, INPUT_INSTANCE);
    }

    private static Encoder encoder(String name) {
        return new Encoder() {
            @Override
            public OutputStream encode(OutputStream out) throws IOException {
                return new GZIPOutputStream(out);
            }

            @Override
            public String name() {
                return name;
            }
        };
    }

    private static OutputEncoding internalEncoding(Encoder encoder) {
        return new OutputEncoding() {
            @Override
            public float compressionFactor() {
                return 0.01F;
            }

            @Override
            public Encoder encoder() {
                return encoder;
            }
        };
    }

    private static OutputEncoding internalEncoding(String name) {
        return internalEncoding(encoder(name));
    }

    private static final OutputEncoding ENCODING_INSTANCE =
        internalEncoding(NAME);

    /**
     * Get a GZIP output encoding using the canonical name
     * <samp>{@value "%s" #NAME}</samp>.
     * 
     * <p>
     * This is intended for use with
     * {@link ResponseEncodingControl#applyPriorContentEncodings(List)}
     * to force an unnegotiated encoding to be applied.
     * 
     * @return the requested encoding
     */
    public static OutputEncoding encoding() {
        return ENCODING_INSTANCE;
    }

    private static final OutputEncoding FAKE_INSTANCE =
        internalEncoding(IdentityProvider.encoder(NAME));

    /**
     * Get an identity encoding using the canonical name
     * <samp>{@value "%s" #NAME}</samp>.
     * 
     * <p>
     * This is intended for use with
     * {@link ResponseEncodingControl#applyPriorContentEncodings(List)}
     * to express encodings that have already been applied.
     * 
     * @return the requested encoding
     */
    public static OutputEncoding fakeEncoding() {
        return FAKE_INSTANCE;
    }

    @Override
    public void getForOutput(
                             Map<? super String,
                                 ? super Map.Entry<? extends OutputEncoding,
                                                   ? extends Number>> into,
                             EncodingContext ctxt, Properties props,
                             CharSequence... pfxs) {
        var qual = Utils
            .getDefault(props, QUALITY_PROP, DEFAULT_QUALITY, Float::parseFloat,
                        Utils.multiplyForEncoding(NAME, OUTPFX, ctxt, pfxs));
        for (var name : NAMES)
            into.put(name, Map.entry(internalEncoding(name), qual));
    }

}
