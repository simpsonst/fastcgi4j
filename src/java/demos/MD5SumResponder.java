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

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import uk.ac.lancs.fastcgi.Responder;
import uk.ac.lancs.fastcgi.context.ResponderContext;

/**
 * Responds by echoing all headers, and displaying a hex MD5 sum of the
 * request.
 * 
 * @author simpsons
 */
public class MD5SumResponder implements Responder {
    /**
     * Derive the path context and sub-path of a request based on CGI
     * parameters. The sub-path should determine what function to
     * actually provide, and either begins with a forward slash or is
     * empty. The path context helps the function generate relative URIs
     * to other functions. Both strings are fully percent-decoded.
     * 
     * <p>
     * The algorithm is as follows:
     * 
     * <ol>
     * 
     * <li>If <samp>PATH_INFO</samp> is set, return its value as the
     * sub-path, and the value of <samp>SCRIPT_NAME</samp> as the path
     * context.</li>
     * 
     * <li>Otherwise, if <samp>SCRIPT_FILENAME</samp> is a
     * <samp>proxy:</samp> URI, get the raw scheme-specific part of it,
     * parse it as a URI, and extract the decoded path. If it is a
     * suffix of the value of <samp>SCRIPT_NAME</samp>, return it as the
     * the sub-path, and subtract it from <samp>SCRIPT_NAME</samp> to
     * yield the path context.</li>
     * 
     * <li>Otherwise, the value of <samp>SCRIPT_NAME</samp> is the path
     * context, and the sub-path is empty.</li>
     * 
     * </ol>
     * 
     * @todo Make this into a utility.
     * 
     * @todo Does this work for a spawned process?
     * 
     * @param params the CGI parameters
     * 
     * @return an array of two strings, the first being the path
     * context, and the second being the sub-path
     */
    private String[] splitPath(Map<String, String> params) {
        final URI scriptFilename = URI.create(params.get("SCRIPT_FILENAME"));
        final String pathInfo = params.get("PATH_INFO");
        final String scriptName = params.get("SCRIPT_NAME");
        if (pathInfo != null) {
            return new String[] { scriptName, pathInfo };
        } else if ("proxy".equals(scriptFilename.getScheme())) {
            final URI ssp =
                URI.create(scriptFilename.getRawSchemeSpecificPart());
            final String virtualPath = ssp.getPath();
            if (scriptName.endsWith(virtualPath)) {
                final String correctedScriptName = scriptName
                    .substring(0, scriptName.length() - virtualPath.length());
                return new String[] { correctedScriptName, virtualPath };
            }
        }
        return new String[] { scriptName, "" };
    }

    @Override
    public void respond(ResponderContext ctxt) throws Exception {
        final byte[] dig;
        MessageDigest md = MessageDigest.getInstance("md5");
        byte[] buf = new byte[1024];
        int got;
        while ((got = ctxt.in().read(buf)) >= 0) {
            md.update(buf, 0, got);
        }
        dig = md.digest();

        String[] pathCtxt = splitPath(ctxt.parameters());

        ctxt.setHeader("Content-Type", "text/plain; charset=UTF-8");
        try (PrintWriter out =
            new PrintWriter(new OutputStreamWriter(ctxt.out(),
                                                   StandardCharsets.UTF_8))) {
            for (var entry : new TreeMap<>(ctxt.parameters()).entrySet()) {
                out.printf("[%s] = [%s]\n", entry.getKey(), entry.getValue());
            }

            out.printf("\nPath computations:\n");
            out.printf("Context: %s\n", pathCtxt[0]);
            out.printf("Subpath: %s\n", pathCtxt[1]);

            out.printf("\nDigest: ");
            for (int i = 0; i < dig.length; i++) {
                out.printf("%02x", dig[i] & 0xff);
            }
            out.printf("\n");
            out.printf("\nDiagnostics: %s\n", ctxt.diagnostics());
        }
    }
}
