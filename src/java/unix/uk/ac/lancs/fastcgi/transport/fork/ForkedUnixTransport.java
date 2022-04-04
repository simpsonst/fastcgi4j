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

package uk.ac.lancs.fastcgi.transport.fork;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import uk.ac.lancs.fastcgi.proto.InvocationVariables;
import uk.ac.lancs.fastcgi.transport.Connection;
import uk.ac.lancs.fastcgi.transport.Transport;

/**
 * Supplies connections based on a FastCGI transport provided by
 * executing the process with file descriptor 0 as a server socket.
 * 
 * @author simpsons
 */
class ForkedUnixTransport implements Transport {
    private final Descriptor fd;

    private interface PeerValidator {
        /**
         * Determine whether the connection from the peer should be
         * accepted.
         * 
         * @param addrLen the number of bytes in the peer's address
         * 
         * @param addr the bytes of the peer's address
         * 
         * @return {@code null} if the connection should be rejected; or
         * a suffix to be used in the connection's description (as
         * defined by {@link Diagnostics#connectionDescription)
         */
        String permit(int addrLen, byte[] addr);
    }

    private final PeerValidator validator;

    private final String descr;

    private final String intDescr;

    private ForkedUnixTransport(int descriptor, String descr, String intDescr,
                                PeerValidator validator) {
        this.fd = new Descriptor(descriptor);
        this.validator = validator;
        this.descr = descr;
        this.intDescr = intDescr;
    }

    /**
     * Detect a forked FastCGI transport on file descriptor 0. The
     * transport is returned if detected. If {@code null} is returned,
     * no such transport is present, and another kind should be sought.
     * 
     * @return a transport based on file descriptor 0; or {@code null}
     * if no such transport is detected
     * 
     * @throws UnknownHostException if a forked Internet-domain
     * transport is detected, but the environment variable
     * {@value InvocationVariables#WEB_SERVER_ADDRS} contains an
     * unresolved hostname or unparsed IP address
     * 
     * @constructor
     */
    static ForkedUnixTransport create() throws UnknownHostException {
        byte[] addr = new byte[Descriptor.getAddressSize()];
        int[] addrLen = new int[1];
        int descriptor = Descriptor.checkDescriptor(addrLen, addr);
        if (descriptor < 0) return null;
        SocketAddress saddr = Descriptor.getSocketAddress(addrLen[0], addr);
        final PeerValidator validator;
        final String intDescr;
        if (saddr instanceof InetSocketAddress) {
            final Collection<InetAddress> permittedCallers =
                InvocationVariables.getAuthorizedInetPeers();
            validator = (addrLen1, addr1) -> {
                InetSocketAddress peerAddr = (InetSocketAddress) Descriptor
                    .getSocketAddress(addrLen1, addr1);
                if (!permittedCallers.contains(peerAddr.getAddress()))
                    return null;
                return "-inet-" + peerAddr;
            };
            intDescr = saddr.toString();
        } else if (saddr instanceof UnixDomainSocketAddress udsa) {
            validator = (addrLen1, addr1) -> "-unix";
            intDescr = udsa.getPath().toString();
        } else {
            return null;
        }
        return new ForkedUnixTransport(descriptor, "forked", intDescr,
                                       validator);
    }

    @Override
    public Connection nextConnection() throws IOException {
        try {
            while (fd.isValid()) {
                byte[] addr = new byte[Descriptor.getAddressSize()];
                int[] addrLen = new int[1];
                int socket =
                    Descriptor.acceptConnection(fd.fd(), addrLen, addr);
                String suffix = validator.permit(addrLen[0], addr);
                if (suffix == null) {
                    Descriptor.closeSocket(socket);
                    continue;
                }
                return new ForkedUnixConnection(descr + suffix, intDescr,
                                                socket);
            }
            return null;
        } catch (IOException ex) {
            try {
                fd.close();
            } catch (IOException sup) {
                ex.addSuppressed(ex);
            }
            throw ex;
        }
    }
}
