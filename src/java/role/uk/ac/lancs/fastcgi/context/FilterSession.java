// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2022,2023, Lancaster University
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
 *  Author: Steven Simpson <s.simpson@lancaster.ac.uk>
 */

package uk.ac.lancs.fastcgi.context;

import java.io.InputStream;
import uk.ac.lancs.fastcgi.FastCGIParameters;

/**
 * Presents the context of a FastCGI session to an application in the
 * Filter role.
 *
 * @author simpsons
 * 
 * @see <a href=
 * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S6.4">FastCGI
 * Specification &mdash; Filter</a>
 */
public interface FilterSession extends RequestableSession {
    /**
     * Get the stream for reading the file data.
     * 
     * @return the input stream providing the file data
     */
    InputStream data();

    /**
     * Get the last-modified time of the data if specified. This is
     * obtained through the request parameter
     * {@value FastCGIParameters#DATA_LAST_MOD_PARAM}.
     * 
     * @return the number of seconds after 1970-01-01T00:00:00Z when the
     * data was last modified; or {@link #DATA_UNSPECIFIED} if not
     * specified; or {@link #DATA_MALFORMED} if not specified as a
     * non-negative decimal integer
     * 
     * @default The default implementation calls {@link #parameters()}
     * and selects {@link FastCGIRequestParameters#DATA_LAST_MOD_PARAM}.
     * It then parses the result as a decimal integer.
     */
    default long dataLastModified() {
        var text = parameters().get(FastCGIParameters.DATA_LAST_MOD_PARAM);
        if (text == null) return DATA_UNSPECIFIED;
        try {
            long rc = Long.parseLong(text, 10);
            if (rc < 0) return DATA_MALFORMED;
            return rc;
        } catch (NumberFormatException ex) {
            return DATA_MALFORMED;
        }
    }

    /**
     * Get the data length if specified. This is obtained through the
     * request parameter {@value FastCGIParameters#DATA_LENGTH_PARAM}.
     * 
     * @return the data length in bytes; or {@link #DATA_UNSPECIFIED} if
     * not specified; or {@link #DATA_MALFORMED} if not specified as a
     * non-negative decimal integer
     * 
     * @default The default implementation calls {@link #parameters()}
     * and selects {@link FastCGIRequestParameters#DATA_LENGTH_PARAM}.
     * It then parses the result as a decimal integer.
     */
    default long dataLength() {
        var text = parameters().get(FastCGIParameters.DATA_LENGTH_PARAM);
        if (text == null) return DATA_UNSPECIFIED;
        try {
            long rc = Long.parseLong(text, 10);
            if (rc < 0) return DATA_MALFORMED;
            return rc;
        } catch (NumberFormatException ex) {
            return DATA_MALFORMED;
        }
    }

    /**
     * Indicates that the data length was not specified. This constant
     * has a negative value, and is returned by {@link #dataLength()}
     * and {@link #dataLastModified()}.
     */
    long DATA_UNSPECIFIED = -1;

    /**
     * Indicates that the data length was malformed. This constant has a
     * negative value, and is returned by {@link #dataLength()} and
     * {@link #dataLastModified()}.
     */
    long DATA_MALFORMED = -2;
}
