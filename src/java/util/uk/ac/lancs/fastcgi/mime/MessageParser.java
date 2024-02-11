// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2023, Lancaster University
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

package uk.ac.lancs.fastcgi.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.lancs.fastcgi.body.BinaryBody;
import uk.ac.lancs.fastcgi.body.Morgue;
import uk.ac.lancs.fastcgi.body.SmartMorgue;
import uk.ac.lancs.fastcgi.body.TextBody;
import uk.ac.lancs.fastcgi.io.BoundarySequence;

/**
 * Parses MIME messages and multipart bodies.
 *
 * @author simpsons
 */
public final class MessageParser {
    private final Morgue morgue;

    /**
     * Prepare to parse messages, storing bodies in a morgue.
     * 
     * @param morgue a place to store bodies
     */
    public MessageParser(Morgue morgue) {
        this.morgue = morgue;
    }

    /**
     * Store a MIME message. The source stream is not closed after use.
     * 
     * <p>
     * A header is parsed. The content type is extracted to determine
     * how to store the body. A <samp>multipart</samp> message leads to
     * body parts being separately stored, and a
     * {@link MultipartMessage} is returned. A <samp>text</samp> message
     * leads to the body being converted into a character stream, and a
     * {@link TextMessage} is returned. Otherwise, the body is stored
     * unaltered, and an {@link BinaryMessage} is returned.
     * 
     * @param in the MIME message as a byte stream
     * 
     * @param assumedCharset the character encoding assumed for text
     * messages when unspecified
     * 
     * @return a means to recover the message and its headers
     * 
     * @throws IOException if an I/O error occurs in storing the message
     */
    public Message parseMessage(InputStream in, Charset assumedCharset)
        throws IOException {
        /* Read in the header, and prepare to modify it. */
        Header header = Header.of(in);
        var headerMod = header.modify();

        /* Decode Content-Transfer-Encoding, and reflect in the modified
         * header. */
        String cte =
            header.get("Content-Transfer-Encoding", Format.LAST_ATOM_FORMAT);
        if (cte != null) {
            if (cte.equalsIgnoreCase("base64")) {
                in = java.util.Base64.getMimeDecoder().wrap(in);
                headerMod.remove("Content-Transfer-Encoding");
            } else if (cte.equalsIgnoreCase("quoted-printable")) {
                in = QuotedPrintable.decodeLeaveOpen(in);
                headerMod.remove("Content-Transfer-Encoding");
            }
        }

        /* Get the media type, and act on the broad type. Text is
         * decoded according to the specified or assumed charset, and
         * stored as a text message. A multipart body is split into its
         * constituents. Other media types are simply stored as
         * binary. */
        MediaType contentType = header.get("Content-Type", MediaType.FORMAT);
        if (contentType == null || contentType.isText()) {
            /* Determine the character encoding. */
            final Charset cs;
            if (contentType == null) {
                /* The content type isn't even set, so use the assumed
                 * encoding, and call it text/plain. */
                cs = assumedCharset;
                contentType = MediaType.of("text", "plain");
            } else {
                /* Check if the encoding is specified with the MIME
                 * type. Use the assumed one if not. Ensure that the
                 * content type to be stored with the text does not
                 * specify an encoding. */
                String csText = contentType.parameter("charset");
                if (csText == null)
                    cs = assumedCharset;
                else
                    cs = Charset.forName(csText);
                contentType = contentType.modify().remove("charset").apply();
            }

            /* Store the modified content type. */
            Header altHeader = headerMod
                .set("Content-Type", MediaType.FORMAT, contentType).apply();

            /* Decode the body bytes into text, store, and reference as
             * a text message. */
            Reader cin = new InputStreamReader(in, cs);
            TextBody body = morgue.store(cin);
            return TextMessage.of(altHeader, body);
        } else if (contentType.isMultipart()) {
            /* Extract the boundary, and remove the parameter. */
            String boundary = contentType.parameter("boundary");
            contentType = contentType.modify().remove("boundary").apply();
            Header altHeader = headerMod
                .set("Content-Type", MediaType.FORMAT, contentType).apply();

            /* Split the body into multiple messages, and reference as a
             * multipart message. */
            List<Message> body =
                parseMultipartBody(in, boundary, assumedCharset);
            return new MultipartMessage() {
                @Override
                public List<Message> multipartBody() {
                    return body;
                }

                @Override
                public Header header() {
                    return altHeader;
                }
            };
        } else {
            /* Store the body unchanged, and reference as a binary
             * message, using only header modificaions made before
             * checking media type. */
            Header altHeader = headerMod.apply();
            BinaryBody body = morgue.store(in);
            return BinaryMessage.of(altHeader, body);
        }
    }

    /**
     * Store a MIME multipart message body.
     * 
     * @param in the multipart body as a byte stream, which is not
     * closed after use
     * 
     * @param boundary the boundary separating the parts
     * 
     * @param assumedCharset the character encoding assumed for text
     * messages when unspecified
     * 
     * @return an immutable sequence of messages allowing retrieval of
     * the parts
     * 
     * @throws IOException if an I/O error occurs in storing the message
     * body
     */
    public List<Message> parseMultipartBody(InputStream in, String boundary,
                                            Charset assumedCharset)
        throws IOException {
        List<Message> result = new ArrayList<>();
        var recognizer = new MultipartBoundaryRecognizer(boundary);
        int mode = 0;
        for (var shadow : BoundarySequence.ofUnclosed(in, recognizer, 80)) {
            try (var subin = shadow) {
                if (mode == 0) {
                    /* Skip the preamble. */
                    mode = 1;
                } else if (mode == 1) {
                    Message sub = parseMessage(subin, assumedCharset);
                    result.add(sub);

                    /* Cause the postscript to be skipped. */
                    if (recognizer.isTerminal()) mode = 2;
                }
            }
        }
        return List.copyOf(result);
    }

    private static final Logger logger =
        Logger.getLogger(MessageParser.class.getPackageName());

    /**
     * @undocumented
     */
    public static void main(String[] args) throws Exception {
        Morgue morgue = SmartMorgue.start().singleThreshold(100)
            .memoryThreshold(1000).build();
        MessageParser parser = new MessageParser(morgue);
        {
            Message msg =
                parser.parseMessage(System.in, StandardCharsets.US_ASCII);
            if (msg instanceof MultipartMessage mpmsg) {
                Header hdr = mpmsg.header();
                for (String name : hdr.names()) {
                    System.out.printf("%s: %s%n", escape(name), escape(hdr
                        .get(name, Format.LAST_STRING_FORMAT)));
                }
                // List<Message> parts = mpmsg.multipartBody();
                // TODO: Print these out?
            }
            msg = null;
        }
        System.err.println("Complete");
        Thread.sleep(Duration.ofSeconds(10));
        System.err.println("Going out of scope");
        System.gc();
        Thread.sleep(Duration.ofSeconds(10));
    }

    private static String escape(CharSequence text) {
        if (text == null) return null;
        StringBuilder result = new StringBuilder();
        final int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            switch (c) {
            case '\r' -> result.append("\\r");

            case '\t' -> result.append("\\t");

            case '\n' -> result.append("\\n");

            case '\\' -> result.append("\\\\");

            default -> result.append(c);
            }
        }
        return result.toString();
    }
}
