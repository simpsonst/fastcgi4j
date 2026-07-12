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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import uk.ac.lancs.http.ChunkedInputStream;
import uk.ac.lancs.io.CompletingInputStream;
import uk.ac.lancs.io.EmptyInputStream;
import uk.ac.lancs.io.LimitedInputStream;
import uk.ac.lancs.io.UnclosedInputStream;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Holds header fields, and detects transfer encodings, including
 * chunking and the presence of a trailer.
 * 
 * @author simpsons
 */
public abstract class Header extends FieldSet {
    /**
     * Indicates whether a trailer is expected to be parsed. This is
     * {@code false} by default, and only set optionally by a call to
     * {@link #getEncodedBody(InputStream, boolean)}.
     */
    public boolean trailerExpected;

    /**
     * Specifies decoders to be applied to the content. The order is
     * reversed from what <samp>Transfer-Encoding</samp> uses.
     */
    public List<String> transferEncodings = new ArrayList<>();

    public abstract void acceptFirstLine(CharSequence txt);

    /**
     * Get a stream of the message body.
     * 
     * @param base the base stream
     * 
     * @param assumeEnd {@code true} if the content can be assumed to
     * reach to the end of the base stream
     * 
     * @return a stream of the message body
     */
    protected InputStream getEncodedBody(InputStream base, boolean assumeEnd) {
        var teTxt = fields.get("Transfer-Encoding");
        if (teTxt != null) {
            Tokenizer tokenizer = new Tokenizer(teTxt);
            List<String> tokens = new ArrayList<>();
            CharSequence token;
            while ((token = tokenizer.whitespaceAtom(0)) != null) {
                tokens.add(token.toString());
                if (!tokenizer.whitespaceCharacter(0, ',')) break;
            }
            if (!tokens.isEmpty() &&
                tokens.get(tokens.size() - 1).equals("chunked")) {
                transferEncodings
                    .addAll(0, tokens.subList(0, tokens.size() - 1).reversed());
                System.err.println("Providing chunked body...");
                trailerExpected = true;
                return new CompletingInputStream(new ChunkedInputStream(new UnclosedInputStream(base)));
            }
            transferEncodings.addAll(0, tokens.reversed());
        }

        var clTxt = fields.get("Content-Length");
        if (clTxt != null) {
            long contentLength = Long.parseLong(clTxt, 10);
            System.err.printf("Providing body of %d bytes...%n", contentLength);
            return new CompletingInputStream(new LimitedInputStream(new UnclosedInputStream(base),
                                                                    contentLength));
        }

        if (assumeEnd) return new UnclosedInputStream(base);
        return new EmptyInputStream();
    }
}
