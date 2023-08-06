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

package uk.ac.lancs.fastcgi.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Splits an input stream into parts separated by certain boundaries.
 *
 * @author simpsons
 */
public final class BoundarySequence implements Iterator<InputStream> {
    private final InputStream base;

    private final BoundaryRecognizer recognizer;

    /**
     * Holds bytes read from the base stream but not yet delivered to
     * the user. Its state is governed by four indices, {@link #start},
     * {@link #cand}, {@link #candEnd} and {@link #lim}, and a Boolean
     * {@link #baseEnded}. Each index is no more than the next,
     * non-negative, and no greater than the length of the buffer. The
     * Boolean is initially {@code false}, but set to {@code true} when
     * the base stream signals EOF.
     * 
     * <p>
     * Bytes earlier than {@link #start} are indeterminate, as are those
     * from {@link #lim}. New bytes are read from the base stream at
     * {@link #lim}, which is then incremented. Bytes are submitted to
     * the user from {@link #start}, which is then incremented. When
     * {@link #lim} reaches <code>{@linkplain #buf}.length</code>, and
     * more bytes are available and required, compaction takes place my
     * shifting all determinate bytes to the start of the buffer. If the
     * buffer is already compact, and more bytes are still required, the
     * buffer is re-allocated. This allows a long boundary candidate to
     * be accommodated.
     * 
     * <p>
     * Bytes from {@link #cand} (inclusive) to {@link #candEnd}
     * (exclusive) constitute either a complete boundary or a prefix of
     * a candidate boundary. If <code>{@linkplain #candEnd} &lt;
     * {@linkplain #lim}</code>, or if they are equal and
     * {@link #baseEnded} is set, then the boundary is complete.
     * Otherwise, <code>{@linkplain #candEnd} ==
     * {@linkplain #lim}</code> and {@link #baseEnded} is clear,
     * indicating only a candidate.
     * 
     * <p>
     * Only bytes between {@link #start} (inclusive) and {@link #cand}
     * (exclusive) can be delivered to the user. By reading more bytes
     * into the buffer, it may be determined that the candidate is
     * false, allowing {@link #cand} to be increased to refer to the
     * next candidate. This then releases more bytes to be delivered to
     * the user.
     */
    private byte[] buf;

    private final boolean closeAfter;

    /**
     * Create a boundary input stream from part of a byte array.
     * 
     * @param base the base stream to read bytes from
     * 
     * @param recognizer the boundary recognizer
     * 
     * @param initBuf the initial buffer size
     */
    private BoundarySequence(InputStream base, BoundaryRecognizer recognizer,
                             int initBuf, boolean closeAfter) {
        this.base = base;
        this.recognizer = recognizer;
        this.closeAfter = closeAfter;
        this.buf = new byte[initBuf];
    }

    /**
     * Create an iteration over the boundary-separated parts of a
     * stream. The result can only be used once. The idiom is:
     * 
     * <pre>
     * {@linkplain InputStream} rawIn = ...;
     * {@linkplain BoundaryRecognizer} recog = ...;
     * for (var shadow : {@linkplain BoundarySequence}.of(rawIn, recog, 1024)) {
     *   try (var in = shadow) {
     *     ...
     *   }
     * }
     * </pre>
     * 
     * <p>
     * The base stream is closed after use.
     * 
     * @param base the base stream to read bytes from
     * 
     * @param recognizer the boundary recognizer
     * 
     * @param initBuf the initial buffer size
     * 
     * @return the requested iteration
     */
    public static Iterable<InputStream>
        of(InputStream base, BoundaryRecognizer recognizer, int initBuf) {
        return () -> new BoundarySequence(base, recognizer, initBuf, true);
    }

    /**
     * Create an iteration over the boundary-separated parts of a
     * stream. The result can only be used once. The idiom is:
     * 
     * <pre>
     * {@linkplain InputStream} rawIn = ...;
     * {@linkplain BoundaryRecognizer} recog = ...;
     * for (var shadow : {@linkplain BoundarySequence}.of(rawIn, recog, 1024)) {
     *   try (var in = shadow) {
     *     ...
     *   }
     * }
     * </pre>
     * 
     * <p>
     * The base stream is <em>not</em> closed after use.
     * 
     * @param base the base stream to read bytes from
     * 
     * @param recognizer the boundary recognizer
     * 
     * @param initBuf the initial buffer size
     * 
     * @return the requested iteration
     */
    public static Iterable<InputStream>
        ofUnclosed(InputStream base, BoundaryRecognizer recognizer,
                   int initBuf) {
        return () -> new BoundarySequence(base, recognizer, initBuf, false);
    }

    /**
     * Locates the first available byte. Earlier bytes (if any) have
     * already been delivered to the user. If non-zero, a compaction may
     * take place. This field is always less than or equal to
     * {@link #cand}, and never negative.
     * 
     * @see #buf
     */
    private int start = 0;

    /**
     * Locates the first byte of a possible boundary. This field is
     * always less than or equal to {@link #candEnd}.
     * 
     * @see #buf
     */
    private int cand = 0;

    /**
     * Locates the end of a possible boundary. When this is less than
     * {@link #lim} (or they are equal, and {@link #baseEnded} is
     * {@code true}), a definite boundary has been found. Otherwise, it
     * equals {@link #lim}, and {@link #baseEnded} is {@code false}, so
     * the end of the buffer forms the prefix of a candidate boundary.
     * This field is always less than or equal to {@link #lim}.
     * 
     * @see #buf
     */
    private int candEnd = 0;

    /**
     * Locates the first unused byte of the buffer after the data. If
     * more date must be read, and this matches the length of the
     * buffer, either a compaction is necessary (if {@link #start} is
     * not zero) or the buffer must be expanded. This field is always
     * less than or equal to the length of the buffer.
     * 
     * @see #buf
     */
    private int lim = 0;

    /**
     * Indicates whether the base stream has reported EOF.
     */
    private boolean baseEnded = false;

    /**
     * Ensure there's data in the buffer, and information about the
     * nearest boundary candidate is up-to-date.
     * 
     * @return {@code true} if at least one byte can be read from the
     * current part; {@code false} otherwise
     * 
     * @throws IOException if an error occurs reading from the base
     * stream
     */
    private boolean populate() throws IOException {
        /* If there's data already available, there's nothing more to
         * do. */
        while (start == cand && !baseEnded &&
            (candEnd == lim || candEnd == cand)) {
            /* Attempt to fill the remainder of the buffer. */
            int rem = buf.length - lim;
            if (rem == 0) {
                if (start > 0) {
                    /* Try to compact the buffer. Move the active part
                     * to the start. */
                    if (false) System.err.printf("Compacting: %d, %d, %d, %d%n",
                                                 start, cand, candEnd, lim);
                    lim -= start;
                    System.arraycopy(buf, start, buf, 0, lim);
                    cand -= start;
                    candEnd -= start;
                    start = 0;
                } else {
                    /* We need a bigger boat! Re-allocate. */
                    int newSize = buf.length + buf.length / 2 + 1;
                    if (false) System.err
                        .printf("Re-allocating: %d, %d, %d, %d -> %d%n", start,
                                cand, candEnd, lim, newSize);
                    byte[] buf = new byte[newSize];
                    lim -= start;
                    System.arraycopy(this.buf, start, buf, 0, lim);
                    cand -= start;
                    candEnd -= start;
                    start = 0;
                    this.buf = buf;
                }
            }
            rem = buf.length - lim;
            assert rem > 0;
            do {
                if (false) System.err.printf("Reading %d: %d, %d, %d, %d%n",
                                             rem, start, cand, candEnd, lim);
                int got = base.read(buf, lim, rem);
                if (got < 0) {
                    if (false) System.err.printf("Closing base%n");
                    baseEnded = true;
                    base.close();
                } else {
                    lim += got;
                    rem -= got;
                    if (false)
                        System.err.printf("Got %d: %d, %d, %d, %d%n", got,
                                          start, cand, candEnd, lim);
                }
            } while (rem > 0 && !baseEnded);
            recognize();
        }
        return start < cand;
    }

    /**
     * Determines whether we're offering the very start of the stream to
     * the recognizer. It is initially {@code true}, but is reset when
     * the user reads at least one byte, or the recognizer tells us to
     * skip over at least one byte.
     */
    private boolean atStart = true;

    private void recognize() {
        boolean go = true;
        while (go && cand < lim) {
            int rc = recognizer.recognize(buf, cand, candEnd, lim, !baseEnded,
                                          atStart);
            assert rc >= -(lim - cand) && rc <= lim - candEnd : "rc=" + rc
                + " not in [" + -(lim - cand) + ", " + (lim - candEnd) + "]";
            if (rc < 0) {
                candEnd = cand -= rc;
                atStart = false;
            } else if (rc > 0) {
                candEnd += rc;
                if (candEnd < lim || baseEnded) go = false;
            } else {
                go = false;
            }
            if (false) System.err.printf("Recognized %d: %d, %d, %d, %d%n", rc,
                                         start, cand, candEnd, lim);
        }
    }

    private class PartStream extends InputStream {
        @Override
        public long transferTo(OutputStream out) throws IOException {
            /* Consume remaining data. */
            long transfered = 0;
            while (populate()) {
                int amount = cand - start;
                out.write(buf, start, amount);
                transfered += amount;
                start = cand;
            }
            return transfered;
        }

        @Override
        public void close() throws IOException {
            if (false) System.err.printf("Closing: %d, %d, %d, %d%n", start,
                                         cand, candEnd, lim);
            /* Discard remaining data. */
            while (populate())
                start = cand;
            if (false) System.err.printf("Discarded: %d, %d, %d, %d%n", start,
                                         cand, candEnd, lim);

            /* Consume the terminator if present. */
            assert start == cand;
            start = cand = candEnd;
            recognize();

            /* Allow another part to proceed. */
            drop();
            if (false) System.err.printf("Passed on: %d, %d, %d, %d%n", start,
                                         cand, candEnd, lim);
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = 0;
            while (n > 0 && populate()) {
                /* Discard up to n bytes, or whatever remains of the
                 * current part, whichever is smaller. */
                int amount = (int) Math.min(n, cand - start);
                skipped += amount;
                start = cand;
            }
            return skipped;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) return 0;

            /* Ensure more bytes of the current part are available, then
             * copy as much of the first section to the caller's
             * buffer. */
            if (!populate()) return -1;
            int amount = Math.min(len, cand - start);
            assert amount > 0;
            atStart = false;
            System.arraycopy(buf, start, b, off, amount);
            start += amount;
            return amount;
        }

        @Override
        public int read() throws IOException {
            if (!populate()) return -1;
            atStart = false;
            return buf[start++] & 0xff;
        }
    }

    private boolean ready = true;

    private boolean moreParts() {
        return start < cand || candEnd < lim || !baseEnded;
    }

    private void drop() {
        ready = true;
    }

    /**
     * Get a stream to read the next part.
     * 
     * @return a stream providing the next part
     * 
     * @throws NoSuchElementException if there is no next part
     * 
     * @throws IllegalStateException if called while the current stream
     * is still open
     */
    @Override
    public InputStream next() {
        if (!ready)
            throw new IllegalStateException("current stream in progress");
        if (moreParts()) {
            ready = false;
            return new PartStream();
        }
        throw new NoSuchElementException();
    }

    /**
     * Test whether another part can be obtained from the base stream.
     * 
     * @return {@code true} if another part can be obtained;
     * {@code false} otherwise
     * 
     * @throws IllegalStateException if called while the current stream
     * is still open
     */
    @Override
    public boolean hasNext() {
        if (!ready)
            throw new IllegalStateException("current stream in progress");
        return moreParts();
    }
}
