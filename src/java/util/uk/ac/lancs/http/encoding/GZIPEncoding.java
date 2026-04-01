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

package uk.ac.lancs.http.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import uk.ac.lancs.scc.jardeps.Service;

/**
 * Performs {@value #NAME} encoding and decoding.
 *
 * @author simpsons
 */
@Service(Encoding.class)
public final class GZIPEncoding implements Encoding {
    private GZIPEncoding() {}

    /**
     * The sole instance of this class
     */
    public static final GZIPEncoding INSTANCE = new GZIPEncoding();

    private static final String NAME = "gzip";

    private static final String OTHER_NAME = "x-" + NAME;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public OutputStream encode(OutputStream out) throws IOException {
        return new GZIPOutputStream(out);
    }

    @Override
    public InputStream decode(InputStream in) throws IOException {
        return new GZIPInputStream(in);
    }

    private static final Set<String> nameSet = Set.of(NAME, OTHER_NAME);

    /**
     * {@inheritDoc}
     * 
     * @return an immutable set containing {@value #NAME} and
     * {@value #OTHER_NAME}
     */
    @Override
    public Collection<? extends CharSequence> names() {
        return nameSet;
    }

    @Override
    public boolean encodingAvailable() {
        return true;
    }
}
