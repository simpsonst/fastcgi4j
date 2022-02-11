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

package uk.ac.lancs.fastcgi.transport;

import java.io.IOException;
import java.util.ServiceLoader;

/**
 * Provides connections from the server as they become available.
 *
 * @author simpsons
 */
public interface ConnectionSupply {
    /**
     * Get the next connection. This call blocks until a connection is
     * available, or there are no more connections.
     * 
     * @return the next connection; or {@code null} if there are no more
     * connections
     * 
     * @throws IOException if an I/O error occurs
     */
    Connection nextConnection() throws IOException;

    /**
     * Get the connection supply using a given class loader. This method
     * will yield the same result for the same argument.
     * 
     * @param loader the class loader to be used to find services
     * implementing {@link ConnectionFactory}; or {@code null} to use
     * the context class loader of the calling thread
     * 
     * @return the connection supply
     * 
     * @throws UnsupportedOperationException if no suitable service
     * exists
     */
    static ConnectionSupply get(ClassLoader loader) {
        return ConnectionSupplies.supplies.computeIfAbsent(loader, k -> {
            for (ConnectionFactory cfact : ServiceLoader
                .load(ConnectionFactory.class, k)) {
                var supply = cfact.getConnectionSupply();
                if (supply != null) return supply;
            }

            throw new UnsupportedOperationException("no service "
                + "for connections");
        });
    }

    /**
     * Get the connection supply using the caller's context class
     * loader. This method calls {@link #get(ClassLoader)}, passing the
     * result of {@link Thread#getContextClassLoader()} applied to the
     * calling thread.
     * 
     * @param loader the class loader to be used to find services
     * implementing {@link ConnectionFactory}; or {@code null} to use
     * the context class loader of the calling thread
     * 
     * @return the connection supply
     * 
     * @throws UnsupportedOperationException if no suitable service
     * exists
     */
    static ConnectionSupply get() {
        return get(Thread.currentThread().getContextClassLoader());
    }
}
