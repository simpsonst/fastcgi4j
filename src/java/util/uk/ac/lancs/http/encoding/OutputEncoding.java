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

import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.TreeMap;

/**
 * Provides an encoding and its impact on compression. Output encodings
 * are best loaded via {@link EncodingProvider} implementations using
 * one of the following methods:
 * 
 * <ul>
 * 
 * <li>{@link OutputEncoding#getMapping(ClassLoader, EncodingContext, Properties, CharSequence[])}
 * 
 * <li>{@link OutputEncoding#getMapping(EncodingContext, Properties, CharSequence[])}
 * 
 * </ul>
 *
 * @author simpsons
 */
public interface OutputEncoding {
    /**
     * Get an encoder for this encoding.
     * 
     * @return the requested encoder
     */
    Encoder encoder();

    /**
     * Get a measure of how compressed an uncompressed stream is after
     * this encoding has been applied. This should indicate how
     * worthwhile it would be to apply another encoding. The highest
     * value, 1.0, indicates that the output is as compressible as the
     * input.
     * 
     * @return the ratio of the typical sizes of a stream after versus
     * before encoding, in the range [0.0, 1.0]
     */
    float compressionFactor();

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
    static Map<String, Map.Entry<? extends OutputEncoding, ? extends Number>>
        getMapping(ClassLoader ldr, EncodingContext ctxt, Properties props,
                   CharSequence... pfxs) {
        Map<String,
            Map.Entry<? extends OutputEncoding, ? extends Number>> result =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (var provider : ServiceLoader.load(EncodingProvider.class, ldr))
            provider.getForOutput(result, ctxt, props, pfxs);
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
    static Map<String, Map.Entry<? extends OutputEncoding, ? extends Number>>
        getMapping(EncodingContext ctxt, Properties props,
                   CharSequence... pfxs) {
        return getMapping(Thread.currentThread().getContextClassLoader(), ctxt,
                          props, pfxs);
    }
}
