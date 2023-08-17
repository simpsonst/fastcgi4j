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

package uk.ac.lancs.fastcgi.misc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import uk.ac.lancs.fastcgi.context.RequestableSession;
import uk.ac.lancs.fastcgi.mime.Disposition;
import uk.ac.lancs.fastcgi.mime.MediaType;
import uk.ac.lancs.fastcgi.mime.Message;
import uk.ac.lancs.fastcgi.mime.MessageParser;
import uk.ac.lancs.fastcgi.mime.StringMessage;

/**
 * Presents form data as a list or a map.
 *
 * @author simpsons
 */
public final class FormSubmission {
    private final List<Map.Entry<? extends String, ? extends Message>> list;

    private final Map<String, List<Message>> map;

    private FormSubmission(List<? extends Map.Entry<? extends String,
                                                    ? extends Message>> list) {
        this.list = List.copyOf(list);

        /**
         * Create an immutable index of the data.
         */
        Map<String, List<Message>> map = new HashMap<>();
        for (var e : list) {
            var k = e.getKey();
            var v = e.getValue();
            map.computeIfAbsent(k, dummy -> new ArrayList<>()).add(v);
        }
        this.map = map.entrySet().stream().collect(Collectors
            .toMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
    }

    /**
     * Load form data from a multipart message body.
     * 
     * @param mpm the parts of the multipart message
     * 
     * @return the requested form data
     */
    public static FormSubmission fromMultipart(List<? extends Message> mpm) {
        List<Map.Entry<? extends String, ? extends Message>> list =
            new ArrayList<>();
        collectFromMultipart(list, mpm);
        return new FormSubmission(list);
    }

    /**
     * Collect fields from a multipart message body.
     * 
     * @param dest the destination for detected fields
     * 
     * @param mpm the parts of the multipart message
     */
    private static void
        collectFromMultipart(List<Map.Entry<? extends String,
                                            ? extends Message>> dest,
                             List<? extends Message> mpm) {
        for (Message msg : mpm) {
            Disposition disp =
                msg.header().get("Content-Disposition", Disposition.FORMAT);
            if (disp == null) continue;
            if (!disp.type().equals("form-data")) continue;
            String name = disp.name();
            if (name == null) continue;
            dest.add(Map.entry(name, msg));
        }
    }

    private static final Pattern PCENC = Pattern.compile("%([0-9A-F]{2})+");

    private static int hexval(char c) {
        return switch (c) {
        case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> c - '0';
        case 'A', 'B', 'C', 'D', 'E', 'F' -> c - 'A';
        default -> -1;
        };
    }

    private static final class PercentDecoder {
        private final Charset charset;

        private byte[] buf = new byte[10];

        public PercentDecoder(Charset charset) {
            this.charset = charset;
        }

        public String decode(CharSequence input) {
            StringBuilder result = new StringBuilder();
            int last = 0;
            for (Matcher m = PCENC.matcher(input); m.find();) {
                result.append(input.subSequence(last, m.start()));
                final int slen = (m.end() - m.start()) / 3;
                if (slen > buf.length) {
                    final int nlen = slen + 12;
                    buf = new byte[nlen];
                }
                for (int j = 0, i = m.start(); i < m.end(); j++, i += 3)
                    buf[j] = (byte) ((hexval(input.charAt(i + 1)) << 4) |
                        hexval(input.charAt(i + 2)));
                result.append(new String(buf, 0, slen, charset));
            }
            result.append(input.subSequence(last, input.length()));
            return result.toString();
        }
    }

    private static final Pattern AMPS = Pattern.compile("&");

    /**
     * Load form data from a query string.
     * 
     * @param qs the query string
     * 
     * @param charset the encoding to assume for percent-encoded
     * characters
     * 
     * @return the form submission equivalent to the provided query
     * string
     */
    public static FormSubmission fromQuery(CharSequence qs, Charset charset) {
        List<Map.Entry<? extends String, ? extends Message>> list =
            new ArrayList<>();
        collectFieldsFromQuery(list, qs, charset);
        return new FormSubmission(list);
    }

    /**
     * Collect fields from a query string. No fields are added if the
     * query string is {@code null}.
     * 
     * @param dest the destination for detected fields
     * 
     * @param qs the query string
     * 
     * @param charset the encoding to assume for percent-encoded
     * characters
     */
    private static void
        collectFieldsFromQuery(List<? super Map.Entry<? extends String,
                                                      ? extends Message>> dest,
                               CharSequence qs, Charset charset) {
        if (qs == null) return;
        PercentDecoder dec = new PercentDecoder(charset);

        /* Split the query string on ampersands. */
        String[] bits = AMPS.split(qs);
        for (String bit : bits) {
            int eq = bit.indexOf('=');
            if (eq < 0) {
                /* TODO: Maybe log this? */
                continue;
            }
            String name = dec.decode(bit.subSequence(0, eq));
            String value = dec.decode(bit.subSequence(eq + 1, bit.length()));
            StringMessage msg = new StringMessage(value);
            dest.add(Map.entry(name, msg));
        }
    }

    /**
     * Load form data from the session. Three sources are checked:
     * 
     * <ul>
     * 
     * <li>If the request method is <samp>GET</samp> or
     * <samp>HEAD</samp>, the <samp>QUERY_STRING</samp> parameter is
     * parsed using {@link #fromQuery(CharSequence, Charset)}.</li>
     * 
     * <li>Otherwise, <samp>CONTENT_TYPE</samp> is checked for
     * <samp>application/x-www-form-urlencoded</samp>. If set, the
     * request body is parsed as a query string.</li>
     * 
     * <li>Finally, if <samp>CONTENT_TYPE</samp> is multipart, the
     * request body is parsed as a MIME multipart message.
     * 
     * </ul>
     * 
     * @param session the request session providing the method, query
     * string and request body
     * 
     * @param assumedCharset the character encoding assumed to be used
     * by the client in forming plain-text field values
     * 
     * @param morgue a place to store bodies
     * 
     * @return the submitted form field values; or {@code null} if no
     * form delivery mechanism was recognized
     * 
     * @throws IOException
     */
    public static FormSubmission fromSession(RequestableSession session,
                                             Charset assumedCharset,
                                             MessageParser parser)
        throws IOException {
        final String qs = session.parameters().get("QUERY_STRING");
        final List<Map.Entry<? extends String, ? extends Message>> list =
            new ArrayList<>();
        collectFieldsFromQuery(list, qs, assumedCharset);

        final String rm = session.parameters().get("REQUEST_METHOD");
        switch (rm) {
        case "GET":
        case "HEAD":
            /* There is no request body to get fields from. */
            break;

        default:
            /* Get the media type of the content. */
            final MediaType mt =
                MediaType.fromString(session.parameters().get("CONTENT_TYPE"));

            if (mt.is("application", "x-www-form-urlencoded")) {
                /* The message body is a simple query string. */
                final String text;
                try (StringWriter out = new StringWriter();
                     InputStreamReader in =
                         new InputStreamReader(session.in(),
                                               StandardCharsets.US_ASCII)) {
                    in.transferTo(out);
                    text = out.toString();
                }
                collectFieldsFromQuery(list, text, assumedCharset);
            } else if (mt.isMultipart()) {
                final String boundary = mt.parameter("boundary");
                List<Message> parts = parser
                    .parseMultipartBody(session.in(), boundary, assumedCharset);
                collectFromMultipart(list, parts);
            }
        }

        return new FormSubmission(list);
    }

    /**
     * Get the immutable list view of the form data.
     * 
     * @return the list view of the form data
     */
    public List<Map.Entry<? extends String, ? extends Message>> list() {
        return list;
    }

    /**
     * Get the map view of the form data.
     * 
     * @return the map view of the form data
     */
    public Map<String, List<Message>> map() {
        return map;
    }

    /**
     * Get the first message with a given name.
     * 
     * @param name the form field name
     * 
     * @return the first message with the requested name; or
     * {@code null} if no fields exist with that name
     */
    public Message getFirst(String name) {
        List<Message> vals = map().get(name);
        if (vals == null || vals.isEmpty()) return null;
        return vals.get(0);
    }
}
