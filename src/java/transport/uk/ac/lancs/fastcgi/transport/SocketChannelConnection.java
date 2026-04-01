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

package uk.ac.lancs.fastcgi.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

/**
 * Implements a FastCGI connection over a socket channel. A custom
 * {@link InputStream} wraps the channel's
 * {@link SocketChannel#read(ByteBuffer)} method, and similarly a custom
 * {@link OutputStream} wraps {@link SocketChannel#write(ByteBuffer)}.
 *
 * @author simpsons
 */
public class SocketChannelConnection implements Connection {
    /**
     * Holds the channel over which the FastCGI connection is
     * implemented.
     */
    protected final SocketChannel channel;

    /**
     * Presents an input-stream view of the socket's read operations. We
     * cannot use {@link Channels#newInputStream(ReadableByteChannel)},
     * as it locks on a monitor that
     * {@link Channels#newOutputStream(WritableByteChannel)} locks on.
     */
    private final InputStream in = new InputStream() {
        private boolean open = true;

        private final ByteBuffer oneByte = ByteBuffer.allocate(1);

        @Override
        public void close() throws IOException {
            if (!open) return;
            open = false;
        }

        private void checkFault() throws IOException {
            if (!open) throw new IOException("input closed");
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            checkFault();
            ByteBuffer buf = ByteBuffer.wrap(b, off, len);
            return channel.read(buf);
        }

        @Override
        public int read() throws IOException {
            checkFault();
            oneByte.clear();
            int got = channel.read(oneByte);
            if (got < 0) return -1;
            return oneByte.get(0) & 0xff;
        }
    };

    /**
     * Presents an output-stream view of the socket's write operations.
     * We cannot use
     * {@link Channels#newOutputStream(WritableByteChannel)}, as it
     * locks on a monitor that
     * {@link Channels#newInputStream(ReadableByteChannel)} locks on.
     */
    private final OutputStream out = new OutputStream() {
        private boolean open = true;

        private IOException fault;

        private final ByteBuffer oneByte = ByteBuffer.allocate(1);

        @Override
        public void close() throws IOException {
            if (!open) return;
            open = false;
        }

        private void writeFully(ByteBuffer buf) throws IOException {
            try {
                while (buf.remaining() > 0)
                    channel.write(buf);
            } catch (IOException ex) {
                fault = ex;
                throw fault;
            }
        }

        private void checkFault() throws IOException {
            if (!open) throw new IOException("output closed");
            if (fault != null) throw new IOException("earlier fault", fault);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            checkFault();
            ByteBuffer buf = ByteBuffer.wrap(b, off, len);
            writeFully(buf);
        }

        @Override
        public void write(int i) throws IOException {
            checkFault();
            oneByte.clear();
            oneByte.put(0, (byte) i);
            writeFully(oneByte);
        }
    };

    private final String descr;

    private final String intDescr;

    /**
     * Create a FastCGI connection from a socket channel.
     * 
     * @param channel the socket channel over which the connection is to
     * be implemented
     * 
     * @param descr a diagnostic description of this connection,
     * excluding sensitive information
     * 
     * @param intDescr sensitive information describing this connection
     */
    public SocketChannelConnection(SocketChannel channel, String descr,
                                   String intDescr) {
        this.channel = channel;
        this.descr = descr;
        this.intDescr = intDescr;
    }

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
        channel.close();
    }

    /**
     * {@inheritDoc}
     * 
     * @default The value returned is the second argument of the
     * constructor.
     */
    @Override
    public String description() {
        return descr;
    }

    /**
     * {@inheritDoc}
     * 
     * @default The value returned is the third argument of the
     * constructor.
     */
    @Override
    public String internalDescription() {
        return intDescr;
    }
}
