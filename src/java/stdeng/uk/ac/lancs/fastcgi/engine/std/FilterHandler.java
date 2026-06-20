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

package uk.ac.lancs.fastcgi.engine.std;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.lancs.fastcgi.Filter;
import uk.ac.lancs.fastcgi.context.FilterSession;
import uk.ac.lancs.fastcgi.context.SessionAbortedException;
import uk.ac.lancs.io.infpipe.Pipe;
import uk.ac.lancs.io.infpipe.SinkClosedException;

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
        this.stdinSink = stdinPipe.getInputStream();
        this.stdinSource = stdinPipe.getOutputStream();
        this.dataPipe = dataPipe;
        this.dataSink = dataPipe.getInputStream();
        this.dataSource = dataPipe.getOutputStream();
    }

    @Override
    void innerRun() throws Exception {
        app.filter(this);
    }

    private final Pipe stdinPipe;

    private final InputStream stdinSink;

    private OutputStream stdinSource;

    private final Pipe dataPipe;

    private final InputStream dataSink;

    private OutputStream dataSource;

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
        if (stdinSource == null) return;
        try {
            in.transferTo(stdinSource);
        } catch (SinkClosedException ex) {
            /* It's normal for the application to close stdin
             * prematurely, so just discard the data. */
            stdinSource = null;
        } catch (IOException ex) {
            /* Log other errors. */
            logger.log(Level.SEVERE, ex,
                       () -> "sess-" + connId + "." + id + ":stdin");
            stdinSource = null;
        }
    }

    private boolean stdinEnded = false;

    @Override
    public void stdinEnd() throws IOException {
        logger.finer(() -> msg("stdin-end"));
        try {
            if (stdinSource != null) stdinSource.close();
        } catch (SinkClosedException ex) {
            /* It's normal for the application to close stdin
             * prematurely, so just discard the data. */
            stdinSource = null;
        } catch (IOException ex) {
            /* Log other errors. */
            logger.log(Level.SEVERE, ex,
                       () -> "sess-" + connId + "." + id + ":stdin");
            stdinSource = null;
        } finally {
            stdinEnded = true;
        }
    }

    @Override
    public InputStream in() {
        return stdinSink;
    }

    @Override
    public Map<String, List<String>> requestTrailer()
        throws InterruptedException {
        if (!stdinEnded) throw new IllegalStateException("STDIN incomplete");
        return super.trailer();
    }

    @Override
    public void data(int len, InputStream in) throws IOException {
        logger.finer(() -> msg("data(%d)", len));
        if (dataSource == null) return;
        try {
            in.transferTo(dataSource);
        } catch (SinkClosedException ex) {
            /* It's normal for the application to close data
             * prematurely, so just discard the data. */
            dataSource = null;
        } catch (IOException ex) {
            /* Log other errors. */
            logger.log(Level.SEVERE, ex,
                       () -> "sess-" + connId + "." + id + ":data");
            dataSource = null;
        }
    }

    @Override
    public void dataEnd() throws IOException {
        logger.finer(() -> msg("data-end"));
        if (dataSource == null) return;
        try {
            dataSource.close();
        } catch (SinkClosedException ex) {
            /* It's normal for the application to close data
             * prematurely, so just discard the data. */
            dataSource = null;
        } catch (IOException ex) {
            /* Log other errors. */
            logger.log(Level.SEVERE, ex,
                       () -> "sess-" + connId + "." + id + ":data");
            dataSource = null;
        }
    }

    @Override
    public InputStream data() {
        return dataSink;
    }

    private static final Logger logger =
        Logger.getLogger(FilterHandler.class.getPackageName());
}
