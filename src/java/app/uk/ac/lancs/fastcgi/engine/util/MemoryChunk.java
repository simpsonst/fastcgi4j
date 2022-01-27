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

    @Override
    public int write(byte[] buf, int off, int len) throws IOException {
        synchronized (this) {
            if (len < array.length - writePos) {
                System.arraycopy(buf, off, array, writePos, len);
                writePos += len;
                memoryUsage.addAndGet(len);
                notify();
                return len;
            }

            System.arraycopy(array, readPos, array, 0, writePos - readPos);
            writePos -= readPos;
            readPos = 0;

            int amount = Integer.min(len, array.length - writePos);
            System.arraycopy(buf, off, array, writePos, amount);
            writePos += amount;
            memoryUsage.addAndGet(amount);
            notify();
            return amount;
        }
    }

    @Override
    public synchronized void complete() throws IOException {
        complete = true;
        notify();
    }

    public synchronized void close() throws IOException {
        array = null;
    }

    public synchronized int available() throws IOException {
        return writePos - readPos;
    }

    public synchronized int read() throws IOException {
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
    }

    public synchronized int read(byte[] b, int off, int len)
        throws IOException {
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
        System.arraycopy(array, readPos, b, off, amount);
        readPos += amount;
        memoryUsage.addAndGet(-amount);
        return amount;
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
