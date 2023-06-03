// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2023, Lancaster University
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

package uk.ac.lancs.fastcgi.engine.std;

import uk.ac.lancs.fastcgi.engine.std.threading.ThreadingManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Yields executors that use exclusively hard threads.
 * 
 * @author simpsons
 */
final class DefaultThreadingManager implements ThreadingManager {
    private final int maxReqsPerConn;

    private final int maxConns;

    /**
     * Create a threading manager based on the configured number of
     * connections and sessions per connection.
     * 
     * @param maxConns the configured number of connections, or 0 if
     * unlimited
     * 
     * @param maxReqsPerConn the configured number of sessions per
     * connection, or 0 if unlimited
     */
    public DefaultThreadingManager(int maxConns, int maxReqsPerConn) {
        this.maxReqsPerConn = maxReqsPerConn;
        this.maxConns = maxConns;
    }

    @Override
    public ExecutorService newConnectionExecutor(ThreadGroup group) {
        if (maxConns == 0) {
            return Executors.newCachedThreadPool();
        } else {
            ThreadFactory conntf = (r) -> new Thread(group, r);
            return Executors.newFixedThreadPool(maxConns, conntf);
        }
    }

    @Override
    public ExecutorService newSessionExceutor(ThreadGroup group) {
        if (maxReqsPerConn == 0) {
            return Executors.newCachedThreadPool();
        } else {
            ThreadFactory tf = r -> new Thread(group, r);
            return Executors.newFixedThreadPool(maxReqsPerConn, tf);
        }
    }
}
