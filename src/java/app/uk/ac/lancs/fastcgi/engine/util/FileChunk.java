/*
 * Copyright (c) 2022, Regents of the University of Lancaster
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
import java.io.RandomAccessFile;
import uk.ac.lancs.fastcgi.StreamAbortedException;

/**
 * Stores content in a file.
 *
 * @author simpsons
 */
final class FileChunk implements Chunk {
    private final long maxFileSize;

    private RandomAccessFile file;

    private long readPos = 0;

    private long writePos = 0;

    private boolean complete = false;

    private Throwable reason = null;

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
        this.maxFileSize = maxFileSize;
    }

    /**
     * {@inheritDoc}
     * 
     * This method claims the monitor, seeks to the end of the file, and
     * writes up to the requested amount, ensuring that the configured
     * maximum size is not exceeded.
     * 
     * @throws IllegalStateException if the chunk has been completed
     * using {@link #complete()}
     */
    @Override
    public synchronized int write(byte[] buf, int off, int len)
        throws IOException {
        if (complete) throw new IllegalStateException("complete");
        if (file == null) return len;
        long remaining = maxFileSize - writePos;
        if (remaining == 0) return 0;
        int amount = (int) Long.min(remaining, len);
        file.seek(writePos);
        file.write(buf, off, amount);
        long exp = writePos + amount;
        writePos = file.getFilePointer();
        assert writePos == exp;
        notify();
        return amount;
    }

    /**
     * {@inheritDoc}
     * 
     * The content is marked as complete.
     */
    @Override
    public synchronized void complete() {
        complete = true;
        notify();
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
    synchronized int read() throws IOException {
        boolean interrupted = false;
        while (file != null && !complete && reason == null &&
            readPos == writePos) {
            try {
                wait();
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }

        /* Re-transmit the interruption. */
        if (interrupted) Thread.currentThread().interrupt();

        if (file == null) throw new IOException("closed");
        if (reason != null) throw new StreamAbortedException(reason);
        if (readPos == writePos) return -1;
        file.seek(readPos);
        int r = file.read();
        readPos++;
        return r;
    }

    /**
     * Close the input stream. The underlying stream is closed, and its
     * handle is discarded to mark this.
     * 
     * @throws IOException if closing the underlying file fails
     */
    synchronized void close() throws IOException {
        try {
            file.close();
        } finally {
            file = null;
        }
    }

    synchronized int available() {
        if (file == null) return 0;
        return (int) Long.max(writePos - readPos, Integer.MAX_VALUE);
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
    synchronized int read(byte[] b, int off, int len) throws IOException {
        boolean interrupted = false;
        while (file != null && !complete && reason == null &&
            readPos == writePos) {
            try {
                wait();
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }

        /* Re-transmit the interruption. */
        if (interrupted) Thread.currentThread().interrupt();

        if (file == null) throw new IOException("closed");
        if (reason != null) throw new StreamAbortedException(reason);
        if (readPos == writePos) return -1;
        int amount = (int) Long.min(len, writePos - readPos);
        file.seek(readPos);
        file.readFully(b, off, amount);
        readPos += amount;
        return amount;
    }

    @Override
    public InputStream getStream() {
        return stream;
    }

    private final InputStream stream = new InputStream() {
        @Override
        public int read() throws IOException {
            return FileChunk.this.read();
        }

        @Override
        public void close() throws IOException {
            FileChunk.this.close();
        }

        @Override
        public int available() throws IOException {
            return FileChunk.this.available();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return FileChunk.this.read(b, off, len);
        }
    };

    @Override
    public synchronized void abort(Throwable reason) {
        if (this.reason != null) return;
        this.reason = reason;
        notify();
    }
}
