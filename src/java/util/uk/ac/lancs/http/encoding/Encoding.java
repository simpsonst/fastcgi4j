// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2022,2023,2026, Lancaster University
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
import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;

/**
 * A named means of encoding an output stream or decoding an input
 * stream
 *
 * @author simpsons
 */
public interface Encoding {
    /**
     * Get the set of names recognized by this encoding. The returned
     * set may be immutable.
     * 
     * @return the set of recognized names, including that returned by
     * {@link #name()}
     */
    Collection<? extends CharSequence> names();

    /**
     * Get the canonical name of this encoding as used in the
     * <samp>Content-Encoding</samp> and <samp>Accept-Encoding</samp>
     * header fields.
     * 
     * @return the encoding name
     * 
     * @see https://www.rfc-editor.org/rfc/rfc9110.html#name-content-coding-registry
     * IANA Content Coding Registry
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
     * 
     * @throws UnsupportedOperationException if encoding is not
     * implemented
     */
    OutputStream encode(OutputStream out) throws IOException;

    /**
     * Determine whether encoding is available.
     * {@link #encode(OutputStream)} will throw an exception if not.
     * 
     * @return {@code true} if encoding is available; {@code false} if
     * not
     */
    boolean encodingAvailable();

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
     * Create a mapping from names to known encoding.
     * 
     * @param loader the loader for locating services of type
     * {@link Encoding}
     * 
     * @return a fresh, modifiable mapping, indexed case-insensitively
     */
    static Map<String, Encoding> getMapping(ClassLoader loader) {
        Map<String, Encoding> result =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (var enc : ServiceLoader.load(Encoding.class, loader))
            for (var name : enc.names())
                result.put(name.toString(), enc);
        return result;
    }

    /**
     * Create a mapping from names to known encoding, using the calling
     * thread's context class loader.
     * 
     * @return a fresh, modifiable mapping, indexed case-insensitively
     */
    static Map<String, Encoding> getMapping() {
        return getMapping(Thread.currentThread().getContextClassLoader());
    }
}
