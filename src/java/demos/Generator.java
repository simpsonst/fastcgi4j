// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2022,2023,2026, Lancaster University
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

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import uk.ac.lancs.cgi.FormSubmission;
import uk.ac.lancs.fastcgi.Responder;
import uk.ac.lancs.fastcgi.app.FastCGIApplication;
import uk.ac.lancs.fastcgi.app.FastCGIConfiguration;
import uk.ac.lancs.fastcgi.ResponderSession;
import uk.ac.lancs.fastcgi.augment.FormHandler;
import uk.ac.lancs.io.DiagnosticInputStream;
import uk.ac.lancs.io.LimitedInputStream;
import uk.ac.lancs.mime.MessageParser;
import uk.ac.lancs.mime.TextMessage;
import uk.ac.lancs.mime.body.Morgue;
import uk.ac.lancs.mime.body.SmartMorgue;

/**
 * Generates self-metering text of a specified size.
 * {@link DiagnosticInputStream} is used to generate the response.
 *
 * @author simpsons
 */
public class Generator extends FastCGIApplication implements Responder {
    @Override
    public boolean init(FastCGIConfiguration config, String[] args) {
        return true;
    }

    private static final Morgue morgue =
        SmartMorgue.start().singleThreshold(20).build();

    private static final FormHandler formHandler =
        new FormHandler(new MessageParser(morgue), StandardCharsets.UTF_8);

    @Override
    public void respond(ResponderSession session) throws Exception {
        final FormSubmission submission = formHandler.get(session);
        int lineLength = 64;
        {
            var wMsg = submission.getFirst("width");
            if (wMsg instanceof TextMessage tmsg)
                lineLength = Integer.parseInt(tmsg.textBody().get(), 10);
        }
        var szMsg = submission.getFirst("size");
        if (szMsg instanceof TextMessage tmsg) {
            long size = Long.parseLong(tmsg.textBody().get(), 10);
            session.setField("Content-Length", Long.toString(size, 10));
            session.setField("Content-Type", "text/plain; charset=US-ASCII");
            session.setField("Cache-Control", "no-cache");
            try (var out = new BufferedOutputStream(session.out(), 4096);
                 var source =
                     new LimitedInputStream(DiagnosticInputStream.start()
                         .width(lineLength).withCarriageReturns(true).build(),
                                            size)) {
                source.transferTo(out);
            }
            return;
        }
        session.setField("Content-Type", "text/plain; charset=UTF-8");
        try (var out =
            new PrintStream(session.out(), false, StandardCharsets.UTF_8)) {
            out.println("Specify form parameter 'size' as decimal.");
        }
    }
}
