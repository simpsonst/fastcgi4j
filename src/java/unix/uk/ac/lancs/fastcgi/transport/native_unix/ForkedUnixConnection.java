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

package uk.ac.lancs.fastcgi.transport.native_unix;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import uk.ac.lancs.fastcgi.transport.Connection;

/**
 *
 * @author simpsons
 */
class ForkedUnixConnection implements Connection {
    private final String descr;

    private final String intDescr;

    private final Descriptor fd;

    ForkedUnixConnection(String descr, String intDescr, int fd) {
        this.descr = descr;
        this.intDescr = intDescr;
        this.fd = new Descriptor(fd);
    }

    private final InputStream input = new InputStream() {
        @Override
        public int read() throws IOException {
            return fd.read();
        }

        @Override
        public void close() throws IOException {
            fd.close();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return fd.read(b, off, len);
        }
    };

    private final OutputStream output = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
            fd.write(b);
        }

        @Override
        public void close() throws IOException {
            fd.close();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            fd.write(b, off, len);
        }
    };

    @Override
    public InputStream getInput() throws IOException {
        return input;
    }

    @Override
    public OutputStream getOutput() throws IOException {
        return output;
    }

    @Override
    public void close() throws IOException {
        fd.close();
    }

    @Override
    public String description() {
        return descr;
    }

    @Override
    public String internalDescription() {
        return intDescr;
    }
}
