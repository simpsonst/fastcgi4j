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

package uk.ac.lancs.fastcgi.engine;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Combines multiple engines as elements of a completion service.
 * 
 * @author simpsons
 */
public class CompletionEngineCluster<K> implements EngineCluster<K> {
    private final ExecutorService executor;

    private final CompletionService<? extends EngineResult<K>> completion;

    /**
     * Combine the engines submitted through a completion service.
     * 
     * @param executor the executor to be shut down when the room is
     * closed
     * 
     * @param completion the completion service that will yield the
     * result of the first engine that terminates
     */
    public CompletionEngineCluster(ExecutorService executor,
                                   CompletionService<? extends EngineResult<K>> completion) {
        this.executor = executor;
        this.completion = completion;
    }

    @Override
    public EngineResult<K> await() throws InterruptedException {
        try {
            return completion.take().get();
        } catch (ExecutionException ex) {
            throw new AssertionError("unreachable", ex);
        }
    }

    /**
     * Close the room.
     * 
     * @implNote The default implementation calls
     * {@link ExecutorService#shutdownNow()} to terminate all remaining
     * engines.
     */
    @Override
    public void close() throws InterruptedException {
        executor.shutdownNow();
        while (!executor.awaitTermination(10, TimeUnit.SECONDS))
            ;
    }
}
