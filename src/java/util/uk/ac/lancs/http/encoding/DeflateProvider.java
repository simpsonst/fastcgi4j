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
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import uk.ac.lancs.scc.jardeps.Service;

/**
 * Provides DEFLATE encoding. This goes by the sole name
 * <samp>{@value "%s" #NAME}</samp>, and is used for both content and
 * transfer encoding.
 * 
 * <p>
 * Properties are recognized with the following forms:
 * 
 * <pre>
 * <var>pfx</var>deflate.in.<var>ctxt</var>.quality;
 * <var>pfx</var>deflate.out.<var>ctxt</var>[.level|.quality];
 * <var>pfx</var>deflate.<var>ctxt</var>[.level|.quality];
 * <var>pfx</var>deflate[.level|.quality];
 * </pre>
 * 
 * <p>
 * <var>ctxt</var> is {@link EncodingContext#key}. This and
 * <var>pfx</var> are given to the provider on invocation.
 * 
 * <p>
 * <samp>{@value "%s" #LEVEL_PROP}</samp> is an integer in the range 0
 * to 9, and defaults to {@value "%d" #DEFAULT_LEVEL}. It does not apply
 * to input.
 * 
 * <p>
 * <samp>{@value "%s" #QUALITY_PROP}</samp> is a real number in the
 * range [0.0, 1.0], and defaults to {@value "%g" #DEFAULT_QUALITY}. It
 * currently does not apply to input.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc1951"
 * title="DEFLATE Compressed Data Format Specification version 1.3">RFC1951</a>
 * 
 * @author simpsons
 */
@Service(EncodingProvider.class)
public class DeflateProvider implements EncodingProvider {
    private static final String OUTPFX = "out";

    private static final String NAME = "deflate";

    private static final String LEVEL_PROP = "level";

    private static final int DEFAULT_LEVEL = 6;

    private static final String QUALITY_PROP = "quality";

    private static final float DEFAULT_QUALITY = 0.7f;

    private static final Collection<String> NAMES = Set.of(NAME);

    private static final Decoder INPUT_INSTANCE = DeflaterInputStream::new;

    @Override
    public void getForInput(Map<? super String, ? super Decoder> into,
                            EncodingContext ctxt, Properties props,
                            CharSequence... pfxs) {
        for (var name : NAMES)
            into.put(name, INPUT_INSTANCE);
    }

    private static Encoder encoder(String name, int lvl) {
        return new Encoder() {
            @Override
            public OutputStream encode(OutputStream out) throws IOException {
                return new DeflaterOutputStream(out, new Deflater(lvl));
            }

            @Override
            public String name() {
                return name;
            }
        };
    }

    private static OutputEncoding encoding(Encoder encoder, int lvl) {
        return new OutputEncoding() {
            @Override
            public float compressionFactor() {
                /* TODO: Make this a function of the level. */
                return 0.04F;
            }

            @Override
            public Encoder encoder() {
                return encoder;
            }
        };
    }

    private static OutputEncoding encoding(String name, int lvl) {
        return encoding(encoder(name, lvl), lvl);
    }

    /**
     * Get a DEFALTE output encoding using the canonical name
     * <samp>{@value "%s" #NAME}</samp>.
     * 
     * <p>
     * This is intended for use with
     * {@link ResponseEncodingControl#force(List)} to force an
     * unnegotiated encoding to be applied.
     * 
     * @param level the compression level, 0-9
     * 
     * @return the requested encoding
     */
    public static OutputEncoding encoding(int level) {
        return encoding(NAME, level);
    }

    /**
     * Get an identity encoding using the canonical name
     * <samp>{@value "%s" #NAME}</samp>.
     * 
     * <p>
     * This is intended for use with
     * {@link ResponseEncodingControl#force(List)} to express encodings
     * that have already been applied.
     * 
     * @param level the compression level, 0-9
     * 
     * @return the requested encoding
     */
    public static OutputEncoding fakeEncoding(int level) {
        return encoding(IdentityProvider.encoder(NAME), level);
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
        var lvl = Utils
            .getDefault(props, LEVEL_PROP, DEFAULT_LEVEL, Integer::parseInt,
                        Utils.multiplyForEncoding(NAME, OUTPFX, ctxt, pfxs));
        for (var name : NAMES)
            into.put(name, Map.entry(encoding(name, lvl), qual));
    }
}
