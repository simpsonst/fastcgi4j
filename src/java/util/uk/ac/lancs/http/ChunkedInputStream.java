// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2024, Lancaster University
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

package uk.ac.lancs.http;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Reads and decodes a chunked input stream. When closed, the base
 * stream is not closed, but left in a position to read the trailer.
 * 
 * @author simpsons
 */
public class ChunkedInputStream extends FilterInputStream {
    /**
     * Prepare to decode a chunked input stream.
     * 
     * @param base the base stream, which is in chunked format
     */
    public ChunkedInputStream(InputStream base) {
        super(base);
    }

    /**
     * The number of bytes remaining in the current chunk. When zero,
     * and {@link #terminated} is false, (the remainder of) a chunk
     * header line is expected.
     */
    private long remaining = 0;

    /**
     * Holds a (partial) chunk header line. {@link #lineLength}
     * indicates how many bytes are occupied from position 0.
     */
    private byte[] line = new byte[128];

    /**
     * Specifies the number of bytes already read from the chunk header
     * line. This indicates the next unused byte of {@link #line}.
     */
    private int lineLength = 0;

    /**
     * Indicates that the base stream has delivered its final (empty)
     * chunk header line. No attempt to read from the base stream must
     * be made once this is set, as the base stream is then in the
     * correct position to read the trailer.
     */
    private boolean terminated = false;

    /**
     * Indicates that the user has closed this stream (not the base).
     * Most calls throw an exception if set. The exception is
     * {@link #close()} itself, which is treated as idempotent.
     */
    private boolean closed = false;

    /**
     * Specifies the characters that form the chunk length in a header.
     * The index of each character gives its value, except for those
     * from position 16 onwards, whose values are 6 less than their
     * position.
     */
    private static final String DIGIT_CHARS = "0123456789abcdefABCDEF";

    /**
     * Provides a dumping ground for ignored chunk extensions.
     */
    private static final Map<String, String> exts = new HashMap<>();

    /**
     * Ensure we know how many bytes remain in the current chunk. If we
     * don't know, attempt to complete a chunk header line, which
     * consists of a hex number and optional extensions.
     * 
     * @return {@code true} if the final chunk header has been read;
     * {@code false} otherwise
     * 
     * @throws IOException if the chunk header is terminated by the end
     * of the underlying stream, or it has a bad format
     */
    private boolean getRemaining() throws IOException {
        if (terminated) return true;
        if (remaining > 0) return false;

        /* Attempt to complete the length line. */
        while (lineLength < 2 || line[lineLength - 2] != 13 ||
            line[lineLength - 1] != 10) {
            /* Use a bigger buffer if necessary. */
            if (lineLength == line.length)
                line = Arrays.copyOf(line, lineLength * 2);
            int rc = in.read();
            if (rc < 0) throw new IOException("input terminated mid-header");
            line[lineLength++] = (byte) rc;
        }
        String text =
            new String(line, 0, lineLength - 2, StandardCharsets.US_ASCII);
        var tokens = new Tokenizer(text);
        int dig;
        while ((dig = tokens.character(DIGIT_CHARS)) >= 0) {
            remaining *= 16;
            if (dig >= 16) dig -= 6;
            remaining += dig;
        }
        if (!tokens.parameters(exts))
            throw new IOException("bad chunk header: " + text);
        lineLength = 0;
        if (remaining == 0) {
            terminated = true;
            return true;
        }
        return false;
    }

    @Override
    public int read() throws IOException {
        if (closed) throw new IOException("closed");
        if (getRemaining()) return -1;
        int rc = in.read();
        if (rc < 0) throw new IOException("input terminated mid-chunk");
        remaining--;
        return rc;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) throw new IOException("closed");
        if (getRemaining()) return -1;
        assert remaining > 0;
        int got = in.read(b, off, (int) Long.min(len, remaining));
        if (got < 0) throw new IOException("input terminated mid-chunk");
        remaining -= got;
        return got;
    }

    /**
     * Discard bytes from the stream.
     * 
     * @param n the maximum number of bytes to skip
     * 
     * @return the number of bytes skipped, which could be 0
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long skip(long n) throws IOException {
        if (closed) throw new IOException("closed");
        if (getRemaining()) return -1;
        assert remaining > 0;
        long got = in.skip(Long.min(n, remaining));
        remaining -= got;
        return got;
    }

    /**
     * Close the stream. The base stream continues to be read until the
     * final chunk header is received. It is then left open in a
     * position to read the trailer.
     * 
     * <p>
     * Calling close more than once has no further effect.
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;

        /* Read and discard the remaining data. */
        while (!getRemaining()) {
            long got = in.skip(remaining);
            remaining -= got;
        }
    }

    /**
     * Get an estimate of the number of bytes that may be read without
     * blocking. The estimate is the minimum of the current chunk's
     * remaining size and the underlying stream's own estimate. It could
     * be zero if the current chunk is complete but the next header has
     * not yet been completely read. No attempt is made to read that
     * header, to avoid making this a blocking call.
     * 
     * @return the estimate
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int available() throws IOException {
        if (closed) throw new IOException("closed");
        return (int) Long.min(in.available(), remaining);
    }
}
