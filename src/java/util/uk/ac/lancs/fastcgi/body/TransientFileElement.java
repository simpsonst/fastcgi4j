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

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deletes a file when garbage-collected, and accounts for its size.
 *
 * @author simpsons
 */
abstract class TransientFileElement {
    private static class State implements Runnable {
        protected final Path path;

        protected final long size;

        private final AtomicLong usage;

        State(Path path, long size, AtomicLong usage) {
            this.path = path;
            this.size = size;
            this.usage = usage;

            this.usage.addAndGet(size);
            this.path.toFile().deleteOnExit();
        }

        @Override
        public void run() {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ex) {
                throw new AssertionError("unreachable", ex);
            } finally {
                usage.addAndGet(-size);
            }
        }
    }

    private final State state;

    /**
     * Get the file size.
     * 
     * @return the file size
     */
    protected long size() {
        return state.size;
    }

    /**
     * Get the file path.
     * 
     * @return the file path
     */
    protected Path path() {
        return state.path;
    }

    private final Cleaner.Cleanable cleanable;

    /**
     * Set a file to be deleted upon garbage collection.
     * 
     * @param cleaner an object manager
     * 
     * @param path the path to the file
     * 
     * @param size the size of the file
     * 
     * @param usage a counter to be incremented now by the file size,
     * and decremented by the same amount upon garbage collection
     */
    protected TransientFileElement(Cleaner cleaner, Path path, long size,
                                   AtomicLong usage) {
        this.state = new State(path, size, usage);
        cleanable = cleaner.register(this, this.state);
    }
}
