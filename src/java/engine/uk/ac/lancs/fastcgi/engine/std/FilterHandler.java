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
import java.util.logging.Logger;
import uk.ac.lancs.fastcgi.Filter;
import uk.ac.lancs.fastcgi.context.SessionAbortedException;
import uk.ac.lancs.fastcgi.engine.util.Pipe;
import uk.ac.lancs.fastcgi.context.FilterSession;

/**
 * Handles Filter sessions.
 *
 * @author simpsons
 */
class FilterHandler extends AbstractHandler implements FilterSession {
    private final Filter app;

    /**
     * Create a Filter handler.
     * 
     * @param ctxt the context
     * 
     * @param app the application-specific behaviour
     * 
     * @param stdinPipe a pipe to cache the standard input
     * 
     * @param dataPipe a pipe to cache the additional data
     */
    public FilterHandler(HandlerContext ctxt, Filter app, Pipe stdinPipe,
                         Pipe dataPipe) {
        super(ctxt);
        this.app = app;
        this.stdinPipe = stdinPipe;
        this.dataPipe = dataPipe;
    }

    @Override
    void innerRun() throws Exception {
        app.filter(this);
    }

    private final Pipe stdinPipe;

    private final Pipe dataPipe;

    @Override
    public void abortRequest() throws IOException {
        SessionAbortedException ex =
            new SessionAbortedException("sess-" + connId + "." + id);
        stdinPipe.abort(ex);
        dataPipe.abort(ex);
        super.abortRequest();
    }

    @Override
    public void transportFailure(IOException ex) {
        stdinPipe.abort(ex);
        dataPipe.abort(ex);
        super.transportFailure(ex);
    }

    @Override
    public void stdin(int len, InputStream in) throws IOException {
        logger.finer(() -> msg("stdin(%d)", len));
        in.transferTo(stdinPipe.getOutputStream());
    }

    @Override
    public void stdinEnd() throws IOException {
        logger.finer(() -> msg("stdin-end"));
        stdinPipe.getOutputStream().close();
    }

    @Override
    public InputStream in() {
        return stdinPipe.getInputStream();
    }

    @Override
    public void data(int len, InputStream in) throws IOException {
        logger.finer(() -> msg("data(%d)", len));
        in.transferTo(dataPipe.getOutputStream());
    }

    @Override
    public void dataEnd() throws IOException {
        logger.finer(() -> msg("data-end"));
        dataPipe.getOutputStream().close();
    }

    @Override
    public InputStream data() {
        return dataPipe.getInputStream();
    }

    private static final Logger logger =
        Logger.getLogger(FilterHandler.class.getPackageName());
}
