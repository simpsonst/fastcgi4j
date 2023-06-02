/*
 * Copyright (c) 2022, Lancaster University
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

package uk.ac.lancs.fastcgi.engine.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import uk.ac.lancs.fastcgi.context.StreamAbortedException;

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

    private final Lock lock = new ReentrantLock();

    private final Condition ready = lock.newCondition();

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

    private int readPos = 0;

    private int writePos = 0;

    private boolean complete = false;

    private Throwable reason = null;

    private void check() {
        assert readPos <= writePos;
        assert readPos >= 0;
        assert writePos <= array.length;
    }

    /**
     * {@inheritDoc}
     * 
     * If there is space at the start of the buffer because bytes there
     * have already been read, the array contents may be shifted to the
     * start, to make more space at the end. As a result, a sufficiently
     * fast reader might prevent the array from filling up, and so
     * obviate the creation of a new chunk. Therefore, the total number
     * of bytes handled by the chunk may be more than the configured
     * limit.
     */
    @Override
    public int write(byte[] buf, int off, int len) {
        try {
            lock.lock();
            /* The content provider should not be supplying more content
             * when they've already said there's no more. */
            if (complete) throw new IllegalStateException("complete");

            /* Short-circuit the no-op. */
            if (len == 0) return 0;

            /* If the consumer has already indicated they're not longer
             * interested in the content, we can absorb it. */
            if (array == null) return len;

            check();
            try {
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
            try {
                /* Wait until there's no reason to block. */
                boolean interrupted = false;
                while (array != null && !complete && reason == null &&
                    readPos == writePos) {
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
                if (reason != null) throw new StreamAbortedException(reason);
                if (readPos == writePos) return -1;

                /* At least one byte is available, so provide it. */
                memoryUsage.addAndGet(-1);
                return array[readPos++] & 0xff;
            } finally {
                check();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Read several bytes into an array. The thread's monitor is claimed
     * until the input stream has been closed, or the content has been
     * marked as complete, or a reason for abortion has been specified,
     * or some bytes are available. If interrupted while waiting for
     * more bytes, this method continues waiting for the terminating
     * condition, but will re-interrupt the thread as soon as it is met.
     *
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
            try {
                /* Wait until there's no reason to block. */
                boolean interrupted = false;
                while (array != null && !complete && reason == null &&
                    readPos == writePos) {
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
                if (reason != null) throw new StreamAbortedException(reason);
                if (readPos == writePos) return -1;

                /* At least one byte is available, so work out how much
                 * to provide, and transfer it. */
                int amount = Integer.min(len, writePos - readPos);
                if (amount == 0) return 0;
                assert amount > 0;
                System.arraycopy(array, readPos, b, off, amount);
                readPos += amount;
                memoryUsage.addAndGet(-amount);
                return amount;
            } finally {
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

    @Override
    public void abort(Throwable reason) {
        try {
            lock.lock();
            if (this.reason != null) return;
            this.reason = reason;
            ready.signal();
        } finally {
            lock.unlock();
        }
    }
}
