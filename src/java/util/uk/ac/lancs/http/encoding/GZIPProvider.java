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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import uk.ac.lancs.scc.jardeps.Service;

/**
 * Provides GZIP encoding. This goes by the canonical name
 * {@value #NAME} and also {@value #OTHER_NAME}, and is used for both
 * content and transfer encoding.
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
 * {@value #QUALITY_PROP} is a real number in the range [0.0, 1.0], and
 * defaults to {@value #DEFAULT_QUALITY}. It currently does not apply to
 * input.
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

    private static Collection<String> NAMES = Set.of(NAME, OTHER_NAME);

    private static final InputEncoding INPUT_INSTANCE = new InputEncoding() {
        @Override
        public InputStream decode(InputStream in) throws IOException {
            return new GZIPInputStream(in);
        }

        @Override
        public Collection<? extends CharSequence> names() {
            return NAMES;
        }
    };

    @Override
    public InputEncoding getForInput(EncodingContext ctxt, Properties props,
                                     String pfx) {
        return INPUT_INSTANCE;
    }

    @Override
    public OutputEncoding getForOutput(EncodingContext ctxt, Properties props,
                                       String pfx) {
        String ourPfx = pfx + NAME + '.';
        var qual = Utils.getDefault(props, ourPfx, OUTPFX, ctxt, QUALITY_PROP,
                                    DEFAULT_QUALITY, Float::parseFloat);

        return new OutputEncoding() {
            @Override
            public String name() {
                return NAME;
            }

            @Override
            public OutputStream encode(OutputStream out) throws IOException {
                return new GZIPOutputStream(out);
            }

            @Override
            public Number compressionQuality() {
                return qual;
            }

            @Override
            public Collection<? extends CharSequence> names() {
                return NAMES;
            }
        };
    }

}
