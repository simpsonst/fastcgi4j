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

import java.util.Properties;
import java.util.function.Function;

/**
 * Provides some internal utilities that encoding providers might need.
 * 
 * @author simpsons
 */
class Utils {
    /**
     * Search for decreasingly specific properties. Properties are
     * sought in this order:
     * 
     * <pre>
     * <var>pfx</var><var>dir</var>.<var>ctxt</var>.<var>sfx</var>
     * <var>pfx</var><var>ctxt</var>.<var>sfx</var>
     * <var>pfx</var><var>sfx</var>
     * </pre>
     * 
     * <p>
     * <var>ctxt</var> is the value of {@link EncodingContext#key}.
     * 
     * @param props the properties to search in
     * 
     * @param pfx a common prefix for all properties
     * 
     * @param dir a direction of encoding, usually <samp>in</samp> or
     * <samp>out</samp>
     * 
     * @param ctxt the encoding context
     * 
     * @param sfx a common suffix for all properties
     * 
     * @return the most specific property's value; or {@code null} if
     * none present
     */
    static String getText(Properties props, String pfx, String dir,
                          EncodingContext ctxt, String sfx) {
        String val;
        val = props.getProperty(pfx + dir + '.' + ctxt.key + '.' + sfx);
        if (val != null) return val;
        val = props.getProperty(pfx + ctxt.key + '.' + sfx);
        if (val != null) return val;
        return props.getProperty(pfx + sfx);
    }

    /**
     * Search for decreasingly specific properties, and convert the
     * value of the most specific. This takes the result of
     * {@link #getText(Properties, String, String, EncodingContext, String)},
     * and converts it with the supplied function. If no matching
     * property is present, a default value is returned.
     * 
     * @param <T> the result type
     * 
     * @param props the properties to search in
     * 
     * @param pfx a common prefix for all properties
     * 
     * @param dir a direction of encoding, usually <samp>in</samp> or
     * <samp>out</samp>
     * 
     * @param ctxt the encoding context
     * 
     * @param sfx a common suffix for all properties
     * 
     * @param dfl the default value
     * 
     * @param conv a function converting from string to the required
     * type
     * 
     * @return the converted value of the most specific property; or the
     * provided default value
     */
    static <T> T getDefault(Properties props, String pfx, String dir,
                            EncodingContext ctxt, String sfx, T dfl,
                            Function<? super String, T> conv) {
        String text = getText(props, pfx, dir, ctxt, sfx);
        if (text == null) return dfl;
        return conv.apply(text);
    }

    private Utils() {}
}
