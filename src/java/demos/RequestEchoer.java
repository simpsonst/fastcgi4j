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
 *  Author: Steven Simpson <s.simpson@lancaster.ac.uk>
 */

import java.io.BufferedInputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import uk.ac.lancs.fastcgi.Responder;
import uk.ac.lancs.fastcgi.app.FastCGIApplication;
import uk.ac.lancs.fastcgi.app.FastCGIConfiguration;
import uk.ac.lancs.fastcgi.ResponderSession;
import uk.ac.lancs.io.CountingInputStream;
import uk.ac.lancs.io.CountingOutputStream;

/**
 * Responds by displaying the request parameters and body.
 *
 * @author simpsons
 */
public class RequestEchoer extends FastCGIApplication implements Responder {
    @Override
    public boolean init(FastCGIConfiguration config, String[] args) {
        return true;
    }

    @Override
    public void respond(ResponderSession session) throws Exception {
        session.setField("Content-Type", "text/plain; charset=UTF-8");
        var outCounter = new Counter();
        var inCounter = new Counter();
        try (var out =
            new PrintWriter(new CountingOutputStream(session.out(), outCounter),
                            false, StandardCharsets.UTF_8)) {
            for (var item : session.parameters().entrySet()) {
                out.printf("%s: %s\n", item.getKey(), item.getValue());
            }
            out.println();
            try (var in =
                new CountingInputStream(new BufferedInputStream(session.in()),
                                        inCounter)) {
                int c;
                while ((c = in.read()) >= 0) {
                    switch (c) {
                    case 13:
                        out.print("\\r");
                        break;

                    case 9:
                        out.print("\\t");
                        break;

                    case 10:
                        out.println("\\n");
                        break;

                    default:
                        if (c <= 31 || c >= 127) {
                            out.printf("\\%03o", c);
                        } else {
                            out.write(c & 0xff);
                        }
                        break;
                    }
                }
            }
            System.err.printf("Bytes in: %d%n", inCounter.get());
        }
        System.err.printf("Bytes out: %d%n", outCounter.get());
    }
}
