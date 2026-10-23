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
 *  Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.fastcgi.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * Supplies a single connection over a named pipe. The pipe is accessed
 * using {@link RandomAccessFile}. To form the connection, an input
 * stream is wrapped around its methods {@link RandomAccessFile#read()},
 * {@link RandomAccessFile#read(byte[], int, int)} and
 * {@link RandomAccessFile#skipBytes(int)}, and an output stream is
 * wrapped around {@link RandomAccessFile#write(int)} and
 * {@link RandomAccessFile#write(byte[], int, int)}.
 *
 * @author simpsons
 */
public class PipeTransport extends SingletonTransport {
    private static Connection makeConnection(RandomAccessFile file,
                                             String descr, String intDescr) {
        final InputStream in = new InputStream() {
            @Override
            public int read() throws IOException {
                return file.read();
            }

            @Override
            public long skip(long n) throws IOException {
                return file.skipBytes((int) Long.min(n, Integer.MAX_VALUE));
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

        return new Connection() {
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
                return descr;
            }

            @Override
            public String internalDescription() {
                return intDescr;
            }
        };
    }

    /**
     * Create a transport over a named pipe. Only one connection will be
     * provided.
     * 
     * @param file the named pipe
     * 
     * @param descr the connection description
     * 
     * @param intDescr the sensitive description of the connection, such
     * as the name of the pipe
     */
    public PipeTransport(RandomAccessFile file, String descr, String intDescr) {
        super(makeConnection(file, descr, intDescr));
    }
}
