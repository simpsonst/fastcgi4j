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
import uk.ac.lancs.fastcgi.context.ResponderContext;
import uk.ac.lancs.fastcgi.context.SessionAbortedException;
import uk.ac.lancs.fastcgi.engine.util.Pipe;
import uk.ac.lancs.fastcgi.Responder;

/**
 * Handles Responder sessions.
 * 
 * @author simpsons
 */
class ResponderHandler extends AbstractHandler implements ResponderContext {
    private final Responder app;

    private final Pipe stdinPipe;

    /**
     * Create a Responder handler.
     * 
     * @param ctxt the context
     * 
     * @param app the application-specific behaviour
     * 
     * @param stdinPipe a pipe to cache the standard input
     */
    public ResponderHandler(HandlerContext ctxt, Responder app,
                            Pipe stdinPipe) {
        super(ctxt);
        this.app = app;
        this.stdinPipe = stdinPipe;
    }

    @Override
    void innerRun() throws Exception {
        app.respond(this);
    }

    @Override
    public void abortRequest() throws IOException {
        SessionAbortedException ex = new SessionAbortedException("id=" + id);
        stdinPipe.abort(ex);
        super.abortRequest();
    }

    @Override
    public void transportFailure(IOException ex) {
        stdinPipe.abort(ex);
        super.transportFailure(ex);
    }

    @Override
    public void stdin(int len, InputStream in) throws IOException {
        in.transferTo(stdinPipe.getOutputStream());
    }

    @Override
    public void stdinEnd() throws IOException {
        stdinPipe.getOutputStream().close();
    }

    @Override
    public InputStream in() {
        return stdinPipe.getInputStream();
    }
}
