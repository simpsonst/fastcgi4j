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

import uk.ac.lancs.fastcgi.body.TextBody;
import uk.ac.lancs.fastcgi.body.StringBody;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Presents a string literal as a text message. The content type is
 * <samp>text/plain</samp>. An optional disposition as form data with a
 * name can be specified.
 * 
 * @author simpsons
 */
public final class StringMessage implements TextMessage {
    private final Header header;

    private final StringBody body;

    private void addField(Map<String, List<String>> fields, String name,
                          String value) {
        fields.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }

    /**
     * Create a text message from a string.
     * 
     * @param content the content
     */
    public StringMessage(String content) {
        this(null, content);
    }

    /**
     * Create a text message as form data.
     * 
     * @param fieldName the name of the form field
     * 
     * @param content the content
     */
    public StringMessage(String fieldName, String content) {
        this.body = new StringBody(content);
        Header.Modification hdr =
            Header.empty().modify().set("Content-Type", MediaType.FORMAT,
                                        MediaType.of("text", "plain"));
        Map<String, List<String>> fields = new HashMap<>();
        addField(fields, "Content-Type", "text/plain");
        if (fieldName != null)
            hdr.set("Content-Disposition", Disposition.FORMAT,
                    Disposition.formData(fieldName));
        this.header = hdr.apply();
    }

    @Override
    public TextBody textBody() {
        return body;
    }

    @Override
    public Header header() {
        return header;
    }
}
