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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.TreeMap;
import uk.ac.lancs.fastcgi.Responder;
import uk.ac.lancs.fastcgi.path.Navigator;
import uk.ac.lancs.fastcgi.path.PathConfiguration;
import uk.ac.lancs.fastcgi.path.PathContext;
import uk.ac.lancs.fastcgi.context.ResponderSession;

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

    @Override
    public void respond(ResponderSession ctxt) throws Exception {
        PathContext<String> pathCtxt = pathConfig.recognize(ctxt.parameters());
        Navigator navigator = pathCtxt.navigator();

        final byte[] dig;
        MessageDigest md = MessageDigest.getInstance("md5");
        byte[] buf = new byte[1024];
        int got;
        while ((got = ctxt.in().read(buf)) >= 0) {
            md.update(buf, 0, got);
        }
        dig = md.digest();

        if (navigator.resource().isEmpty()) {
            ctxt.setStatus(302);
            ctxt.setHeader("Location",
                           navigator.locate("/").absolute().toASCIIString());
            return;
        }

        ctxt.setHeader("Content-Type", "text/plain; charset=UTF-8");
        try (PrintWriter out =
            new PrintWriter(new OutputStreamWriter(ctxt.out(),
                                                   StandardCharsets.UTF_8))) {
            for (var entry : new TreeMap<>(ctxt.parameters()).entrySet()) {
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

            out.printf("\nDigest: ");
            for (int i = 0; i < dig.length; i++) {
                out.printf("%02x", dig[i] & 0xff);
            }
            out.printf("\n");
            out.printf("\nDiagnostics: %s\n", ctxt.diagnostics());
        }
    }
}
