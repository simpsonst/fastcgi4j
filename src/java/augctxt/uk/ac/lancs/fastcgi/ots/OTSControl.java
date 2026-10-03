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

package uk.ac.lancs.fastcgi.ots;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import uk.ac.lancs.http.field.FieldNames;
import uk.ac.lancs.mime.MediaType;

/**
 * Controls aspects of the response.
 */
public interface OTSControl {
    /**
     * Set the status code.
     *
     * @param code the new status code
     */
    void setStatus(int code);

    /**
     * Remove the <samp>{@value "%s" FieldNames#CONTENT_TYPE}</samp>
     * field.
     */
    void clearContentType();

    /**
     * Remove the <samp>{@value "%s" FieldNames#CONTENT_LENGTH}</samp>
     * field.
     */
    void clearContentLength();

    /**
     * Remove the <samp>{@value "%s" FieldNames#CONTENT_ENCODING}</samp>
     * field.
     */
    void clearContentEncoding();

    /**
     * Get the output stream.
     *
     * @return the output stream
     *
     * @throws IOException if an I/O errors in setting up the stream
     */
    OutputStream out() throws IOException;

    /**
     * Set the <samp>{@value "%s" FieldNames#LOCATION}</samp> field.
     *
     * @param location the new value for the field
     */
    void setLocation(URI location);

    /**
     * Set the <samp>{@value "%s" FieldNames#CONTENT_TYPE}</samp> field.
     *
     * @param type the new content type
     */
    void setContentType(MediaType type);

}
