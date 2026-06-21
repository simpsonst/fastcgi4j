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
import java.util.HexFormat;
import java.util.Map;
import java.util.function.ObjLongConsumer;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Reads and decodes a chunked input stream. When closed, the base
 * stream is also closed. Use {@link UnclosedInputStream} so that the
 * base stream is left in a position to read the trailer.
 * 
 * @author simpsons
 */
public class ChunkedInputStream extends FilterInputStream {
    private static final int MAX_CHUNK_HEADER = 1024;

    private final ObjLongConsumer<? super Map<String, String>> paramConsumer;

    /**
     * Prepare to decode a chunked input stream, and act on chunk
     * parameters.
     * 
     * @param base the base stream, which is in chunked format
     * 
     * @param paramConsumer to be invoked when chunk parameters are
     * parsed, and including the total number of bytes delivered so far
     */
    public ChunkedInputStream(InputStream base,
                              ObjLongConsumer<? super Map<String,
                                                          String>> paramConsumer) {
        super(base);
        this.paramConsumer = paramConsumer;
    }

    /**
     * Prepare to decode a chunked input stream.
     * 
     * @param base the base stream, which is in chunked format
     */
    public ChunkedInputStream(InputStream base) {
        super(base);
        this.paramConsumer = (x, y) -> {};
    }

    /**
     * The number of bytes remaining in the current chunk. When zero,
     * and {@link #terminated} is false, (the remainder of) a chunk
     * header line is expected.
     */
    private long remaining = 0;

    /**
     * Records the exception that prevented successful parsing.
     */
    private IOException abortion = null;

    /**
     * Form an I/O exception with the given message format an arguments,
     * record it, and throw it.
     * 
     * @param msg the message format for the exception
     * 
     * @param args arguments to be applied to the message format
     * 
     * @throws IOException always
     */
    private void abort(String msg, Object... args) throws IOException {
        abortion = new IOException(msg.formatted(args));
        throw abortion;
    }

    /**
     * Check if parsing has been aborted.
     * 
     * @throws IOException if parsing has been aborted, including the
     * original cause
     */
    private void checkAbortion() throws IOException {
        if (abortion != null) throw new IOException("aborted", abortion);
    }

    /**
     * Records how much space is required for the CRLF chunk terminator
     * of the current chunk. Initially, this is zero, as there is no
     * current chunk, but it becomes 2 after the first chunk header is
     * parsed.
     */
    private int termSpace = 0;

    /**
     * Holds a (partial) chunk header line. {@link #lineLength}
     * indicates how many bytes are occupied from position 0.
     */
    private byte[] line = new byte[128];

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

    private long total = 0;

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

        /* Attempt to read the length line and preceding chunk
         * terminator (CRLF). Stop when we have the minimum number of
         * bytes, ending with CRLF. */
        int len = 0;
        while (len < termSpace + 2 || line[len - 2] != 13 ||
            line[len - 1] != 10) {

            /* Use a bigger buffer if necessary. */
            if (len == line.length) {
                /* Check for a practical upper limit, and abort if
                 * already reached. */
                if (line.length >= MAX_CHUNK_HEADER) {
                    abort("excessive chunk header: (%d) %s", len,
                          HexFormat.of().formatHex(line, 0, len));
                    throw new AssertionError("unreachable");
                }
                line =
                    Arrays.copyOf(line, Integer.min(len * 2, MAX_CHUNK_HEADER));
            }

            /* Read and store the byte. EOF here is a fatal error. */
            int rc = in.read();
            if (rc < 0) {
                abort("input terminated mid-header: %s",
                      HexFormat.of().formatHex(line, 0, len));
                throw new AssertionError("unreachable");
            }
            line[len++] = (byte) rc;
        }

        /* Abort if we don't have enough bytes for 2xCRLF. */
        if (len < termSpace + 2) {
            abort("bad chunk separator: %s",
                  HexFormat.of().formatHex(line, 0, len));
            throw new AssertionError("unreachable");
        }

        /* Abort if a CRLF terminator is expected but not present. */
        if (termSpace == 2) {
            if (line[0] != 13 || line[1] != 10) {
                abort("bad chunk terminator: %s",
                      HexFormat.of().formatHex(line, 0, len));
                throw new AssertionError("unreachable");
            }
        }

        /* Extract the text between the CRLFs, and remember that all
         * subsequent chunk separators begin with a CRLF terminator. */
        String text = new String(line, termSpace, len - 2 - termSpace,
                                 StandardCharsets.US_ASCII);
        termSpace = 2;

        /* Parse the length line as a hex number followed by optional
         * parameters. */
        var tokens = new Tokenizer(text);
        int dig;
        while ((dig = tokens.character(DIGIT_CHARS)) >= 0) {
            remaining *= 16;
            if (dig >= 16) dig -= 6;
            remaining += dig;
        }
        final Map<String, String> exts = new HashMap<>();
        if (!tokens.parameters(exts)) {
            abort("bad chunk header: %s", text);
            throw new AssertionError("unreachable");
        }
        paramConsumer.accept(exts, total);

        /* Detect and report EOF. */
        if (remaining == 0) {
            terminated = true;
            return true;
        }

        /* There is still some data to read. */
        return false;
    }

    /**
     * Read a byte.
     * 
     * <p>
     * If a chunk header is processed, the parameters and current offset
     * into the content are passed to the configured consumer.
     * 
     * @return the next byte as an unsigned value; or {@code -1} on EOF
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        if (closed) throw new IOException("closed");
        checkAbortion();
        if (getRemaining()) return -1;
        int rc = in.read();
        if (rc < 0) {
            abort("input terminated after %d with %d of chunk remaining", total,
                  remaining);
            throw new AssertionError("unreachable");
        }
        remaining--;
        total++;
        return rc;
    }

    /**
     * Read bytes into part of an array.
     * 
     * <p>
     * If a chunk header is processed, the parameters and current offset
     * into the content are passed to the configured consumer.
     * 
     * @param b the array to populate
     * 
     * @param off the index into the array of the first byte to read
     * into
     * 
     * @param len the maximum number of bytes to read
     * 
     * @return the number of bytes read; or {@code -1} on EOF
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) throw new IOException("closed");
        checkAbortion();
        if (getRemaining()) return -1;
        assert remaining > 0;
        int got = in.read(b, off, (int) Long.min(len, remaining));
        if (got < 0) {
            abort("input terminated after %d with %d of chunk remaining", total,
                  remaining);
            throw new AssertionError("unreachable");
        }
        remaining -= got;
        total += got;
        return got;
    }

    /**
     * Discard bytes from the stream.
     * 
     * <p>
     * If a chunk header is processed, the parameters and current offset
     * into the content are passed to the configured consumer.
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
        checkAbortion();
        if (getRemaining()) return -1;
        assert remaining > 0;
        long got = in.skip(Long.min(n, remaining));
        remaining -= got;
        return got;
    }

    /**
     * Close the stream. The base stream continues to be read until the
     * final chunk header is received.
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

        in.close();
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
        checkAbortion();
        return (int) Long.min(in.available(), remaining);
    }
}
