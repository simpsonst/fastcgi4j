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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import uk.ac.lancs.fastcgi.Responder;
import uk.ac.lancs.fastcgi.body.BinaryBody;
import uk.ac.lancs.fastcgi.body.Morgue;
import uk.ac.lancs.fastcgi.body.SmartMorgue;
import uk.ac.lancs.fastcgi.context.ResponderSession;
import uk.ac.lancs.fastcgi.mime.BinaryMessage;
import uk.ac.lancs.fastcgi.mime.Message;
import uk.ac.lancs.fastcgi.mime.MessageParser;
import uk.ac.lancs.fastcgi.mime.TextMessage;
import uk.ac.lancs.fastcgi.misc.FormSubmission;
import uk.ac.lancs.fastcgi.misc.SessionAugment;
import uk.ac.lancs.fastcgi.path.Navigator;
import uk.ac.lancs.fastcgi.path.PathConfiguration;
import uk.ac.lancs.fastcgi.path.PathContext;

/**
 * Responds by echoing all headers, and displaying a hex MD5 sum of the
 * request.
 * 
 * @author simpsons
 */
public class MD5SumResponder implements Responder {
    private static final String[] subpaths = { "", "/", "baz/qux", "/baz/qux",
        "baz/qux/quux", "baz/qux/", "baz/yan/tan/", "baz/yan/tan", "/baz/",
        "/baz", "/foo:bar/baz", "/foó/bär/båz" };

    private static final PathConfiguration<String> pathConfig;

    static {
        Properties props = new Properties();
        Path propPath = Paths.get("scratch", "instances.properties");
        try (Reader in = Files.newBufferedReader(propPath)) {
            props.load(in);
        } catch (FileNotFoundException ex) {
            /* Ignore. */
        } catch (IOException ex) {
            System.err.printf("failed to load from %s%n", propPath);
        }
        pathConfig = PathConfiguration.<String>start()
            .instances(props, "", s -> s).create();
    }

    private static final Morgue morgue =
        SmartMorgue.start().singleThreshold(20).build();

    @Override
    public void respond(ResponderSession session) throws Exception {
        SessionAugment augment = new SessionAugment(session);
        PathContext<String> pathCtxt =
            pathConfig.recognize(session.parameters());
        Navigator navigator = pathCtxt.navigator();

        final byte[] dig;
        if (false) {
            dig = null;
        } else {
            MessageDigest md = MessageDigest.getInstance("md5");
            byte[] buf = new byte[1024];
            int got;
            while ((got = session.in().read(buf)) >= 0) {
                md.update(buf, 0, got);
            }
            dig = md.digest();
        }

        if (navigator.resource().isEmpty()) {
            augment.found(navigator.locate("/").absolute());
            return;
        }
        final FormSubmission submission;
        final BinaryBody body;
        if (false) {
            submission = null;
            body = null;
            System.err.printf("Message body!!!!%n");
            try (var in = session.in(); var out = new PrintWriter(System.err)) {
                dump("body ", out, in);
            }
        } else if (false) {
            submission = null;
            try (var in = session.in()) {
                body = morgue.store(in);
            }
        } else if (false) {
            body = null;
            submission =
                FormSubmission.fromSession(session, StandardCharsets.UTF_8,
                                           () -> new MessageParser(morgue));
        } else {
            body = null;
            submission = null;
        }

        augment.offerCompression();
        try (PrintWriter out = augment.textOut("plain")) {
            for (var entry : new TreeMap<>(session.parameters()).entrySet()) {
                out.printf("[%s] = [%s]\n", entry.getKey(), entry.getValue());
            }

            out.printf("\nPath computations:\n");
            out.printf("Script: %s\n", pathCtxt.script());
            out.printf("Subpath: %s\n", navigator.resource());
            for (String sp : subpaths) {
                try {
                    out.printf("Ref: [%s] -> [%s] [%s] [%s]%n", sp,
                               navigator.locate(sp).relative().toASCIIString(),
                               navigator.locate(sp).local().toASCIIString(),
                               navigator.locate(sp).absolute().toASCIIString());
                } catch (IllegalArgumentException ex) {
                    out.printf("Ref: [%s] invalid (%s)%n", sp, ex.getMessage());
                }
            }

            if (dig != null) {
                out.printf("\nDigest: ");
                for (int i = 0; i < dig.length; i++) {
                    out.printf("%02x", dig[i] & 0xff);
                }
                out.printf("\n");
            }

            out.printf("\nDiagnostics: %s\n", session.diagnostics());

            if (body != null) {
                try (var in = body.recover()) {
                    dump("body ", out, in);
                }
            }

            if (submission != null) {
                out.printf("\nForm fields:\n");
                for (var e : submission.map().entrySet()) {
                    List<Message> values = e.getValue();
                    out.printf("  %s (%d):\n", e.getKey(), values.size());
                    int i = 0;
                    for (Message msg : values) {
                        final int pos = ++i;
                        if (msg instanceof TextMessage tmsg) {
                            out.printf("  %d: %s\n", pos,
                                       tmsg.textBody().get());
                        } else if (msg instanceof BinaryMessage bmsg) {
                            dump(String.format("%4d ", pos), out,
                                 bmsg.body().recover());
                        }
                    }
                }
                Thread.sleep(Duration.ofSeconds(30));
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
