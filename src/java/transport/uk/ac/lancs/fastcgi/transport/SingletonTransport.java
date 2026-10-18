// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2026, Lancaster University
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
 *  Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.fastcgi.transport;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Supplies a single connection. Attempts to obtain a second connection
 * will return {@code null}.
 * 
 * @author simpsons
 */
public class SingletonTransport implements Transport {
    /**
     * Create a transport serving a single connection.
     * 
     * @param primary the sole connection
     */
    protected SingletonTransport(Connection primary) {
        this.primary = primary;
    }

    private Connection primary;

    /**
     * {@inheritDoc}
     * 
     * @return the connection supplied to the constructor on the first
     * call; {@code null} otherwise
     */
    @Override
    public Connection nextConnection() throws IOException {
        if (primary != null) {
            Connection result = primary;
            primary = null;
            logger
                .info(() -> String.format("accepted sole connection as %s (%s)",
                                          result.description(),
                                          result.internalDescription()));
            return result;
        }

        return null;
    }

    private static final Logger logger =
        Logger.getLogger(SingletonTransport.class.getPackageName());
}
