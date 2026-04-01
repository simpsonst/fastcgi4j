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

package uk.ac.lancs.fastcgi.engine.std;

import java.io.IOException;
import java.io.InputStream;
import uk.ac.lancs.fastcgi.proto.serial.RecordHandler;

/**
 * Receives application record types, with the request id stripped and
 * implied by context.
 * 
 * @see RecordHandler
 */
interface SessionHandler {
    /**
     * Start the session, if not already.
     *
     * @return {@code true} if the session was already started
     */
    boolean start();

    /**
     * Abort a request.
     * 
     * @throws IOException if an I/O error occurs in transmitting a
     * responding record
     * 
     * @see RecordHandler#abortRequest(int)
     */
    void abortRequest() throws IOException;

    /**
     * Receive a stream of parameter data.
     * 
     * @param len the number of bytes
     * 
     * @param in a stream of the bytes
     * 
     * @throws IOException if an I/O error occurs in receiving the
     * record content
     * 
     * @see RecordHandler#params(int, int, InputStream)
     */
    void params(int len, InputStream in) throws IOException;

    /**
     * Indicate that parameter data is complete.
     * 
     * @default A recommended implementation is to call
     * {@link ParamReader#complete()}.
     * 
     * @throws IOException if an I/O error occurred in transmitting a
     * responding record
     * 
     * @see RecordHandler#paramsEnd(int)
     */
    void paramsEnd() throws IOException;

    /**
     * Receive a stream of standard-input data.
     * 
     * @param len the number of bytes
     * 
     * @param in a stream of the bytes
     * 
     * @throws IOException if an I/O error occurred in transmitting the
     * record
     * 
     * @see RecordHandler#stdin(int, int, InputStream)
     */
    void stdin(int len, InputStream in) throws IOException;

    /**
     * Indicate that standard input is complete.
     * 
     * @throws IOException if an I/O error occurred in transmitting a
     * responding record
     * 
     * @see RecordHandler#stdinEnd(int)
     */
    void stdinEnd() throws IOException;

    /**
     * Receive a stream of extra data.
     * 
     * @param len the number of bytes
     * 
     * @param in a stream of the bytes
     * 
     * @throws IOException if an I/O error occurred in transmitting the
     * record
     * 
     * @see RecordHandler#data(int, int, InputStream)
     */
    void data(int len, InputStream in) throws IOException;

    /**
     * Indicate that extra data is complete.
     * 
     * @throws IOException if an I/O error occurred in transmitting the
     * record
     * 
     * @see RecordHandler#dataEnd(int)
     */
    void dataEnd() throws IOException;

    /**
     * Indicate that the underlying transport has failed. No more
     * records can be sent or received for this session.
     * 
     * @param ex the reason for failure
     */
    void transportFailure(IOException ex);
}
