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

package uk.ac.lancs.fastcgi.transport.inet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Collection;
import uk.ac.lancs.fastcgi.proto.InvocationVariables;
import uk.ac.lancs.fastcgi.transport.TransportConfigurationException;
import uk.ac.lancs.scc.jardeps.Service;
import uk.ac.lancs.fastcgi.transport.Transport;
import uk.ac.lancs.fastcgi.transport.TransportFactory;

/**
 * Recognizes stand-alone Internet-domain transports. The environment
 * variable {@value InvocationVariables#INET_BIND_ADDR} must be set,
 * specifying the address to bind to. The environment variable
 * {@value InvocationVariables#WEB_SERVER_ADDRS} also must be set,
 * listing valid peer addresses.
 * 
 * <p>
 * Each connection's description begins
 * {@value #STANDALONE_DESCRIPTION}. This prefix is combined with the
 * peer address in the form
 * <samp><var>prefix</var>#<var>address</var></samp> to complete the
 * connection description.
 * 
 * @author simpsons
 */
@Service(TransportFactory.class)
public class InetTransportFactory implements TransportFactory {
    @Override
    public Transport getTransport() {
        try {
            /* What do we bind to? If not set, it's not our
             * transport. */
            InetSocketAddress bindAddress =
                InvocationVariables.getInetBindAddress();
            if (bindAddress == null) return null;

            /* We must know what peers are permitted. */
            Collection<InetAddress> allowedPeers =
                InvocationVariables.getAuthorizedStandaloneInetPeers();
            if (allowedPeers == null) return null;

            final ServerSocket ss;
            final String descr;
            ss = new ServerSocket(bindAddress.getPort(), 5,
                                  bindAddress.getAddress());
            descr = STANDALONE_DESCRIPTION;
            return new InetTransport(descr, ss, allowedPeers);
        } catch (IOException ex) {
            throw new TransportConfigurationException(ex);
        }
    }

    private static final String STANDALONE_DESCRIPTION = "inet-standalone";
}
