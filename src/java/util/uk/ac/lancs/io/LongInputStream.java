// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2026, Lancaster University
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

package uk.ac.lancs.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Provides an infinite-length stream of bytes from a source of
 * {@code long}s. The implementation retains fragments of the bytes of a
 * {@code long} that are yet to be used.
 * 
 * <p>
 * Do not wrap a {@link CompletingInputStream} directly around this one,
 * as it never completes!
 *
 * @author simpsons
 */
public final class LongInputStream extends InputStream {
    private final LongSupplier source;

    private final Runnable closeAction;

    private boolean closed = false;

    /**
     * Create an input stream from the bytes of an infinite sequence of
     * {@code long}s, and specify an action to be taken on close.
     * 
     * @param source the source of {@code long}s
     * 
     * @param closeAction an action to be invoked at most once when the
     * stream is closed; or {@code null} if no action is to be taken
     */
    public LongInputStream(LongSupplier source, Runnable closeAction) {
        this.source = source;
        this.closeAction = closeAction == null ? () -> {} : closeAction;
    }

    /**
     * Create an input stream from the bytes of an infinite sequence of
     * {@code long}s.
     * 
     * @param source the source of {@code long}s
     * 
     * @param closeAction an action to be invoked at most once when the
     * stream is closed
     */
    public LongInputStream(LongSupplier source) {
        this.source = source;
        this.closeAction = () -> {};
    }

    private final byte[] buf = new byte[7];

    private int from = buf.length;

    private byte next() {
        assert from == buf.length;
        long r = source.getAsLong();
        while (from > 0) {
            buf[--from] = (byte) r;
            r >>>= 8;
        }
        return (byte) r;
    }

    /**
     * Estimate the number of bytes that can be read without blocking.
     * As the stream is infinite, {@value Integer#MAX_VALUE} is
     * returned.
     * 
     * @return the estimated number of bytes available
     * 
     * @throws IOException if the stream has been closed
     */
    @Override
    public int available() throws IOException {
        if (closed) throw new IOException("closed");
        return Integer.MAX_VALUE;
    }

    /**
     * Read a single byte. Note that this implementation never returns
     * {@code -1} (EOF), as the stream is of infinite length.
     * 
     * @return the byte as an unsigned value
     * 
     * @throws IOException if the stream has been closed
     */
    @Override
    public int read() throws IOException {
        if (closed) throw new IOException("closed");
        return rawRead() & 0xff;
    }

    private byte rawRead() {
        if (from < this.buf.length) return this.buf[from++];
        return next();
    }

    /**
     * Read bytes into part of an array. Note that this implementation
     * never returns {@code -1} (EOF), as the stream is of infinite
     * length.
     * 
     * @param b the buffer into which the bytes shall be written
     * 
     * @param off the index into the array of the first byte to be
     * overwritten
     * 
     * @param len the number of bytes to read
     * 
     * @return the requested number of bytes
     * 
     * @throws IOException if the stream has been closed
     * 
     * @throws IllegalArgumentException if the requested number of bytes
     * is negative
     * 
     * @throws ArrayIndexOutOfBoundsException if the range of bytes is
     * not within the array
     * 
     * @throws NullPointerException if the buffer is {@code null}s
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) throw new IOException("closed");
        Objects.requireNonNull(b, "b");
        if (len < 0) throw new IllegalArgumentException("-ve length " + len);
        if (off < 0 || off > b.length)
            throw new ArrayIndexOutOfBoundsException(off);
        final int lim = off + len;
        if (lim > b.length) throw new ArrayIndexOutOfBoundsException(lim);
        while (from < this.buf.length && off < lim)
            buf[off++] = this.buf[from++];
        while (lim - off >= 8) {
            long r = source.getAsLong();
            for (int i = 8; i > 0;) {
                buf[off + --i] = (byte) r;
                r >>>= 8;
            }
            off += 8;
        }
        while (off < lim)
            buf[off++] = rawRead();
        return len;
    }

    /**
     * Skip a number of bytes. This implementation first consumes any
     * buffered bytes, then consumes whole words (8 bytes each), so that
     * the stream is left on a word boundary, so no bytes remain
     * buffered. However, if the caller asks to skip less than a whole
     * word while already on a word boundary, a new word is taken from
     * the source, and its unused bytes are buffered. This ensures that
     * the number of bytes skipped is never zero. Although there's no
     * requirement for this, it prevents getting stuck in an impassable
     * situation.
     * 
     * @param n the maximum number of bytes to skip
     * 
     * @return the number of bytes skipped, which is always positive
     * 
     * @throws IOException if the stream is closed
     * 
     * @throws IllegalArgumentException if the requested number of bytes
     * is negative
     */
    @Override
    public long skip(long n) throws IOException {
        if (closed) throw new IOException("closed");
        if (n < 0) throw new IllegalArgumentException("-ve length " + n);
        if (n == 0) return 0;
        final long orign = n;

        /* Use up what's in our buffer. */
        var fromBuf = Long.min(n, buf.length - from);
        n -= fromBuf;
        from += fromBuf;

        /* Discard whole words. */
        while (n >= 8) {
            source.getAsLong();
            n -= 8;
        }

        if (n == orign) {
            assert n > 0;
            assert n < 8;
            assert from == buf.length;
            /* We have nothing in the buffer, and we've skipped nothing.
             * We should at least skip the small number of bytes
             * requested, and store the remaining ones of the word. */
            long r = source.getAsLong();
            while (from > n) {
                buf[--from] = (byte) r;
                r >>>= 8;
            }
            n = 0;
        }

        return orign - n;
    }

    /**
     * Close the stream. Further attempts to read or skip bytes will
     * throw an exception. The first call invokes the specified close
     * action.
     */
    @Override
    public void close() {
        if (closed) return;
        closed = true;
        closeAction.run();
    }
}
