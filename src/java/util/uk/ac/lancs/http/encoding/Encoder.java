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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import uk.ac.lancs.http.field.FieldNames;

/**
 * Provides a named means of encoding output streams. The name is used
 * to populate fields such as <samp>{@value "%s"
 * FieldNames#CONTENT_ENCODING}</samp> and <samp>{@value "%s"
 * FieldNames#TRANSFER_ENCODING}</samp>. As it might be inappropriate to
 * list some encodings (such as the identity), a name need not be
 * provided.
 * 
 * @author simpsons
 */
public interface Encoder {
    /**
     * Wrap an encoder around a stream.
     *
     * @param out the stream that encoded data will be written to
     *
     * @return a stream that unencoded data can be written to, causing
     * it to be encoded and written to the provided stream
     *
     * @throws IOException if an I/O error occurs in creating the new
     * stream
     */
    OutputStream encode(OutputStream out) throws IOException;

    /**
     * Get the name of that this encoding should be listed as.
     *
     * @return the declared name; or {@code null} if not to be listed
     */
    String name();

    /**
     * Encode a stream using according to a plan. The supplied stream is
     * wrapped by the {@link Encoder#encode(OutputStream)} of each
     * encoding in the plan, in reverse order, so that the first one in
     * the plan will be applied first to bytes written to the result.
     * 
     * @param out the stream that will carry the encoded data
     * 
     * @param encodings the encodings to be applied
     * 
     * @return the unencoded stream
     * 
     * @throws IOException if an I/O error occurs in applying an
     * encoding
     */
    public static OutputStream encode(OutputStream out,
                                      List<? extends Encoder> encodings)
        throws IOException {
        for (var ent : encodings.reversed())
            out = ent.encode(out);
        return out;
    }

    /**
     * Get a declaration of the encoding order. Encodings whose
     * {@link Encoder#name()} methods return {@code null} are not
     * included. The order is the same as that which should go in a
     * <samp>{@value "%s" FieldNames#CONTENT_ENCODING}</samp> or
     * <samp>{@value "%s" FieldNames#TRANSFER_ENCODING}</samp> header
     * field.
     * 
     * @param encodings the encodings to be applied
     * 
     * @return the names of encodings that would be applied to a stream
     * submitted to {@link #encode(OutputStream, List)}
     */
    public static List<String> declare(List<? extends Encoder> encodings) {
        return encodings.stream().map(Encoder::name).filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
