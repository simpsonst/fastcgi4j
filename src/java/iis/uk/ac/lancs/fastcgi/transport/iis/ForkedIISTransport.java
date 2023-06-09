/*
 * Copyright (c) 2022,2023, Lancaster University
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

package uk.ac.lancs.fastcgi.transport.iis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import uk.ac.lancs.fastcgi.transport.Connection;
import uk.ac.lancs.fastcgi.transport.Transport;

/**
 * Supplies a single connection over a named pipe. Attempts to obtain a
 * second connection will block indefinitely.
 * 
 * @author simpsons
 */
class ForkedIISTransport implements Transport {
    private static final String DESCR = "iis";

    /**
     * Create a transport over a named pipe. Only one connection will be
     * provided. Its description will be {@value #DESCR}.
     * 
     * @param file the named pipe
     * 
     * @param intDescr the sensitive description of the connection, such
     * as the name of the pipe
     */
    public ForkedIISTransport(RandomAccessFile file, String intDescr) {
        final InputStream in = new InputStream() {
            @Override
            public int read() throws IOException {
                return file.read();
            }

            @Override
            public long skip(long n) throws IOException {
                return file.skipBytes((int) Long.max(n, Integer.MAX_VALUE));
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return file.read(b, off, len);
            }
        };

        final OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                file.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                file.write(b, off, len);
            }
        };

        primary = new Connection() {
            @Override
            public InputStream input() throws IOException {
                return in;
            }

            @Override
            public OutputStream output() throws IOException {
                return out;
            }

            @Override
            public void close() throws IOException {
                file.close();
            }

            @Override
            public String description() {
                return DESCR;
            }

            @Override
            public String internalDescription() {
                return intDescr;
            }
        };
    }

    private Connection primary;

    private boolean more = true;

    private final Lock lock = new ReentrantLock();

    private final Condition ready = lock.newCondition();

    /**
     * Stop blocking on subsequent requests for the next connection.
     */
    public void terminate() {
        try {
            lock.lock();
            more = false;
            ready.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @default On the first call, the sole connection is returned. A
     * subsequent call will block until {@link #terminate() } is called.
     */
    @Override
    public Connection nextConnection() throws IOException {
        if (primary != null) {
            Connection result = primary;
            primary = null;
            return result;
        }

        try {
            lock.lock();
            while (more)
                ready.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
        return null;
    }
}
