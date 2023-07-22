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
import java.util.ArrayList;
import java.util.List;
import uk.ac.lancs.fastcgi.io.BoundarySequence;

/**
 * Stores bodies for later retrieval.
 *
 * @author simpsons
 */
public interface Morgue {
    /**
     * Store a byte stream. The stream is not closed after use. The call
     * only returns after storing the entire stream.
     * 
     * @param data the source stream
     * 
     * @return a means to recover the data
     * 
     * @throws IOException if an I/O error occurs in storing the stream
     */
    BinaryBody store(InputStream data) throws IOException;

    /**
     * Store a character stream. The stream is not closed after use. The
     * call only returns after storing the entire stream.
     * 
     * @param data the source stream
     * 
     * @return a means to recover the data
     * 
     * @throws IOException if an I/O error occurs in storing the stream
     */
    TextBody store(Reader data) throws IOException;

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
     * @return a means to recover the message and its headers
     * 
     * @throws IOException if an I/O error occurs in storing the message
     */
    default Message storeMessage(InputStream in) throws IOException {
        /* Read in the header, and get the content type. */
        Header header = new Header(in);
        MediaType contentType = header.getMediaType();
        if (contentType.isMultipart()) {
            String boundary = contentType.parameter("boundary");
            List<Message> body = storeMultipartBody(in, boundary);
            return new MultipartMessage() {
                @Override
                public List<Message> multipartBody() {
                    return body;
                }

                @Override
                public Header header() {
                    return header;
                }
            };
        } else if (contentType.isText()) {
            String csText = contentType.parameter("charset");
            Charset cs = csText == null ? StandardCharsets.US_ASCII :
                Charset.forName(csText);
            Reader cin = new InputStreamReader(in, cs);
            TextBody body = store(cin);
            return new TextMessage() {
                @Override
                public TextBody textBody() {
                    return body;
                }

                @Override
                public Header header() {
                    return header;
                }
            };
        } else {
            BinaryBody body = store(in);
            return new BinaryMessage() {
                @Override
                public BinaryBody body() {
                    return body;
                }

                @Override
                public Header header() {
                    return header;
                }
            };
        }
    }

    /**
     * Store a MIME multipart message body.
     * 
     * @param in the multipart body as a byte stream
     * 
     * @param boundary the boundary separating the parts
     * 
     * @return a sequence of messages allowing retrieval of the parts
     * 
     * @throws IOException if an I/O error occurs in storing the message
     * body
     */
    default List<Message> storeMultipartBody(InputStream in, String boundary)
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
                    Message sub = storeMessage(subin);
                    result.add(sub);

                    /* Cause the postscript to be skipped. */
                    if (recognizer.isTerminal()) mode = 2;
                }
            }
        }
        return List.copyOf(result);
    }
}
