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
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.TreeMap;

/**
 *
 * @author simpsons
 */
public interface OutputEncoding extends Encoding {
    /**
     * Get the canonical name of this encoding to be used when no
     * explicit indication is made.
     * 
     * @return the canonical name
     */
    String name();

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
     * Get the compression quality for this encoding.
     * 
     * @return the compression quality in the range [0.0, 1.0]; or
     * {@code null} if encoding is not intended for compression
     */
    Number compressionQuality();

    /**
     * Determine whether this encoding needs to be listed.
     * 
     * @return {@code true} if the encoding must be listed;
     * {@code false} otherwise
     * 
     * @apiNote All encodings should be listed, except the identity
     * encoding.
     * 
     * @implNote The default behaviour is to return {@code true}.
     */
    default boolean listed() {
        return true;
    }

    /**
     * Create a mapping from names to encodings and their qualities for
     * the purpose of encoding, based on configuration properties.
     * 
     * @param ctxt the context for encoding
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
    static Map<String, Map.Entry<OutputEncoding, Number>>
        getMapping(EncodingContext ctxt, Properties props, ClassLoader ldr,
                   CharSequence... pfxs) {
        Map<String, Map.Entry<OutputEncoding, Number>> result =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (var provider : ServiceLoader.load(EncodingProvider.class, ldr)) {
            var encoding = provider.getForOutput(ctxt, props, pfxs);
            if (encoding == null) continue;
            var quality = encoding.compressionQuality();
            if (quality == null) continue;
            var entry = Map.entry(encoding, quality);
            for (var name : encoding.names())
                result.put(name.toString(), entry);
        }
        return result;
    }

    /**
     * Create a mapping from names to encodings and their qualities for
     * the purpose of encoding, based on configuration properties, using
     * the calling thread's context class loader.
     * 
     * @param ctxt the context for encoding
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
    static Map<String, Map.Entry<OutputEncoding, Number>>
        getMapping(EncodingContext ctxt, Properties props,
                   CharSequence... pfxs) {
        return getMapping(ctxt, props,
                          Thread.currentThread().getContextClassLoader(), pfxs);
    }
}
