
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import uk.ac.lancs.fastcgi.Authorizer;
import uk.ac.lancs.fastcgi.context.AuthorizerSession;
import uk.ac.lancs.fastcgi.util.Http;

// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2023, Lancaster University
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

/**
 * Acts as a really bad authenticator/authorizer. The session parameter
 * <samp>FCGI_APACHE_ROLE</samp> is tested. If it's
 * <samp>AUTHENTICATOR</samp>, it checks whether
 * <samp>REMOTE_PASSWD</samp> is <samp>ho</samp>. If it is, it sets
 * <samp>FLONG_USER</samp> and <samp>FLONG_METHOD</samp> as
 * authentication variables, and yields {@link Http#OK} response. If the
 * password is wrong it yields a {@link Http#FORBIDDEN} response with a
 * message. If the Apache role is <samp>AUTHORIZER</samp>, it yields a
 * {@link Http#FORBIDDEN} response with a message.
 * 
 * <p>
 * The primary purpose of this class is to explore whether Apache
 * consults an authorizer before responding to an <samp>Expect:
 * 100-continue</samp> request header field with <samp>100</samp>. It
 * seems it does! That's useful, because it will already have got the
 * request body by the time it calls a responder, so a FastCGI script
 * can reject the postponed request body, but only as an authorizer.
 * 
 * @author simpsons
 */
public class DiagnosticAuthorizer implements Authorizer {
    @Override
    public void authorize(AuthorizerSession session) throws Exception {
        String apacheRole = session.parameters().get("FCGI_APACHE_ROLE");
        for (var e : session.parameters().entrySet()) {
            System.err.printf("%s: %s\n", e.getKey(), e.getValue());
        }
        if ("AUTHENTICATOR".equals(apacheRole)) {
            System.err.printf("We are a pure authenticator today%n");
            if ("ho".equals(session.parameters().get("REMOTE_PASSWD"))) {
                System.err.printf("   and the user is okay%n");
                session.setVariable("FLONG_USER", "the-one");
                session.setVariable("FLONG_METHOD", "lame");
                return;
            }
            System.err.printf("   naughty user%n");
            session.setField("Content-Type", "text/plain; charset=UTF-8");
            session.setStatus(Http.FORBIDDEN);
            try (var out =
                new PrintWriter(session.out(), false, StandardCharsets.UTF_8)) {
                out.printf("Who are you?! Doo-dooo!\n");
            }
        } else if ("AUTHORIZER".equals(apacheRole)) {
            System.err.printf("We are a pure authorizer now: %d%n",
                              Http.FORBIDDEN);
            // session.setField("Content-Type", "text/plain;
            // charset=UTF-8");
            session.setStatus(Http.FORBIDDEN);
            // try (var out =
            // new PrintWriter(session.out(), false,
            // StandardCharsets.UTF_8)) {
            // out.printf("No!\n");
            // }
        }
    }
}
