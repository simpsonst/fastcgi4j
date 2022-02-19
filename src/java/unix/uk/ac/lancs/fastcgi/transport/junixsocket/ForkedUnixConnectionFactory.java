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

package uk.ac.lancs.fastcgi.transport.junixsocket;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Collection;
import org.newsclub.net.unix.AFUNIXServerSocket;
import uk.ac.lancs.fastcgi.proto.InvocationVariables;
import uk.ac.lancs.fastcgi.transport.ConnectionFactory;
import uk.ac.lancs.fastcgi.transport.ConnectionSupply;
import uk.ac.lancs.fastcgi.transport.TransportConfigurationException;
import uk.ac.lancs.scc.jardeps.Service;

/**
 * Creates a connection supply from a Unix-domain stream server socket
 * on file descriptor 0. This descriptor is available in Java as
 * {@link FileDescriptor#in}, and is defined within FastCGI as the
 * server socket which the application should accept new connections on.
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
     * {@link InvocationVariables#getAuthorizedInetPeers()} yields
     * non-{@code null}, descriptor 0 is probably an Internet-domain
     * socket.
     */
    @Override
    public ConnectionSupply getConnectionSupply() {
        try {
            /* See if we've been told who to allow connection from. This
             * only happens under FastCGI using an Internet-domain
             * socket, so it's not for us. */
            Collection<InetAddress> inetPeers =
                InvocationVariables.getAuthorizedInetPeers();
            if (inetPeers != null) return null;

            /* Check whether FD 0 has been provided. */
            if (!FileDescriptor.in.valid())
                throw new TransportConfigurationException("STDIN is invalid");

            /* Attempt to create a Unix-domain server socket from FD 0.
             * If it is not a bound socket, a SocketException will be
             * thrown and caught, and null is then returned, indicating
             * that this process hsa not been invoked according to
             * FastCGI with a Unix-domain socket. */
            AFUNIXServerSocket serverSocket =
                AFUNIXServerSocket.newInstance(FileDescriptor.in, 1000, 1001);
            return new ForkedUnixConnectionSupply(serverSocket);
        } catch (SocketException ex) {
            return null;
        } catch (IOException ex) {
            throw new TransportConfigurationException(ex);
        }
    }
}
