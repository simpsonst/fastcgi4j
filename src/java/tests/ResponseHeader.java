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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;
import uk.ac.lancs.io.EmptyInputStream;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Holds a response header.
 *
 * @author simpsons
 */
public class ResponseHeader extends Header {
    private static final String FIRST_LINE_PATTERN_STRING =
        "^([A-Za-z][A-Za-z0-9]*/[0-9]+\\.[0-9]+)" + "\\s+([0-9]+)\\s+(.*)$";

    private static final Pattern FIRST_LINE_PATTERN =
        Pattern.compile(FIRST_LINE_PATTERN_STRING);

    /**
     * Holds the response status code. This is set only after
     * {@link #acceptResponseLine(CharSequence)} is called with a
     * well-formed line.
     */
    public int status = 0;

    /**
     * Holds the protocol specified by the server in the response line.
     * This is set only after {@link #acceptResponseLine(CharSequence)}
     * is called with a well-formed line.
     */
    public String serverProtocol;

    /**
     * Holds the human-readable part of the response line. This is set
     * only after {@link #acceptResponseLine(CharSequence)} is called
     * with a well-formed line.
     */
    public String message;

    /**
     * Parse the response line, and store the components. The pattern
     * must match {@value #FIRST_LINE_PATTERN_STRING}.
     * 
     * @param txt the unparsed response line
     */
    @Override
    public void acceptFirstLine(CharSequence txt) {
        Matcher m = FIRST_LINE_PATTERN.matcher(txt);
        if (!m.matches()) {
            System.err.printf("bad: %s%n", txt);
            return;
        }
        serverProtocol = m.group(1);
        status = Integer.parseInt(m.group(2), 10);
        message = m.group(3);
    }

    public Set<String> connectionTokens;

    private void ensureConnectionTokens() {
        if (connectionTokens != null) return;
        var conTxt = fields.get("Connection");
        connectionTokens = new HashSet<>();
        if (conTxt != null) {
            Tokenizer tokenizer = new Tokenizer(conTxt);
            CharSequence token;
            while ((token = tokenizer.whitespaceAtom(0)) != null) {
                connectionTokens.add(token.toString());
                if (!tokenizer.whitespaceCharacter(0, ',')) break;
            }
        }
    }

    /**
     * Get an input stream lasting exactly as long as the response body,
     * with as many understood decodings applied as possible. Any
     * remaining encodings are left in {@link Header#transferEncodings}.
     * 
     * @param base the base input stream
     * 
     * @param head {@code true} if this is a response to a head request
     * 
     * @return the decoded body stream
     * 
     * @throws IOException if an I/O error occurs in establishing
     * decoders
     */
    public InputStream getBody(InputStream base, boolean head)
        throws IOException {
        if (head) return new EmptyInputStream();

        System.err.println("Getting connection tokens...");
        ensureConnectionTokens();

        base = getEncodedBody(base, connectionTokens.contains("close"));
        next_encoding: while (!transferEncodings.isEmpty()) {
            String top = transferEncodings.get(0);
            switch (top) {
            case "deflate":
                base = new DeflaterInputStream(base);
                transferEncodings.remove(0);
                break;

            case "gzip":
                base = new GZIPInputStream(base);
                transferEncodings.remove(0);
                break;

            default:
                break next_encoding;
            }
        }

        return base;
    }
}
