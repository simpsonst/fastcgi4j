/*
 * Copyright (c) 2022, Regents of the University of Lancaster
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

package uk.ac.lancs.fastcgi.proto.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Accepts deserialized FastCGI records.
 * 
 * @author simpsons
 */
public interface RecordHandler {
    /**
     * Inform of a request for a set of application variables.
     * 
     * @param id the request id, which should always be zero
     * 
     * @param names the names of the requested variables
     */
    void getValues(Collection<? extends String> names) throws IOException;

    /**
     * Begin a request.
     * 
     * @param id the request id
     * 
     * @param role the role of the application in serving this request
     */
    void beginRequest(int id, int role, int flags) throws IOException;

    /**
     * Abort a request.
     * 
     * @param id the request id
     */
    void abortRequest(int id) throws IOException;

    /**
     * Receive a stream of parameter data. The stream must be consumed
     * before returning.
     * 
     * @param id the request id
     * 
     * @param len the number of bytes
     * 
     * @param in a stream of the bytes
     */
    void params(int id, int len, InputStream in) throws IOException;

    /**
     * Indicate that parameter data is complete.
     * 
     * @param id the request id
     */
    void paramsEnd(int id) throws IOException;

    /**
     * Receive a stream of standard-input data. The stream must be
     * consumed before returning.
     * 
     * @param id the request id
     * 
     * @param len the number of bytes
     * 
     * @param in a stream of the bytes
     */
    void stdin(int id, int len, InputStream in) throws IOException;

    /**
     * Indicate that standard input is complete.
     * 
     * @param id the request id
     */
    void stdinEnd(int id) throws IOException;

    /**
     * Receive a stream of extra data. The stream must be consumed
     * before returning.
     * 
     * @param id the request id
     * 
     * @param len the number of bytes
     * 
     * @param in a stream of the bytes
     */
    void data(int id, int len, InputStream in) throws IOException;

    /**
     * Indicate that extra data is complete.
     * 
     * @param id the request id
     */
    void dataEnd(int id) throws IOException;

    /**
     * Report a bad record.
     * 
     * @param reasons flags identifying the reasons for rejecting the
     * record
     * 
     * @param version the record type version
     * 
     * @param type the record type
     * 
     * @param length the content length
     * 
     * @param id the request id
     */
    void bad(int reasons, int version, int type, int length, int id)
        throws IOException;

    /**
     * Indicates that the type was not recognized. This can contribute
     * to the value of the first argument to
     * {@link #bad(int, int, int, int, int)}.
     */
    int UNKNOWN_TYPE = 1;

    /**
     * Indicates that the type was recognized, but the version is too
     * high. This can contribute to the value of the first argument to
     * {@link #bad(int, int, int, int, int)}.
     */
    int TOO_NEW = 2;

    /**
     * Indicates that the version number was invalid (e.g., 0). This can
     * contribute to the value of the first argument to
     * {@link #bad(int, int, int, int, int)}.
     */
    int BAD_VERSION = 4;

    /**
     * Indicates that the message has an unexpected length. This can
     * contribute to the value of the first argument to
     * {@link #bad(int, int, int, int, int)}.
     */
    int BAD_LENGTH = 8;

    /**
     * Indicates that the request id had an unexpected value for the
     * type. This can contribute to the value of the first argument to
     * {@link #bad(int, int, int, int, int)}.
     */
    int BAD_REQ_ID = 16;
}
