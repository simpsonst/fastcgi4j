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

import java.util.List;

/**
 * Retains a MIME multipart message.
 * 
 * @author simpsons
 */
public interface MultipartMessage extends Message {
    /**
     * Get the message's body as a sequence of messages.
     * 
     * @return the message's parts
     */
    List<Message> multipartBody();

    /**
     * Create a multipart message from existing parts.
     * 
     * @param header the message header
     * 
     * @param body the message body, which will be copied
     * 
     * @return the composed message
     */
    static MultipartMessage of(Header header, List<? extends Message> body) {
        List<Message> copy = List.copyOf(body);
        return new MultipartMessage() {
            @Override
            public List<Message> multipartBody() {
                return copy;
            }

            @Override
            public Header header() {
                return header;
            }
        };
    }

    @Override
    default MultipartMessage replaceHeader(Header newHeader) {
        List<Message> body = multipartBody();
        return new MultipartMessage() {
            @Override
            public List<Message> multipartBody() {
                return body;
            }

            @Override
            public Header header() {
                return newHeader;
            }
        };
    }
}
