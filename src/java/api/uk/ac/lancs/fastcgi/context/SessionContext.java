/*
 * Copyright (c) 2022, Lancaster University
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

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

/**
 * Presents the context of a FastCGI session to an application.
 * 
 * @author simpsons
 */
public interface SessionContext {
    /**
     * Complete the request. The exit code should be 0 to indicate a
     * normal exit.
     * 
     * @param exitCode the exit code
     * 
     * @throws IllegalArgumentException if the exit code is negative;
     * these are reserved for the library
     */
    void exit(int exitCode);

    /**
     * Get diagnostics for this session. Applications should not
     * normally need this, except to annotate diagnostic messages so
     * they can be cross-referenced with other system-/library-generated
     * messages.
     * 
     * @return the session diagnostics
     */
    Diagnostics diagnostics();

    /**
     * Set a response header field, replacing any existing values.
     * Leading and trailing spaces are trimmed from the name.
     * 
     * @param name the header field name
     * 
     * @param value the new value
     * 
     * @throws IllegalArgumentException if the field name is
     * {@value #STATUS_FIELD}
     */
    void setHeader(String name, String value);

    /**
     * Specifies the header field name used to set the HTTP status code.
     * The value is {@value}.
     */
    String STATUS_FIELD = "Status";

    /**
     * Add a response header field, retaining earlier values as distinct
     * fields. Leading and trailing spaces are trimmed from the name.
     * 
     * @param name the header name
     * 
     * @param value the additional value
     * 
     * @throws IllegalArgumentException if the field name is
     * {@value #STATUS_FIELD}
     */
    void addHeader(String name, String value);

    /**
     * Set the response status. The default is 200.
     * 
     * @param code the new response status
     * 
     * @throws IllegalArgumentException if the status code is negative
     */
    void setStatus(int code);

    /**
     * Get the CGI environment parameters.
     * 
     * @return an immutable set of parameters
     */
    Map<String, String> parameters();

    /**
     * Get the stream for writing the response.
     * 
     * @return the response body stream
     */
    OutputStream out();

    /**
     * Try to set the buffer size for writing the response. This cannot
     * be set once output has started to be written. It might also be
     * truncated to an implementation-defined limit. In these cases,
     * rather than an exception, the return value indicates failure, as
     * it usually does not constitute a functional failure. An exception
     * is thrown if the argument is invalid.
     * 
     * @param amount the buffer size in bytes; 0 to disable buffering
     * 
     * @return {@code true} if the buffer size was set to the requested
     * value; {@code false} otherwise
     * 
     * @throws IllegalArgumentException if the size is negative
     */
    boolean setBufferSize(int amount);

    /**
     * Get the stream for writing error messages.
     * 
     * @return the error stream
     */
    PrintStream err();
}
