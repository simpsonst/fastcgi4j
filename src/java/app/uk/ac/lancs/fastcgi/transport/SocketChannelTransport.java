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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Creates connections by accepting socket channels from a server socket
 * channel.
 *
 * @author simpsons
 */
public abstract class SocketChannelTransport implements Transport {
    /**
     * Holds the channel from which connections are accepted.
     */
    protected final ServerSocketChannel channel;

    private final String intDescr;

    /**
     * Create a transport based on a server socket channel.
     * 
     * @param channel the channel from which connections will be
     * accepted
     * 
     * @throws IOException if an error occurs in obtaining the local
     * address of the channel
     */
    public SocketChannelTransport(ServerSocketChannel channel)
        throws IOException {
        this.channel = channel;
        this.intDescr = channel.getLocalAddress().toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @default Repeatedly, {@link ServerSocketChannel#accept()} is
     * invoked on the configured server socket channel. The new channel
     * is submitted to {@link #describe(SocketChannel)}. If this returns
     * {@code null}, the new channel is closed. Otherwise, the result is
     * used as the public diagnostic description of a new
     * {@link SocketChannelConnection} build from the new channel. Its
     * internal description is the server socket channel's local
     * address.
     */
    @Override
    public Connection nextConnection() throws IOException {
        do {
            SocketChannel chan = channel.accept();
            String descr = describe(chan);
            if (descr == null) {
                chan.close();
                continue;
            }
            return new SocketChannelConnection(chan, descr, intDescr);
        } while (true);
    }

    /**
     * Determine whether to build a connection from a socket channel,
     * and how to describe it.
     * 
     * @param channel the channel to be tested
     * 
     * @return a public description of the channel; or {@code null} if
     * the connection is to be rejected
     * 
     * @throws IOException if an I/O error occurs, usually in
     * determining whether to build a connection
     */
    protected abstract String describe(SocketChannel channel)
        throws IOException;
}
