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

package uk.ac.lancs.fastcgi.conn.unix;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import org.newsclub.net.unix.AFUNIXServerSocket;
import uk.ac.lancs.fastcgi.engine.ConnectionFactory;
import uk.ac.lancs.fastcgi.engine.ConnectionSupply;
import uk.ac.lancs.fastcgi.proto.ConnectionVariables;
import uk.ac.lancs.scc.jardeps.Service;

/**
 * Creates a connection supply from a Unix-domain stream server socket
 * on file descriptor 0.
 *
 * @author simpsons
 */
@Service(ConnectionFactory.class)
public class ForkedUnixConnectionFactory implements ConnectionFactory {
    /**
     * {@inheritDoc}
     * 
     * This implementation returns {@code null} if there is any
     * indication that the process has not been invoked with a
     * Unix-domain socket on file descriptor 0. For example, if
     * {@link ConnectionVariables#getAuthorizedInetPeers()} yields
     * non-{@code null}, descriptor 0 is probably an Internet-domain
     * socket.
     */
    @Override
    public ConnectionSupply getConnectionSupply() {
        try {
            Collection<InetAddress> inetPeers =
                ConnectionVariables.getAuthorizedInetPeers();
            if (inetPeers != null) return null;

            AFUNIXServerSocket serverSocket =
                AFUNIXServerSocket.newInstance(FileDescriptor.in, 1000, 1001);
            return new ForkedUnixConnectionSupply(serverSocket);
        } catch (UnknownHostException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        }
    }
}
