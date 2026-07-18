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

package uk.ac.lancs.io.infpipe;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Stores content in a byte array. A maximum size is configured. When
 * there are insufficient bytes at the end of the array for a write
 * operation, but there are free bytes at the start because of prior
 * read operations, the contents are shifted to the start of the array
 * for re-use of the space. Changes to the amount of space used are
 * recorded in an atomic counter, allowing the user to decide when to
 * switch to backing store for new chunks.
 * 
 * @author simpsons
 */
final class MemoryChunk implements Chunk {
    private final AtomicLong memoryUsage;

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition ready = lock.newCondition();

    /**
     * Holds bytes received but not yet delivered. The bytes between
     * {@link #readPos} (inclusive) and {@link #writePos} (exclusive)
     * hold the current bytes. When this chunk's content is discarded
     * with {@link #close()}, this array is set to {@code null} to
     * record that state.
     */
    private byte[] array;

    /**
     * Create a chunk storing content in a byte array.
     * 
     * @param memChunkSize the maximum number of bytes to be permitted
     * in the chunk simultaneously
     * 
     * @param memoryUsage a counter to be updated as bytes are added and
     * removed
     */
    public MemoryChunk(int memChunkSize, AtomicLong memoryUsage) {
        this.memoryUsage = memoryUsage;
        this.array = new byte[memChunkSize];
    }

    /**
     * Identifies the first byte of the array that should be read from.
     * The number of bytes available is obtained by subtracting this
     * value from {@link #writePos}.
     * 
     * @see #array
     */
    private int readPos = 0;

    /**
     * Identifies the first byte of the array that should be written to.
     * The maximum number of bytes that can be immediately written is
     * the length of {@link #array} minus this value. Extra space can be
     * made by moving the bytes between {@link #readPos} (inclusive) to
     * {@link #writePos} (exclusive) to position 0, subtracting
     * {@link #readPos} from {@link #writePos}, and then setting
     * {@link #readPos} to zero.
     * 
     * @see #array
     */
    private int writePos = 0;

    /**
     * Indicates whether the chunk's supply of data is complete. If
     * {@code true}, no more data is expected via
     * {@link #write(byte[], int, int)}.
     */
    private boolean complete = false;

    private void check() {
        assert readPos <= writePos;
        assert readPos >= 0;
        assert writePos <= array.length;
    }

    /**
     * {@inheritDoc}
     * 
     * @implNote If there is space at the start of the buffer because
     * bytes there have already been read, the array contents may be
     * shifted to the start, to make more space at the end. As a result,
     * a sufficiently fast reader might prevent the array from filling
     * up, and so obviate the creation of a new chunk. Therefore, the
     * total number of bytes handled by the chunk may be more than the
     * configured limit.
     * 
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public int write(byte[] buf, int off, int len) {
        if (len == 0)
            throw new IllegalArgumentException("non-positive length: " + len);
        try {
            lock.lock();
            /* The content provider should not be supplying more content
             * when they've already said there's no more. */
            if (complete) throw new IllegalStateException("complete");

            /* If the consumer has already indicated they're not longer
             * interested in the content, we can absorb it. */
            if (array == null) return len;

            check();
            try {
                /* Are we empty, and is there any space in the user's
                 * buffer? If so, we can copy bytes directly into the
                 * waiting user's buffer. */
                if (writePos == readPos && userBuf != null) {
                    /* We current have no data (so the reader has caught
                     * up with the writer), and the reader is waiting
                     * right now with a buffer. We should be able to
                     * write bytes directly into that buffer. */

                    /* How many bytes are free in the reader's direct
                     * buffer? */
                    final int rem = userLen - suppliedToUser;
                    assert rem >= 0;

                    /* Get the largest amount that is available and fits
                     * into the reader's direct buffer. */
                    final int amount = Math.min(len, rem);

                    if (amount > 0) {
                        /* Place as much data as we can directly into
                         * the reader's buffer, and mark it as consumed.
                         * Tell the reader that they have data
                         * immediately available. */
                        System.arraycopy(buf, off, userBuf,
                                         userOff + suppliedToUser, amount);
                        suppliedToUser += amount;
                        ready.signal();

                        /* Mark this portion of the incoming data as
                         * accepted. If this is less than the amount
                         * offered, the caller is expected to keep
                         * calling with the remnant, until all of it has
                         * been accepted. */
                        return amount;
                    }
                }

                /* How much space is at the end of the array? */
                final int rem = array.length - writePos;
                final int amount;
                if (len > rem) {
                    /* We don't have enough space in our buffer. Try
                     * shifting the current content to the start of the
                     * buffer. */
                    System.arraycopy(array, readPos, array, 0,
                                     writePos - readPos);
                    writePos -= readPos;
                    readPos = 0;
                    amount = Integer.min(len, array.length - writePos);
                } else {
                    /* Just use what we have left. */
                    amount = Integer.min(len, rem);
                }

                if (amount == 0) {
                    complete = true;
                    ready.signal();
                    return 0;
                }

                /* Consume some of the supplied content. */
                System.arraycopy(buf, off, array, writePos, amount);
                writePos += amount;
                memoryUsage.addAndGet(amount);
                ready.signal();
                return amount;
            } finally {
                check();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void complete() {
        try {
            lock.lock();
            complete = true;
            ready.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Identifies the current reading thread. Only one thread is
     * permitted at any time. Reading operations should use this idiom
     * to hold the right to read:
     * 
     * <pre class="java">
     * {@linkplain claimAsReader claimAsReader()};
     * try {
     *   // ...
     * } finally {
     *   {@linkplain releaseAsReader releaseAsReader()};
     * }
     * </pre>
     */
    private Thread readingThread;

    /**
     * Claim the right to read. {@link #readingThread} must be
     * {@code null} on entry, and is set to the calling thread.
     * 
     * @throws IllegalStateException if another thread holds the right
     */
    private void claimAsReader() {
        assert lock.isHeldByCurrentThread();
        var ct = Thread.currentThread();
        if (readingThread != null)
            throw new IllegalStateException("pipe accessed by multiple threads");
        readingThread = ct;
    }

    /**
     * Release the right to read. {@link #readingThread} is set to null.
     */
    private void releaseAsReader() {
        assert readingThread == Thread.currentThread();
        readingThread = null;
    }

    /**
     * Close the input stream. {@link #array} is set to {@code null} to
     * mark this. Closing twice is not an error.
     */
    void close() {
        try {
            lock.lock();
            array = null;

            /* The memory is no longer in use, so account for it as if
             * it had been delivered. */
            memoryUsage.addAndGet(readPos - writePos);
            readPos = writePos;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Holds the reader's destination buffer. Being non-{@code null}
     * indicates that a thread is calling
     * {@link #read(byte[], int, int)}, and offers its first argument
     * (this field) for a writing thread to place data directly into.
     * The reader must ensure that this field is set back to
     * {@code null} before releasing the lock.
     * 
     * <p>
     * Fields {@link #userOff} and {@link #userLen} further describe
     * which part of the buffer is available for direct writing.
     * {@link #suppliedToUser} is incremented by the writer to show how
     * much has been supplied. These fields are meaningless if this
     * field is {@code null}.
     */
    private byte[] userBuf;

    /**
     * Identifies the first byte of the reader's buffer that can be
     * directly written into. {@link #userBuf} provides the buffer, and
     * renders this field meaningless if {@code null}. Inside the
     * {@link #read(byte[], int, int)} call, the reading thread should
     * set this field to its second argument, at the same time as it
     * sets {@link #userBuf}. It should not be modified as data is
     * directly written into {@link #userBuf}; {@link #suppliedToUser}
     * indicates that, so <code class=
     * "java">{@linkplain #userOff} + {@linkplain #suppliedToUser}</code>
     * gives the offset for writing the next byte.
     */
    private int userOff;

    /**
     * Specifies the maximum number of bytes available in the reader's
     * buffer. This should be set by {@link #read(byte[], int, int)} to
     * its third argument, when its first argument is stored in
     * {@link #userBuf}. This field has no meaning if {@link #userBuf}
     * is {@code null}, so it does not need to be reset afterwards. This
     * field should not be modified when data is actually written
     * directly into {@link #userBuf}; {@link #suppliedToUser} records
     * that, so <code class=
     * "java">{@linkplain #userLen} - {@linkplain #suppliedToUser}</code>
     * actually gives the remaining space.
     */
    private int userLen;

    /**
     * Records the number of bytes placed directly into the reader's
     * buffer. The reader's buffer is a portion of {@link #userBuf} (if
     * not {@code null}), of {@link #userLen} bytes starting at offset
     * {@link #userOff}. This field indicates how many initial bytes of
     * this region have already been written to, and should be set to
     * zero when these other fields are set.
     */
    private int suppliedToUser;

    /**
     * Compute the available bytes.
     * 
     * @return the number of available bytes, i.e., the difference
     * between the read and write positions
     */
    int available() {
        try {
            lock.lock();
            return writePos - readPos;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Read a single byte. The thread's monitor is claimed until the
     * input stream has been closed, or the content has been marked as
     * complete, or a reason for abortion has been specified, or some
     * bytes are available. If interrupted while waiting for more bytes,
     * this method continues waiting for the terminating condition, but
     * will re-interrupt the thread as soon as it is met.
     * 
     * @return the byte read, as an unsigned value; or {@code -1} on
     * end-of-file
     * 
     * @throws StreamAbortedException if the stream has been aborted
     * with {@link #abort(Throwable)}
     * 
     * @throws IOException if the input stream has been closed
     */
    int read() throws IOException {
        try {
            lock.lock();
            check();
            claimAsReader();
            try {
                final int rem = awaitData();
                if (rem == 0) return -1;

                /* At least one byte is available, so provide it. */
                memoryUsage.addAndGet(-1);
                return array[readPos++] & 0xff;
            } finally {
                releaseAsReader();
                check();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wait until there is data to be read or some other
     * stream-terminating condition. The calling thread must hold
     * {@link #lock}. If an interruption occurs while waiting, the
     * interruption is recorded, and waiting continues. The interruption
     * is then re-issued before exiting the method.
     * 
     * @return the number of bytes available
     * 
     * @throws StreamAbortedException if the stream has been aborted
     * with {@link #abort(Throwable)}
     * 
     * @throws IOException if the input stream has been closed
     */
    private int awaitData() throws IOException {
        assert lock.isHeldByCurrentThread();

        /* Wait until there's no reason to block. */
        boolean interrupted = false;
        int rem = -1;
        while (array != null && (rem = writePos - readPos) == 0 && !complete &&
            (userBuf == null || suppliedToUser == 0)) {
            try {
                ready.await();
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }

        /* Re-transmit the interruption. */
        if (interrupted) Thread.currentThread().interrupt();

        /* Detect errors and end-of-file. */
        if (array == null) throw new IOException("closed");
        assert rem >= 0;
        return rem;
    }

    /**
     * Read several bytes into an array. The thread's monitor is claimed
     * until the input stream has been closed, or the content has been
     * marked as complete, or a reason for abortion has been specified,
     * or some bytes are available. If interrupted while waiting for
     * more bytes, this method continues waiting for the terminating
     * condition, but will re-interrupt the thread as soon as it is met.
     * 
     * @param b the array to store the bytes
     * 
     * @param off the index into the array of the first byte
     * 
     * @param len the maximum number of bytes to read
     * 
     * @return the number of bytes read; or {@code -1} on end-of-file
     * 
     * @throws StreamAbortedException if the stream has been aborted
     * with {@link #abort(Throwable)}
     * 
     * @throws IOException if the input stream has been closed
     */
    int read(byte[] b, int off, int len) throws IOException {
        /* Short-circuit the no-op. */
        if (len == 0) return 0;

        try {
            lock.lock();
            check();
            claimAsReader();
            try {
                /* Indicate that we can receive bytes directly into our
                 * own buffer (if circumstances permit). */
                userOff = off;
                userLen = len;
                suppliedToUser = 0;
                userBuf = b;

                try {
                    final int rem = awaitData();

                    /* Stop here if data was written directly into the
                     * reader's buffer. */
                    if (suppliedToUser > 0) return suppliedToUser;

                    /* If we've unblocked with no bytes available, this
                     * chunk has reached EOF. */
                    if (rem == 0) return -1;

                    /* At least one byte is available, so work out how
                     * much to provide, and transfer it. */
                    int amount = Integer.min(len, rem);
                    assert amount > 0;
                    System.arraycopy(array, readPos, b, off, amount);
                    readPos += amount;
                    memoryUsage.addAndGet(-amount);
                    return amount;
                } finally {
                    userBuf = null;
                }
            } finally {
                releaseAsReader();
                check();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public InputStream getStream() {
        return stream;
    }

    private final InputStream stream = new InputStream() {
        @Override
        public int read() throws IOException {
            return MemoryChunk.this.read();
        }

        @Override
        public void close() throws IOException {
            MemoryChunk.this.close();
        }

        @Override
        public int available() throws IOException {
            return MemoryChunk.this.available();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return MemoryChunk.this.read(b, off, len);
        }
    };
}
