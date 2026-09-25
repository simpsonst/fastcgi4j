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
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.TreeMap;

/**
 * Decodes input streams. Input encodings are best loaded via
 * {@link EncodingProvider} implementations using one of the following
 * static methods:
 * 
 * <ul>
 * 
 * <li>{@link Decoder#getMapping(ClassLoader, EncodingContext, Properties, CharSequence[])}
 * 
 * <li>{@link Decoder#getMapping(EncodingContext, Properties, CharSequence[])}
 * 
 * </ul>
 * 
 * @author simpsons
 */
public interface Decoder {
    /**
     * Wrap a decoder around a stream.
     * 
     * @param in the encoded stream
     * 
     * @return the decoded stream
     * 
     * @throws IOException if an I/O error occurs in creating the new
     * stream
     */
    InputStream decode(InputStream in) throws IOException;

    /**
     * De-apply a sequence of encodings to a stream.
     * 
     * @param in the stream to be decoded
     * 
     * @param encodings the sequence of encodings to de-apply
     * 
     * @return the decoded stream
     * 
     * @throws IOException if an I/O error occurs in de-applying an
     * encoding
     */
    static InputStream decode(InputStream in, List<? extends Decoder> encodings)
        throws IOException {
        for (var enc : encodings)
            in = enc.decode(in);
        return in;
    }

    /**
     * Create a mapping from names to encodings for the purpose of
     * decoding.
     * 
     * @param ctxt the context for decoding
     * 
     * @param props a set of properties that providers can read for
     * configuration
     * 
     * @param pfxs a sequence of prefixes of property names that the
     * providers should look under
     * 
     * @param ldr the loader for locating services of type
     * {@link EncodingProvider}
     * 
     * @return the mutable mapping configured by available classes and
     * supplied properties
     */
    static Map<String, Decoder>
        getMapping(ClassLoader ldr, EncodingContext ctxt, Properties props,
                   CharSequence... pfxs) {
        Map<String, Decoder> result =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (var provider : ServiceLoader.load(EncodingProvider.class, ldr))
            provider.getForInput(result, ctxt, props, pfxs);

        return result;
    }

    /**
     * Create a mapping from names to encodings for the purpose of
     * decoding, using the calling thread's context class loader.
     * 
     * @param ctxt the context for decoding
     * 
     * @param props a set of properties that providers can read for
     * configuration
     * 
     * @param pfxs a sequence of prefixes of property names that the
     * providers should look under
     * 
     * @return the mutable mapping configured by available classes and
     * supplied properties
     */
    static Map<String, Decoder> getMapping(EncodingContext ctxt,
                                           Properties props,
                                           CharSequence... pfxs) {
        return getMapping(Thread.currentThread().getContextClassLoader(), ctxt,
                          props, pfxs);
    }
}
