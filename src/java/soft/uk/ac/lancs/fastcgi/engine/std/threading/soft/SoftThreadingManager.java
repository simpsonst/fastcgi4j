// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

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
 *  Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.fastcgi.engine.std.threading.soft;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import uk.ac.lancs.fastcgi.engine.std.threading.ThreadingManager;

/**
 * Yields executors that generate soft threads to handle connections and
 * sessions.
 * 
 * @author simpsons
 */
final class SoftThreadingManager implements ThreadingManager {
    private final int maxReqsPerConn;

    private final int maxConns;

    private final boolean allConnections;

    private final boolean allSessions;

    SoftThreadingManager(int maxConns, int maxReqsPerConn,
                         boolean allConnections, boolean allSessions) {
        this.maxReqsPerConn = maxReqsPerConn;
        this.maxConns = maxConns;
        this.allConnections = allConnections;
        this.allSessions = allSessions;
    }

    /**
     * Create a threading manager that uses unlimited soft threads for
     * all connections and sessions.
     * 
     * @return the requested manager
     */
    public static SoftThreadingManager soft() {
        return new SoftThreadingManager(0, 0, true, true);
    }

    /**
     * Create a threading manager that uses unlimited soft threads got
     * all connections, but a fixed number of hard threads for sessions.
     * 
     * @param maxReqsPerConn the number of hard threads for sessions
     * 
     * @return the requested manager
     */
    public static SoftThreadingManager softConnections(int maxReqsPerConn) {
        return new SoftThreadingManager(0, maxReqsPerConn, true, false);
    }

    /**
     * Create a threading manager that uses a fixed number of hard
     * threads for connections, but unlimited soft threads for sessions.
     * 
     * @param maxConns the number of hard threads for sessions
     * 
     * @return the requested manager
     */
    public static SoftThreadingManager softSessions(int maxConns) {
        return new SoftThreadingManager(maxConns, 0, false, true);
    }

    @Override
    public ExecutorService newConnectionExecutor(ThreadGroup group) {
        if (allConnections || maxConns == 0) {
            /* With virtual threads, we cannot use the thread group. */
            ThreadFactory conntf = (r) -> Thread.ofVirtual().unstarted(r);
            return Executors.newThreadPerTaskExecutor(conntf);
        } else {
            ThreadFactory conntf = (r) -> new Thread(group, r);
            return Executors.newFixedThreadPool(maxConns, conntf);
        }
    }

    @Override
    public ExecutorService newSessionExceutor(ThreadGroup group) {
        if (allSessions || maxReqsPerConn == 0) {
            ThreadFactory tf = r -> Thread.ofVirtual().unstarted(r);
            return Executors.newThreadPerTaskExecutor(tf);
        } else {
            ThreadFactory tf = r -> new Thread(group, r);
            return Executors.newFixedThreadPool(maxReqsPerConn, tf);
        }
    }
}
