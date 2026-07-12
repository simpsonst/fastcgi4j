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
 *  Author: Steven Simpson <s.simpson@lancaster.ac.uk>
 */

package uk.ac.lancs.http;

import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import uk.ac.lancs.io.UnclosedOutputStream;

/**
 * Writes a chunked message to an output stream. Each write operation on
 * this stream generates a chunk. No trailer is generated. When closed,
 * the base stream is closed. Use {@link UnclosedOutputStream} to
 * prevent that.
 *
 * @author simpsons
 */
public class ChunkedOutputStream extends FilterOutputStream {
    private static final byte[] CRLF = { 13, 10 };

    /* TODO: Allow the user to specify parameters to place in the chunk
     * header. */

    /**
     * Create a chunked output stream.
     * 
     * @param out the base stream
     */
    public ChunkedOutputStream(OutputStream out) {
        super(out);
    }

    private boolean closed = false;

    /**
     * Close this stream. A final zero-length chunk is transmitted,
     * i.e., an ASCII 0 (U+0048) and CRLF. The base stream is not
     * closed, but it is flushed. Calling this method more than once has
     * no additional effect.
     * 
     * @throws IOException if an I/O error occurs in writing the
     * terminal chunk
     */
    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        out.write(48);
        out.write(CRLF);
        out.close();
    }

    /**
     * Write part of an array as a chunk. The chunk length is converted
     * to hexadecimal and then to ASCII, then written with a following
     * CRLF. Then the array payload is written, followed by another
     * CRLF. Attempting to write a chunk of length 0 has no effect.
     * 
     * @param b the array containing the chunk payload
     * 
     * @param off the offset into the array of the first byte of the
     * payload
     * 
     * @param len the number of bytes of the payload
     * 
     * @throws IOException if an I/O error occurs in writing the chunk
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len < 0) throw new IllegalArgumentException("-ve len");
        if (len == 0) return;
        if (closed) throw new IOException("closed");
        out.write(Integer.toString(len, 16)
            .getBytes(StandardCharsets.US_ASCII));
        out.write(CRLF);
        out.write(b, off, len);
        out.write(CRLF);
    }

    /**
     * Write a single-byte chunk. A US-ASCII 1 (U+0049) is transmitted,
     * followed by CRLF, the supplied byte, and another CRLF.
     * 
     * <p>
     * Use of this method is highly inefficient. Collect several bytes
     * together first, and call {@link #write(byte[], int, int)}
     * instead. Wrapping a {@link BufferedOutputStream} around this
     * stream might help to do this automatically.
     * 
     * @param b the byte to be transmitted
     * 
     * @throws IOException if an I/O error occurs in writing the chunk
     */
    @Override
    public void write(int b) throws IOException {
        if (closed) throw new IOException("closed");
        out.write(49);
        out.write(CRLF);
        out.write(b);
        out.write(CRLF);
    }
}
