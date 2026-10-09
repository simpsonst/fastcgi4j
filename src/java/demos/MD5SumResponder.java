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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.TreeMap;
import uk.ac.lancs.cgi.path.Navigator;
import uk.ac.lancs.cgi.path.PathConfiguration;
import uk.ac.lancs.cgi.path.PathContext;
import uk.ac.lancs.fastcgi.ConfigurationException;
import uk.ac.lancs.fastcgi.Responder;
import uk.ac.lancs.fastcgi.ResponderSession;
import uk.ac.lancs.fastcgi.SessionException;
import uk.ac.lancs.fastcgi.augment.HttpResponderContext;
import uk.ac.lancs.fastcgi.augment.HttpResponderSession;
import uk.ac.lancs.fastcgi.ots.OTSResponses;
import uk.ac.lancs.http.field.FieldExtension;
import uk.ac.lancs.http.field.FieldId;

/**
 * Responds by echoing all headers, and displaying a hex MD5 sum of the
 * request.
 * 
 * @author simpsons
 */
public class MD5SumResponder implements Responder {
    private static final FieldExtension MY_E2E_NAMESPACE = FieldExtension
        .in("http://example.com/ns-e2e").endToEnd().optional().complete();

    private static final FieldExtension MY_HBH_NAMESPACE = FieldExtension
        .in("http://example.com/ns-hbh").hopByHop().optional().complete();

    private static final FieldExtension MY_EMPTY_NAMESPACE = FieldExtension
        .in("http://example.com/ns-empty").endToEnd().optional().complete();

    private static final FieldId REQUEST_FIELD = MY_E2E_NAMESPACE.of("Foo");

    private static final FieldId SILLY_FIELD = MY_E2E_NAMESPACE.of("Silly");

    private static final FieldId LATE_FIELD = MY_E2E_NAMESPACE.of("Late");

    private static final FieldId STUPID_FIELD = MY_HBH_NAMESPACE.of("Stupid");

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

    private static final HttpResponderContext httpRspCtxt =
        new HttpResponderContext() {};

    @Override
    public void respond(ResponderSession session)
        throws IOException,
            SessionException,
            InterruptedException {
        PathContext<String> pathCtxt =
            pathConfig.recognize(session.parameters());
        Navigator navigator = pathCtxt.navigator();
        try (HttpResponderSession httpSession =
            new HttpResponderSession(session, httpRspCtxt)) {
            OTSResponses otsRsp =
                new OTSResponses(httpSession.otsResponseControl());

            var foo = httpSession.requestHeader().get(REQUEST_FIELD);
            System.err.printf("%s=%s%n", REQUEST_FIELD, foo);

            final byte[] dig;
            {
                try {
                    var md = MessageDigest.getInstance("md5");
                    try (var mdis =
                        new DigestInputStream(httpSession.in(), md)) {
                        mdis.transferTo(OutputStream.nullOutputStream());
                    }
                    dig = md.digest();
                } catch (NoSuchAlgorithmException ex) {
                    throw new ConfigurationException("obtaining digest", ex);
                }
            }

            if (navigator.resource().isEmpty()) {
                var dest = navigator.locate("/").absolute();
                System.err.printf("Redirecting \"%s\" to \"%s\"%n",
                                  navigator.resource(), dest);
                otsRsp.found(dest);
                return;
            }

            boolean trailerAllowed = httpSession.responseTrailerAllowed();
            if (trailerAllowed) httpSession.expectInTrailer(LATE_FIELD);
            httpSession.vary(SILLY_FIELD);
            var emptyId =
                httpSession.responseExtensions().define(MY_EMPTY_NAMESPACE);
            SILLY_FIELD.set(httpSession.responseHeader(), "silliness");
            STUPID_FIELD.set(httpSession.responseHeader(),
                             emptyId.toString() + "-stupidity");
            try (PrintWriter out = otsRsp.textOut("plain")) {
                for (var entry : new TreeMap<>(session.parameters())
                    .entrySet()) {
                    out.printf("[%s] = [%s]\n", entry.getKey(),
                               entry.getValue());
                }

                out.printf("\nPath computations:\n");
                out.printf("Script: %s (deprecated)\n", pathCtxt.script());
                out.printf("Script: %s\n", navigator.locate("").local());
                out.printf("Subpath: %s\n", navigator.resource());
                for (String sp : subpaths) {
                    try {
                        out.printf("Ref: [%s] -> [%s] [%s] [%s]%n", sp,
                                   navigator.locate(sp).relative()
                                       .toASCIIString(),
                                   navigator.locate(sp).local().toASCIIString(),
                                   navigator.locate(sp).absolute()
                                       .toASCIIString());
                    } catch (IllegalArgumentException ex) {
                        out.printf("Ref: [%s] invalid (%s)%n", sp,
                                   ex.getMessage());
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
                if (trailerAllowed)
                    LATE_FIELD.set(httpSession.responseTrailer(), "Hey!");
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
