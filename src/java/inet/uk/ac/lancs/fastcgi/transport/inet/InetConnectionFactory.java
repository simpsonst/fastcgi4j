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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Collection;
import uk.ac.lancs.fastcgi.proto.InvocationVariables;
import uk.ac.lancs.fastcgi.transport.ConnectionFactory;
import uk.ac.lancs.fastcgi.transport.ConnectionSupply;
import uk.ac.lancs.fastcgi.transport.TransportConfigurationException;
import uk.ac.lancs.scc.jardeps.Service;

/**
 * Recognizes stand-alone and forked Internet-domain transports. If the
 * environment variable {@value InvocationVariables#WEB_SERVER_ADDRS} is
 * not set, the transport is not recognized. If
 * {@value InvocationVariables#INET_BIND_ADDR} is set, stand-alone mode
 * is assumed, and a socket is bound to that address. Otherwise,
 * {@link FileDescriptor#in} is assumed to be an Internet-domain socket,
 * and the process has been forked by the server.
 * 
 * <p>
 * Each connection's description begins with
 * {@value #FORKED_DESCRIPTION} if the process was invoked with the
 * server socket already created on descriptor 0, or
 * {@value #STANDALONE_DESCRIPTION} if the process has created the
 * server socket itself. This prefix is combined with the peer address
 * in the form <samp><var>prefix</var>#<var>address</var></samp> to
 * complete the connection description.
 * 
 * @author simpsons
 */
@Service(ConnectionFactory.class)
public class InetConnectionFactory implements ConnectionFactory {
    @Override
    public ConnectionSupply getConnectionSupply() {
        try {
            Collection<InetAddress> allowedPeers =
                InvocationVariables.getAuthorizedInetPeers();
            if (allowedPeers == null) return null;
            InetSocketAddress bindAddress =
                InvocationVariables.getInetBindAddress();
            final ServerSocket ss;
            final String descr;
            if (bindAddress == null) {
                ss = FDServerSocket.newInstance(FileDescriptor.in);
                descr = FORKED_DESCRIPTION;
            } else {
                ss = new ServerSocket(bindAddress.getPort(), 5,
                                      bindAddress.getAddress());
                descr = STANDALONE_DESCRIPTION;
            }
            return new InetConnectionSupply(descr, ss, allowedPeers);
        } catch (IOException ex) {
            throw new TransportConfigurationException(ex);
        }
    }

    private static final String FORKED_DESCRIPTION = "inet-forked";

    private static final String STANDALONE_DESCRIPTION = "inet-standalone";
}
