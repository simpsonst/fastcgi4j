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
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.TreeMap;

/**
 *
 * @author simpsons
 */
public interface InputEncoding extends Encoding {
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
     * Create a mapping from names to encodings for the purpose of
     * decoding.
     * 
     * @param ctxt the context for decoding
     * 
     * @param props a set of properties that providers can read for
     * configuration
     * 
     * @param pfx a prefix of property names that the providers should
     * look under
     * 
     * @param ldr the loader for locating services of type
     * {@link EncodingProvider}
     * 
     * @return the mutable mapping configured by available classes and
     * supplied properties
     */
    static Map<String, InputEncoding> getMapping(EncodingContext ctxt,
                                                 Properties props, String pfx,
                                                 ClassLoader ldr) {
        Map<String, InputEncoding> result =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (var provider : ServiceLoader.load(EncodingProvider.class, ldr)) {
            var encoding = provider.getForInput(ctxt, props, pfx);
            if (encoding == null) continue;
            for (var name : encoding.names())
                result.put(name.toString(), encoding);
        }

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
     * @param pfx a prefix of property names that the providers should
     * look under
     * 
     * @return the mutable mapping configured by available classes and
     * supplied properties
     */
    static Map<String, InputEncoding> getMapping(EncodingContext ctxt,
                                                 Properties props, String pfx) {
        return getMapping(ctxt, props, pfx,
                          Thread.currentThread().getContextClassLoader());
    }
}
