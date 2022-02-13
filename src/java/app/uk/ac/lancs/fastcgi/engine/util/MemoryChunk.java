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
import java.io.InterruptedIOException;
import java.util.concurrent.atomic.AtomicLong;
import uk.ac.lancs.fastcgi.StreamAbortedException;

/**
 *
 * @author simpsons
 */
class MemoryChunk implements Chunk {
    private final AtomicLong memoryUsage;

    private byte[] array;

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
        assert readPos <= array.length;
        assert writePos >= 0;
        assert writePos <= array.length;
    }

    @Override
    public synchronized int write(byte[] buf, int off, int len)
        throws IOException {
        check();
        try {
            final int rem = array.length - writePos;
            final int amount;
            if (len > rem) {
                /* We don't have enough space in our buffer. Try
                 * shifting the current content to the start of the
                 * buffer. */
                System.arraycopy(array, readPos, array, 0, writePos - readPos);
                writePos -= readPos;
                readPos = 0;
                amount = Integer.min(len, array.length - writePos);
            } else {
                /* Just use what we have left. */
                amount = Integer.min(len, rem);
            }

            System.arraycopy(buf, off, array, writePos, amount);
            writePos += amount;
            memoryUsage.addAndGet(amount);
            notify();
            return amount;
        } finally {
            check();
        }
    }

    @Override
    public synchronized void complete() throws IOException {
        complete = true;
        // System.err.printf("completed %s%n", this);
        notify();
    }

    synchronized void close() throws IOException {
        array = null;
    }

    synchronized int available() throws IOException {
        return writePos - readPos;
    }

    synchronized int read() throws IOException {
        check();
        try {
            while (!complete && reason == null && readPos == writePos) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    throw new InterruptedIOException();
                }
            }
            if (reason != null) throw new StreamAbortedException(reason);
            if (readPos == writePos) return -1;
            return array[readPos++] & 0xff;
        } finally {
            check();
        }
    }

    synchronized int read(byte[] b, int off, int len) throws IOException {
        check();
        // System.err.printf("reading from %s%n", this);
        try {
            while (!complete && reason == null && readPos == writePos) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    throw new InterruptedIOException();
                }
            }
            if (reason != null) throw new StreamAbortedException(reason);
            if (readPos == writePos) return -1;
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
    public synchronized void abort(Throwable reason) {
        if (this.reason != null) return;
        this.reason = reason;
        notify();
    }
}
