// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2024, Lancaster University
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

package uk.ac.lancs.fastcgi.misc;

import java.io.IOException;
import java.nio.charset.Charset;
import uk.ac.lancs.fastcgi.context.RequestableSession;
import uk.ac.lancs.fastcgi.mime.MessageParser;

/**
 * Retains context for parsing form submissions.
 * 
 * @author simpsons
 */
public final class FormHandler {
    private final Charset assumedCharset;

    private final MessageParser parser;

    /**
     * Create a handler that records the message parser and default
     * character encoding for parsing form submissions.
     * 
     * @param parser the parser for <samp>multipart/form-data</samp>
     * submissions
     * 
     * @param assumedCharset the character encoding to assume for
     * percent-encoded submissions
     */
    public FormHandler(MessageParser parser, Charset assumedCharset) {
        this.assumedCharset = assumedCharset;
        this.parser = parser;
    }

    /**
     * Get the form submission from a session. This simply calls
     * {@link FormSubmission#fromSession(RequestableSession, Charset, MessageParser)}
     * using the parameters provided during construction.
     * 
     * @param session the request session providing the method, query
     * string and request body
     * 
     * @return the submitted form field values; or {@code null} if no
     * form delivery mechanism was recognized
     * 
     * @throws IOException if an I/O error occurs in reading the request
     * body or storing any bodies
     */
    public FormSubmission get(RequestableSession session) throws IOException {
        return FormSubmission.fromSession(session, assumedCharset, parser);
    }
}
