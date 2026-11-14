// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2022,2023,2026, Lancaster University
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

import java.io.IOException;
import java.util.Objects;

/**
 * Holds the result of an engine exiting as well as a key associated
 * with the transport.
 *
 * @param <K> the transport key type
 * 
 * @author simpsons
 */
public final class EngineResult<K> {
    private final K key;

    private final Throwable cause;

    private EngineResult(K key, Throwable cause) {
        this.key = key;
        this.cause = cause;
    }

    /**
     * Create a result for an exhausted engine.
     *
     * @param <K> the transport key type
     * 
     * @param key the transport key
     * 
     * @return the requested result
     */
    public static <K> EngineResult<K> exhausted(K key) {
        return new EngineResult<>(key, null);
    }

    /**
     * Create a result for an engine that terminated with an exception.
     *
     * @param <K> the transport key type
     * 
     * @param key the transport key
     * 
     * @param cause the cause of termination of the engine
     * 
     * @return the requested result
     */
    public static <K> EngineResult<K> failed(K key, Throwable cause) {
        return new EngineResult<>(key, Objects.requireNonNull(cause, "cause"));
    }

    /**
     * Get the key associated with the transport.
     * 
     * @return the transport key
     */
    public K key() {
        return key;
    }

    /**
     * Test the result for exceptions.
     * 
     * @throws IOException if the engine terminated with an I/O error
     * 
     * @throws InterruptedException if the engine was interrupted
     * 
     * @throws RuntimeException if the engine failed with a run-time
     * exception
     * 
     * @throws Error if the engine failed with an error
     */
    public void test() throws IOException, InterruptedException {
        if (cause == null) return;
        try {
            throw cause;
        } catch (IOException | InterruptedException | RuntimeException |
                 Error ex) {
            throw ex;
        } catch (Throwable t) {
            throw new AssertionError("unreachable", cause);
        }
    }
}
