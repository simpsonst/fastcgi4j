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

package uk.ac.lancs.fastcgi.proto.serial;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import uk.ac.lancs.fastcgi.proto.RecordTypes;
import uk.ac.lancs.fastcgi.proto.RequestFlags;

/**
 * Accepts deserialized FastCGI records.
 * 
 * @author simpsons
 */
public interface RecordHandler {
    /**
     * Inform of a request for a set of application variables. This is
     * called on reception of a {@link RecordTypes#GET_VALUES} record.
     * 
     * @param names the names of the requested variables
     * 
     * @throws IOException if an I/O error occurs in transmitting a
     * responding record
     */
    void getValues(Collection<? extends String> names) throws IOException;

    /**
     * Begin a request. This is called on reception of a
     * {@link RecordTypes#BEGIN_REQUEST} record.
     * 
     * @param id the request id
     * 
     * @param role the role of the application in serving this request
     * 
     * @param flags the request flags, as defined by
     * {@link RequestFlags}
     * 
     * @throws IOException if an I/O error occurs in transmitting a
     * responding record
     */
    void beginRequest(int id, int role, int flags) throws IOException;

    /**
     * Abort a request. This is called on reception of a
     * {@link RecordTypes#ABORT_REQUEST} record.
     * 
     * @param id the request id
     * 
     * @throws IOException if an I/O error occurs in transmitting a
     * responding record
     */
    void abortRequest(int id) throws IOException;

    /**
     * Receive a stream of parameter data. This is called on reception
     * of a non-empty {@link RecordTypes#PARAMS} record. The provided
     * stream will provide the exact number of bytes expected, and can
     * be closed early without disrupting any underlying stream it is
     * based on.
     * 
     * @apiNote A recommended implementation is to call
     * {@link ParamReader#consume(InputStream)}.
     * 
     * @param id the request id
     * 
     * @param len the number of bytes
     * 
     * @param in a stream of the bytes
     * 
     * @throws IOException if an I/O error occurs in receiving the
     * record content
     */
    void params(int id, int len, InputStream in) throws IOException;

    /**
     * Indicate that parameter data is complete. This is called on
     * reception of an empty {@link RecordTypes#PARAMS} record.
     * 
     * @apiNote A recommended implementation is to call
     * {@link ParamReader#complete()}.
     * 
     * @param id the request id
     * 
     * @throws IOException if an I/O error occurred in transmitting a
     * responding record
     */
    void paramsEnd(int id) throws IOException;

    /**
     * Receive a stream of standard-input data. This is called on
     * reception of a non-empty {@link RecordTypes#STDIN} record. The
     * provided stream will provide the exact number of bytes expected,
     * and can be closed early without disrupting any underlying stream
     * it is based on.
     * 
     * @param id the request id
     * 
     * @param len the number of bytes
     * 
     * @param in a stream of the bytes
     * 
     * @throws IOException if an I/O error occurred in transmitting the
     * record
     */
    void stdin(int id, int len, InputStream in) throws IOException;

    /**
     * Indicate that standard input is complete. This is called on
     * reception of an empty {@link RecordTypes#STDIN} record.
     * 
     * @param id the request id
     * 
     * @throws IOException if an I/O error occurred in transmitting a
     * responding record
     */
    void stdinEnd(int id) throws IOException;

    /**
     * Receive a stream of extra data. This is called on reception of a
     * non-empty {@link RecordTypes#DATA} record. The provided stream
     * will provide the exact number of bytes expected, and can be
     * closed early without disrupting any underlying stream it is based
     * on.
     * 
     * @param id the request id
     * 
     * @param len the number of bytes
     * 
     * @param in a stream of the bytes
     * 
     * @throws IOException if an I/O error occurred in transmitting the
     * record
     */
    void data(int id, int len, InputStream in) throws IOException;

    /**
     * Indicate that extra data is complete. This is called on reception
     * of an empty {@link RecordTypes#DATA} record.
     * 
     * @param id the request id
     * 
     * @throws IOException if an I/O error occurred in transmitting the
     * record
     */
    void dataEnd(int id) throws IOException;

    /**
     * Report a bad record.
     * 
     * @param reasons flags identifying the reasons for rejecting the
     * record, as defined by {@link #TOO_NEW},
     * {@link #BAD_VERSION},{@link #BAD_REQ_ID}, {@link #BAD_LENGTH} and
     * {@link #UNKNOWN_TYPE}
     * 
     * @param version the record type version
     * 
     * @param type the record type
     * 
     * @param length the content length
     * 
     * @param id the request id
     * 
     * @throws IOException if an I/O error occurred in transmitting the
     * record
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
