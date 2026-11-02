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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import uk.ac.lancs.cgi.FormSubmission;
import uk.ac.lancs.fastcgi.Responder;
import uk.ac.lancs.fastcgi.ResponderSession;
import uk.ac.lancs.fastcgi.app.FastCGIApplication;
import uk.ac.lancs.fastcgi.app.FastCGIConfiguration;
import uk.ac.lancs.fastcgi.form.FormHandler;
import uk.ac.lancs.fastcgi.http.HttpResponderContext;
import uk.ac.lancs.fastcgi.http.HttpResponderSession;
import uk.ac.lancs.fastcgi.http.ots.OTSResponses;
import uk.ac.lancs.http.cache.CacheDirective;
import uk.ac.lancs.http.field.Field;
import uk.ac.lancs.io.DiagnosticInputStream;
import uk.ac.lancs.io.LimitedInputStream;
import uk.ac.lancs.mime.MediaType;
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
    private static final HttpResponderContext httpContext =
        new HttpResponderContext() {};

    @Override
    public boolean init(FastCGIConfiguration config, String[] args) {
        System.err.printf("transfer decoders = %s%n",
                          httpContext.transferDecoders().keySet());
        System.err.printf("transfer encoders = %s%n",
                          httpContext.transferEncoders());
        return true;
    }

    private static final Morgue morgue =
        SmartMorgue.start().singleThreshold(20).build();

    private static final FormHandler formHandler =
        new FormHandler(new MessageParser(morgue), StandardCharsets.UTF_8);

    @Override
    public void respond(ResponderSession session) throws IOException {
        final FormSubmission submission = formHandler.get(session);

        /* Get the requested line length, or use the default. */
        final int lineLength;
        {
            var wMsg = submission.getFirst("width");
            if (wMsg instanceof TextMessage tmsg)
                lineLength = Integer.parseInt(tmsg.textBody().get(), 10);
            else
                lineLength = 64;
        }

        /* Get the requested file size, if specified. */
        final Long size;
        var szMsg = submission.getFirst("size");
        if (szMsg instanceof TextMessage tmsg) {
            size = Long.valueOf(tmsg.textBody().get(), 10);
        } else {
            size = null;
        }

        try (var httpSession = new HttpResponderSession(session, httpContext)) {
            /* Generate the requested content if the size is
             * specified. */
            if (size != null) {
                Field.CONTENT_TYPE.set(httpSession.responseHeader(),
                                       MediaType.of("text", "plain").modify()
                                           .set("charset", "US-ASCII").apply());
                Field.CONTENT_LENGTH_LONG.set(httpSession.responseHeader(),
                                              size);
                CacheDirective.NO_CACHE.set(httpSession.responseCacheControl());
                try (var out =
                    new BufferedOutputStream(httpSession.out(), 4096);
                     var source = new LimitedInputStream(DiagnosticInputStream
                         .start().width(lineLength).withCarriageReturns(true)
                         .build(), size)) {
                    source.transferTo(out);
                }
                return;
            }

            /* Generate an informative message if no size is
             * specified. */
            var ots = new OTSResponses(httpSession.otsResponseControl());
            try (var out = ots.textOut("plain")) {
                out.println("Specify form parameter 'size' as decimal.");
            }
        }
    }
}
