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

package uk.ac.lancs.fastcgi.augment;

import java.util.Objects;
import uk.ac.lancs.fastcgi.FilterSession;

/**
 * Extracts metadata from a filter session.
 * 
 * @author simpsons
 */
public final class FilterMetadata {
    /**
     * Identifies the request parameter that a filter receives giving
     * the last-modification time of the data. The value is
     * {@value}, and is used by {@link #dataLastModified()}.
     */
    public static final String DATA_LAST_MOD_PARAM = "FCGI_DATA_LAST_MOD";

    /**
     * Identifies the request parameter that a filter receives giving
     * the length of the data in bytes. The value is {@value}, and is
     * used by {@link #dataLength()}.
     */
    public static final String DATA_LENGTH_PARAM = "FCGI_DATA_LENGTH";

    /**
     * Holds the base session context.
     */
    public final FilterSession base;

    /**
     * Access filter metadata from a base session.
     * 
     * @param base the base session
     * 
     * @throws NullPointerException if {@code base} is {@code null}
     */
    public FilterMetadata(FilterSession base) {
        Objects.requireNonNull(base, "base");
        this.base = base;
    }

    /**
     * Get the last-modified time of the data if specified. This is
     * obtained through the request parameter
     * {@value #DATA_LAST_MOD_PARAM}, which is parsed as a non-negative
     * integer.
     * 
     * @return the number of seconds after 1970-01-01T00:00:00Z when the
     * data was last modified
     * 
     * @throws NullPointerException if the parameter is not set
     * 
     * @throws NumberFormatException if the parameter is not a
     * non-negative decimal integer
     */
    public long dataLastModified() {
        var text = base.parameters().get(DATA_LAST_MOD_PARAM);
        if (text == null)
            throw new NullPointerException("missing " + DATA_LAST_MOD_PARAM);
        long rc = Long.parseLong(text, 10);
        if (rc < 0) throw new NumberFormatException("-ve " + DATA_LAST_MOD_PARAM
            + ": " + text);
        return rc;
    }

    /**
     * Get the data length if specified. This is obtained through the
     * request parameter {@value #DATA_LENGTH_PARAM}, which is parsed as
     * a non-negative decimal integer.
     * 
     * @return the data length in bytes
     * 
     * @throws NullPointerException if the parameter is not set
     * 
     * @throws NumberFormatException if the parameter is not a
     * non-negative decimal integer
     */
    public long dataLength() {
        var text = base.parameters().get(DATA_LENGTH_PARAM);
        if (text == null)
            throw new NullPointerException("missing " + DATA_LENGTH_PARAM);
        long rc = Long.parseLong(text, 10);
        if (rc < 0) throw new NumberFormatException("-ve " + DATA_LENGTH_PARAM
            + ": " + text);
        return rc;
    }

}
