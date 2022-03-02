/*
 * Copyright (c) 2022, Regents of the University of Lancaster
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import uk.ac.lancs.fastcgi.ResponderContext;
import uk.ac.lancs.fastcgi.role.Responder;

/**
 * Responds by echoing all headers, and displaying a hex MD5 sum of the
 * request.
 * 
 * @author simpsons
 */
public class MD5SumResponder implements Responder {
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
        ctxt.setHeader("Content-Type", "text/plain; charset=UTF-8");
        try (PrintWriter out =
            new PrintWriter(new OutputStreamWriter(ctxt.out(),
                                                   StandardCharsets.UTF_8))) {
            for (var entry : ctxt.parameters().entrySet()) {
                out.printf("[%s] = [%s]\n", entry.getKey(), entry.getValue());
            }
            out.printf("Digest: ");
            for (int i = 0; i < dig.length; i++) {
                out.printf("%02x", dig[i] & 0xff);
            }
            out.printf("\n");
            out.printf("Diagnostics: %s\n", ctxt.diagnostics());
        }
    }
}
