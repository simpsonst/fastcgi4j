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

package uk.ac.lancs.fastcgi;

/**
 * Provides diagnostic information for a FastCGI session.
 * 
 * @author simpsons
 */
public final class Diagnostics {
    /**
     * Holds a string describing how the server and application
     * communicate.
     */
    public final String connectionDescription;

    /**
     * Holds an internal identifier for the transport connection.
     */
    public final String connectionId;

    /**
     * Holds the FastCGI request identifier.
     */
    public final int requestId;

    /**
     * Identifies the implementation. An overarching package should be
     * specified.
     */
    public final Package implementation;

    /**
     * Get a string representation of this object. This takes the form
     * <samp><var >{@linkplain #connectionDescription}</var>&#64;<var
     * >{@linkplain #connectionId}</var>.<var
     * >{@linkplain #requestId}</var></samp>.
     * 
     * @return the requested string representation
     */
    @Override
    public String toString() {
        return connectionDescription + "@" + connectionId + "." + requestId;
    }

    /**
     * Create diagnostics.
     * 
     * @param implementation the implementation
     * 
     * @param connectionDescription the connection description
     * 
     * @param connectionId the internal connection identifier
     * 
     * @param requestId the FastCGI request identifier
     */
    public Diagnostics(Package implementation, String connectionDescription,
                       String connectionId, int requestId) {
        this.implementation = implementation;
        this.connectionDescription = connectionDescription;
        this.connectionId = connectionId;
        this.requestId = requestId;
    }
}
