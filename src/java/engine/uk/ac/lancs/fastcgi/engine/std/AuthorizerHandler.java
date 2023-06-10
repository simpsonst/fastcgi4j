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

import java.util.Objects;
import uk.ac.lancs.fastcgi.Authorizer;
import uk.ac.lancs.fastcgi.context.AuthorizerContext;

/**
 * Handles Authorizer sessions.
 *
 * @author simpsons
 */
class AuthorizerHandler extends AbstractHandler implements AuthorizerContext {
    private final Authorizer app;

    /**
     * Create an Authorizer handler.
     * 
     * @param ctxt the context
     * 
     * @param app the application-specific behaviour
     */
    public AuthorizerHandler(HandlerContext ctxt, Authorizer app) {
        super(ctxt);
        this.app = app;
    }

    @Override
    void innerRun() throws Exception {
        app.authorize(this);
    }

    @Override
    public void addHeader(String name, String value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        name = name.trim();
        if (isVariable(name))
            throw new IllegalArgumentException("reserved name " + name);
        addHeaderInternal(name, value);
        if (statusCode == 200) statusCode = 401;
    }

    @Override
    public void setHeader(String name, String value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        name = name.trim();
        if (isVariable(name))
            throw new IllegalArgumentException("reserved name " + name);
        setHeaderInternal(name, value);
        if (statusCode == 200) statusCode = 401;
    }

    @Override
    public void setVariable(String name, String value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        name = name.trim();
        setHeaderInternal(AuthorizerContext.VARIABLE_PREFIX + name, value);
    }

    private static final int VARIABLE_PREFIX_LENGTH =
        AuthorizerContext.VARIABLE_PREFIX.length();

    private static boolean isVariable(CharSequence in) {
        if (in.length() < VARIABLE_PREFIX_LENGTH) return false;
        CharSequence prefix =
            in.subSequence(0, VARIABLE_PREFIX_LENGTH).toString();
        return AuthorizerContext.VARIABLE_PREFIX.equals(prefix);
    }
}
