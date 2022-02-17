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

package uk.ac.lancs.fastcgi.transport.inet;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;

/**
 *
 * @author simpsons
 */
class FDServerSocketImpl extends SocketImpl {
    public FDServerSocketImpl(FileDescriptor fd) throws IOException {
        if (!fd.valid()) throw new IllegalArgumentException("invalid FD");
        this.fd = fd;
        this.localport = -1;
        this.port = -1;
        this.address = null;
    }

    private static native InetSocketAddress getOwnAddr(FileDescriptor fd);

    InetSocketAddress getLocalSocketAddress() {
        return getOwnAddr(fd);
    }

    @Override
    protected void accept(SocketImpl impl) throws IOException {
        accept(fd, impl);
    }

    private static native void accept(FileDescriptor fd, SocketImpl impl)
        throws IOException;

    @Override
    protected void close() throws IOException {
        try {
            close(fd);
        } finally {
            fd = null;
            localport = -1;
        }
    }

    private static native void close(FileDescriptor fd) throws IOException;

    @Override
    protected void create(boolean arg0) throws IOException {
        throw new IOException("server socket");
    }

    @Override
    protected void connect(String arg0, int arg1) throws IOException {
        throw new UnsupportedOperationException("unsupported");
    }

    @Override
    protected void connect(InetAddress arg0, int arg1) throws IOException {
        throw new UnsupportedOperationException("unsupported");
    }

    @Override
    protected void connect(SocketAddress arg0, int arg1) throws IOException {
        throw new UnsupportedOperationException("unsupported");
    }

    @Override
    protected void bind(InetAddress arg0, int arg1) throws IOException {
        throw new IOException("already bound");
    }

    @Override
    protected void listen(int arg0) throws IOException {
        throw new IOException("already listening");
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        throw new IOException("server socket");
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        throw new IOException("server socket");
    }

    @Override
    protected int available() throws IOException {
        return -1;
    }

    @Override
    protected void sendUrgentData(int arg0) throws IOException {
        throw new IOException("server socket");
    }

    @Override
    public void setOption(int arg0, Object arg1) throws SocketException {
        throw new SocketException("server socket");
    }

    @Override
    public Object getOption(int arg0) throws SocketException {
        throw new SocketException("server socket");
    }
}
