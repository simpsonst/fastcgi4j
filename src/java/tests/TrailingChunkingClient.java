// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.function.LongConsumer;
import uk.ac.lancs.http.ChunkedOutputStream;
import uk.ac.lancs.http.Timing;
import uk.ac.lancs.io.CountingInputStream;
import uk.ac.lancs.io.UnclosedInputStream;
import uk.ac.lancs.io.UnclosedOutputStream;
import uk.ac.lancs.io.UnclosedWriter;
import uk.ac.lancs.mime.MediaType;

/**
 * Attempts to PUT a chunked file to a server, with a message digest in
 * the request trailer.
 * 
 * @author simpsons
 */
public class TrailingChunkingClient {
    /**
     * From a socket's output, create a suitable writer for sending HTTP
     * headers, trailers, request lines and response lines.
     * 
     * @param sock the socket to write to
     * 
     * @return a US-ASCII writer to the socket that won't close it when
     * it itself is closed
     * 
     * @throws IOException if an I/O error occurs in getting the
     * socket's output stream
     */
    private static PrintWriter fieldSetWriter(OutputStream out)
        throws IOException {
        return new PrintWriter(out, false, StandardCharsets.US_ASCII);
    }

    private static int getPort(URI loc) {
        int port = loc.getPort();
        if (port >= 1) return port;
        return switch (loc.getScheme()) {
        case "http" -> 80;
        case "https" -> 443;
        default -> -1;
        };
    }

    /**
     * Attempt to PUT a file to a URI, attaching a SHA-512 digest in the
     * trailer. The first argument is a path to a local file to be sent.
     * The second argument is the destination URI.
     * 
     * @param args command-line arguments
     * 
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        URI dest = URI.create(args[1]);
        try (var in = new FileInputStream(file)) {
            try (Socket sock = new Socket(dest.getHost(), getPort(dest));
                 var sockIn =
                     new UnclosedInputStream(new BufferedInputStream(sock
                         .getInputStream()));
                 var sockOut =
                     new UnclosedOutputStream(new BufferedOutputStream(sock
                         .getOutputStream()))) {
                ResponseHeader responseHeader;

                /* Write out the request line and header as US-ASCII. */
                System.err.println("Transmitting header...");
                try (var out = fieldSetWriter(sockOut)) {
                    out.printf("PUT %s HTTP/1.1\r\n", dest.getRawPath());
                    out.printf("Host: %s\r\n", dest.getHost());
                    out.printf("Date: %s\r\n", Timing
                        .generateTimestamp(System.currentTimeMillis()));
                    out.print("Transfer-Encoding: chunked\r\n");
                    out.print("TE: trailers\r\n");
                    out.print("Trailer: X-Digest-SHA-512\r\n");
                    out.print("Expect: 100-continue\r\n");
                    out.print("\r\n");
                }

                /* Await 100 Continue or an error code. */
                System.err.println("Awaiting continue...");
                responseHeader = new ResponseHeader();
                FieldSetParser.of(sockIn, responseHeader).readAll();
                do {
                    if (responseHeader.status != 100) break;

                    /* Write out the request body. */
                    System.err.println("Writing request body...");
                    MessageDigest digest = MessageDigest.getInstance("SHA-512");
                    try (var out =
                        new DigestOutputStream(new ChunkedOutputStream(sockOut),
                                               digest)) {
                        in.transferTo(out);
                    }

                    /* Write out the request trailer. */
                    System.err.println("Writing request trailer...");
                    try (var out =
                        new PrintWriter(new UnclosedOutputStream(sockOut),
                                        false, StandardCharsets.US_ASCII)) {
                        out.printf("X-Digest-SHA-512: %s\r\n",
                                   HexFormat.of().formatHex(digest.digest()));
                        out.print("\r\n");
                    }

                    /* Receive the real response header. */
                    System.err.println("Receiving response header...");
                    responseHeader = new ResponseHeader();
                    FieldSetParser.of(sockIn, responseHeader).readAll();
                } while (false);

                System.err.printf("Response: %d %s%n", responseHeader.status,
                                  responseHeader.message);
                for (var item : responseHeader.fields.entrySet()) {
                    System.err.printf("%s: %s%n", item.getKey(),
                                      item.getValue());
                }

                class Counter implements LongConsumer {
                    long total = 0;

                    public long get() {
                        return total;
                    }

                    @Override
                    public void accept(long amount) {
                        total += amount;
                    }
                }
                var inCounter = new Counter();
                var bytesIn = new CountingInputStream(responseHeader
                    .getBody(sockIn, false), inCounter);
                MediaType type = MediaType
                    .fromString(responseHeader.fields.get("Content-Type"));

                display_body: do {
                    if (type != null) {
                        var cst = type.parameter("charset");
                        if (cst != null) {
                            System.err.printf("charset=%s%n", cst);
                            try (var bodyIn =
                                new InputStreamReader(bytesIn,
                                                      Charset.forName(cst));
                                 var bodyOut =
                                     new UnclosedWriter(new OutputStreamWriter(System.out))) {
                                bodyIn.transferTo(bodyOut);
                            }

                            break display_body;
                        }
                    }

                    try (var bodyIn = bytesIn) {
                        int c, pos = 0;
                        while ((c = bodyIn.read()) >= 0) {
                            switch (c) {
                            case '\n':
                                System.out.print("\\n\n");
                                pos = 0;
                                break;

                            case '\r':
                                System.out.print("\\r");
                                pos += 2;
                                break;

                            case '\t':
                                System.out.print("\\t");
                                pos += 2;
                                break;

                            default:
                                if (c < 32 || c >= 127) {
                                    System.out.printf("\\x%02x", c);
                                    pos += 4;
                                } else {
                                    System.out.print((char) c);
                                    pos += 1;
                                }
                                break;
                            }
                            if (pos >= 72) System.out.println();
                        }
                    }
                } while (false);
                System.err.printf("bytes returned: %d%n", inCounter.get());

                if (responseHeader.trailerExpected) {
                    FieldSet trailer = new FieldSet();
                    FieldSetParser.of(sockIn, trailer).readAll();
                    System.err.println("Trailer:");
                    for (var item : trailer.fields.entrySet()) {
                        System.err.printf("%s: %s%n", item.getKey(),
                                          item.getValue());
                    }
                }
                System.err.println("Complete.");
            }
        }
    }
}