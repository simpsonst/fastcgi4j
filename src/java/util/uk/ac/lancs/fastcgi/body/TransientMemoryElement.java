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

package uk.ac.lancs.fastcgi.body;

import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Accounts for in-memory storage when garbage-collected.
 * 
 * @author simpsons
 */
abstract class TransientMemoryElement {
    private static class State implements Runnable {
        protected final long size;

        private final AtomicLong usage;

        public State(long size, AtomicLong usage) {
            this.size = size;
            this.usage = usage;

            this.usage.addAndGet(size);
        }

        @Override
        public void run() {
            usage.addAndGet(-size);
        }
    }

    private final State state;

    /**
     * Get the amount of member used in bytes.
     * 
     * @return the memory usage
     */
    protected long size() {
        return state.size;
    }

    private final Cleaner.Cleanable cleanable;

    /**
     * Account for memory being released.
     * 
     * @param cleaner an object manager
     * 
     * @param size the number of bytes allocated
     * 
     * @param usage a counter to be incremented now by the size, and
     * decremented by the same amount upon garbage collection
     */
    protected TransientMemoryElement(Cleaner cleaner, long size,
                                     AtomicLong usage) {
        this.state = new State(size, usage);
        cleanable = cleaner.register(this, this.state);
    }
}
