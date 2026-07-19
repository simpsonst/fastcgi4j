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
import java.util.List;
import java.util.function.Function;

/**
 * Decodes input streams according to a mapping from token to encoding.
 * Given a list of encodings that have been applied, the decoder
 * recognizes the last one and decodes the stream accordingly, also
 * removing the encoding token from the end of the list. It continues
 * until it encounters an unknown encoding, or all encodings have been
 * removed.
 *
 * @author simpsons
 */
public final class Decoder {
    private final Function<? super String, ? extends Encoding> mapping;

    /**
     * Create a decoder. The supplied mapping must yield {@code null} if
     * passed an encoding token it does not recognize.
     * 
     * @param mapping the mapping from token to encoding
     */
    public Decoder(Function<? super String, ? extends Encoding> mapping) {
        this.mapping = mapping;
    }

    /**
     * Decode a stream. A mutable list of tokens specifies the encodings
     * that have been applied to the stream. This method repeatedly
     * decodes the stream according to the last encoding token in the
     * list, and then removes that token from the list. It stops when
     * the list is exhausted, or the last token is not recognized by the
     * configured mapping.
     * 
     * <p>
     * On exit, if all encodings were recognized by the configured
     * mapping, the list will be empty. Otherwise, the list ends with an
     * encoding that was not recognized, and may contain recognizable
     * encodings that were not reached.
     * 
     * @param in the source stream
     * 
     * @param encodings a mutable list of the encodings applied to the
     * bytes of the stream; modified by removing a tail of recognized
     * encodings
     * 
     * @return the decoded stream
     * 
     * @throws IOException if an I/O error occurs in de-applying an
     * encoding
     */
    public InputStream decode(InputStream in, List<String> encodings)
        throws IOException {
        int sz = encodings.size();
        while (sz > 0) {
            var last = encodings.remove(--sz);
            var enc = mapping.apply(last);
            if (enc == null) return in;
            in = enc.decode(in);
        }

        return in;
    }
}