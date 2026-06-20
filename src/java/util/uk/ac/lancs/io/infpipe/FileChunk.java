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

package uk.ac.lancs.io.infpipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Stores content in a file. The chunk is initially in writing mode, and
 * calls to {@link #write(byte[], int, int)} will continue to work up to
 * the configured maximum file size. At that point, or when the input
 * stream is accessed, the chunk goes into reading mode, and further
 * writes signal that the chunk is full.
 *
 * @author simpsons
 */
public class FileChunk implements Chunk {
    /**
     * Holds the handle for accessing the file. When the input stream is
     * closed, this is set to {@code null}, causing further accesses to
     * throw an exception.
     */
    private RandomAccessFile file;

    /**
     * The maximum number of bytes stored in the file
     */
    private final long maxFileSize;

    /**
     * Create a chunk stored in a file.
     * 
     * @param file the handle for reading and writing to and from the
     * stream
     * 
     * @param maxFileSize the maximum number of bytes to allow to be
     * written
     */
    public FileChunk(RandomAccessFile file, long maxFileSize) {
        this.file = file;
        this.remaining = this.maxFileSize = maxFileSize;
    }

    private final Lock lock = new ReentrantLock();

    private final Condition ready = lock.newCondition();

    /**
     * Indicates that the chunk is in reading mode.
     */
    private volatile boolean inReadingMode = false;

    /**
     * Indicates that the reader now wants to access the data. Setting
     * this is a request to put the chunk into reading mode.
     */
    private volatile boolean wanted = false;

    /**
     * Holds the remaining capacity in writing mode, or the remaining
     * bytes in reading mode. A call to {@link #complete()} switches
     * into reading mode. If a call to {@link #write(byte[], int, int)}
     * returns zero, that also puts the chunk into reading mode.
     */
    private long remaining;

    @Override
    public int write(byte[] buf, int off, int len) throws IOException {
        if (len == 0)
            throw new IllegalArgumentException("non-positive length: " + len);
        /* If we have put ourselves into reading mode, the writer might
         * attempt one more write, so we tell them we're full. */
        if (inReadingMode) return 0;

        /* If the reader has become active, we'll mark ourselves as in
         * reading mode. */
        if (wanted) {
            complete();
            return 0;
        }

        /* See how much more data we can take, compared to what's on
         * offer. */
        int amount = (int) Long.min(remaining, len);

        /* If we're full, switch to reading mode. */
        if (amount == 0) {
            complete();
            return 0;
        }

        /* Write the computed amount to the file, and account for it. */
        file.write(buf, off, amount);
        remaining -= amount;
        return amount;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Calling this method puts the chunk into reading mode. It is also
     * called internally by {@link #write(byte[], int, int)} when the
     * chunk is full, or the request to switch to reading mode has been
     * detected.
     */
    @Override
    public void complete() throws IOException {
        /* Make this call idempotent for the writer. */
        if (inReadingMode) return;

        /* Reset the stream back to the start, ready for reading. */
        file.seek(0);

        /* Redefine the count of remaining bytes to be of those now in
         * the file. */
        remaining = maxFileSize - remaining;

        /* Notify the reader that the chunk is in reading mode. */
        try {
            lock.lock();
            inReadingMode = true;
            ready.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Request that the chunk be put in reading mode, and wait until it
     * is.
     * 
     * @throws IOException if the stream has been closed
     */
    private void claim() throws IOException {
        /* We clear the reference to the underlying stream when closed,
         * so detect that. */
        if (file == null) throw new IOException("closed");

        /* Tell the writer not to write any more. */
        wanted = true;

        /* If the writer has already completed, we're okay. */
        if (inReadingMode) return;

        /* Await readiness of the stream for reading. */
        boolean interrupted = false;
        try {
            lock.lock();
            while (!inReadingMode) {
                try {
                    ready.await();
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
        } finally {
            lock.unlock();
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    @Override
    public InputStream getStream() {
        return stream;
    }

    private final InputStream stream = new InputStream() {
        @Override
        public void close() throws IOException {
            claim();
            if (file == null) return;
            try {
                file.close();
            } finally {
                file = null;
            }
        }

        @Override
        public int available() throws IOException {
            claim();
            return (int) Long.min(Integer.MAX_VALUE, remaining);
        }

        @Override
        public long skip(long n) throws IOException {
            claim();
            long amount = Long.min(remaining, n);
            file.seek(file.getFilePointer() + amount);
            remaining -= amount;
            return amount;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            claim();
            int rc = file.read(b, off, len);
            if (rc >= 0) remaining -= rc;
            return rc;
        }

        @Override
        public int read() throws IOException {
            claim();
            int rc = file.read();
            if (rc >= 0) remaining--;
            return rc;
        }
    };
}
