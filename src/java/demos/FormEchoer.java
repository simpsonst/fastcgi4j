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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import uk.ac.lancs.cgi.FormSubmission;
import uk.ac.lancs.fastcgi.Responder;
import uk.ac.lancs.fastcgi.app.FastCGIApplication;
import uk.ac.lancs.fastcgi.app.FastCGIConfiguration;
import uk.ac.lancs.fastcgi.ResponderSession;
import uk.ac.lancs.fastcgi.augment.SessionAugment;
import uk.ac.lancs.fastcgi.augment.FormHandler;
import uk.ac.lancs.mime.BinaryMessage;
import uk.ac.lancs.mime.Message;
import uk.ac.lancs.mime.MessageParser;
import uk.ac.lancs.mime.TextMessage;
import uk.ac.lancs.mime.body.Morgue;
import uk.ac.lancs.mime.body.SmartMorgue;

/**
 * Assumes the request is a form submission, and echoes back the fields.
 *
 * @author simpsons
 */
public class FormEchoer extends FastCGIApplication implements Responder {
    private static final Morgue morgue =
        SmartMorgue.start().singleThreshold(20).build();

    private static final FormHandler formHandler =
        new FormHandler(new MessageParser(morgue), StandardCharsets.UTF_8);

    @Override
    public boolean init(FastCGIConfiguration config, String[] args) {
        return true;
    }

    @Override
    public void respond(ResponderSession session) throws Exception {
        SessionAugment augment = new SessionAugment(session);
        final FormSubmission submission = formHandler.get(session);
        try (PrintWriter out = augment.textOut("plain")) {
            out.printf("\nForm fields:\n");
            for (var e : submission.map().entrySet()) {
                List<Message> values = e.getValue();
                out.printf("  %s (%d):\n", e.getKey(), values.size());
                int i = 0;
                for (Message msg : values) {
                    final int pos = ++i;
                    if (msg instanceof TextMessage tmsg) {
                        out.printf("  %d: %s\n", pos, tmsg.textBody().get());
                    } else if (msg instanceof BinaryMessage bmsg) {
                        dump(String.format("%4d ", pos), out,
                             bmsg.body().recover());
                    }
                }
            }
        }
    }

    private static void dump(String pfx, PrintWriter out, InputStream in)
        throws IOException {
        byte[] buf = new byte[16];
        long off = 0;
        do {
            int got = 0;
            while (got < buf.length) {
                int n = in.read(buf, got, buf.length - got);
                if (n < 0) break;
                got += n;
            }
            if (got == 0) break;
            out.printf("%s%08x", pfx, off);
            for (int i = 0; i < buf.length; i++)
                if (i < got)
                    out.printf(" %02X", buf[i]);
                else
                    out.print("   ");
            out.print(' ');
            for (int i = 0; i < got; i++)
                out.printf("%c", buf[i] > 32 ? (char) buf[i] : '.');
            out.println();
            off += got;
        } while (true);
        out.printf("%s total %d\n", pfx, off);
    }
}
