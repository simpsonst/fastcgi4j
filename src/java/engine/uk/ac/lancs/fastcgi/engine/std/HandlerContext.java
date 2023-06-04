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

import java.nio.charset.Charset;
import java.util.concurrent.Executor;
import uk.ac.lancs.fastcgi.proto.serial.RecordWriter;

/**
 * Provides context for initializing all kinds of session handlers.
 *
 * @author simpsons
 */
class HandlerContext {
    final int connId;

    final int id;

    final Package impl;

    final String connDescr;

    final String intConnDescr;

    final Runnable connAbort;

    final Runnable cleanUp;

    final RecordWriter recordsOut;

    final Executor executor;

    final Charset charset;

    final BufferPool paramBufs;

    final int stdoutBufferSize;

    final int stderrBufferSize;

    /**
     * 
     * @param connId the internal transport connection id
     * 
     * @param id the session id
     * 
     * @param impl the implementation of the transport connection
     * 
     * @param connDescr a textual description of the transport
     * connection
     * 
     * @param intConnDescr sensitive parts of the textual description of
     * the transport connection
     * 
     * @param connAbort an action to take if the transport is disrupted
     * 
     * @param cleanUp an action to take when the session is complete
     * 
     * @param recordsOut a means to write FastCGI records to the
     * transport connection
     * 
     * @param executor a means to execute the application
     * 
     * @param charset the character encoding for the standard error
     * output and the response headers
     * 
     * @param paramBufs a pool of buffers for reading in request
     * parameters
     * 
     * @param stdoutBufferSize the default buffer size for standard
     * output
     * 
     * @param stderrBufferSize the buffer size of standard error output
     */
    public HandlerContext(int connId, int id, Package impl, String connDescr,
                          String intConnDescr, Runnable connAbort,
                          Runnable cleanUp, RecordWriter recordsOut,
                          Executor executor, Charset charset,
                          BufferPool paramBufs, int stdoutBufferSize,
                          int stderrBufferSize) {
        this.connId = connId;
        this.id = id;
        this.impl = impl;
        this.connDescr = connDescr;
        this.intConnDescr = intConnDescr;
        this.connAbort = connAbort;
        this.cleanUp = cleanUp;
        this.recordsOut = recordsOut;
        this.executor = executor;
        this.charset = charset;
        this.paramBufs = paramBufs;
        this.stdoutBufferSize = stdoutBufferSize;
        this.stderrBufferSize = stderrBufferSize;
    }
}
