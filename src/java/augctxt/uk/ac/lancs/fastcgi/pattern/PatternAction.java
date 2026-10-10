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

package uk.ac.lancs.fastcgi.pattern;

import java.io.IOException;
import java.util.regex.Matcher;
import uk.ac.lancs.fastcgi.SessionException;

/**
 * Submits a matched regular expression to an instance of an
 * application-defined type.
 * 
 * @param <T> the application-defined type
 * 
 * @param <R> the result type of submissions
 * 
 * @author simpsons
 */
@FunctionalInterface
public interface PatternAction<T, R> {
    /**
     * Submit a matching regular expression to an application object.
     * 
     * @param receiver the application object
     * 
     * @param matcher the matched regular expression
     * 
     * @return the result of the submission
     * 
     * @throws InterruptedException if the application was interrupted
     * (usually by the server or the remote client aborting the session)
     * 
     * @throws SessionException if the application is temporarily unable
     * to respond to the request
     * 
     * @throws IOException if an I/O error occurs in processing any of
     * the streams of the session context
     */
    R accept(T receiver, Matcher matcher)
        throws SessionException,
            IOException,
            InterruptedException;
}
