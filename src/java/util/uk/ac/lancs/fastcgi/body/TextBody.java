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

package uk.ac.lancs.fastcgi.body;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

/**
 * Stores a potentially large character sequence.
 * 
 * @author simpsons
 */
public interface TextBody {
    /**
     * Get the body size in characters.
     * 
     * @return the body size
     */
    long size();

    /**
     * Open a new character stream of the body's content.
     * 
     * @return the new stream
     * 
     * @throws IOException if there's an error retrieving the content
     */
    Reader recover() throws IOException;

    /**
     * Get a copy of the body as a string. As strings are immutable, the
     * method may return the same object on multiple calls.
     * 
     * @return the body as a string
     * 
     * @throws IOException if there's an error retrieving the content
     * 
     * @default This implementation calls {@link #size()} to see whether
     * it can reasonably fit the string in memory. It then calls
     * {@link #recover()} to build a new string.
     */
    default String get() throws IOException {
        final long sz = size();

        /* TODO: Choose a smaller size? */
        if (sz > Integer.MAX_VALUE)
            throw new IOException("body size too great for literal: " + sz);
        try (StringWriter out = new StringWriter((int) sz);
             Reader in = recover()) {
            in.transferTo(out);
            return out.toString();
        }
    }
}
