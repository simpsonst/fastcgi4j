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

package uk.ac.lancs.fastcgi.engine.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Provides a blocking enumeration to which elements can be added
 * asynchronously. A queue is maintained internally. Methods of
 * {@link Enumeration} block until the queue is populated.
 * {@link #submit(Object)} and {@link #complete()} populate the queue.
 * 
 * @author simpsons
 */
final class BlockingEnumeration<E> implements Enumeration<E> {
    private final List<E> queue = new ArrayList<>();

    private Exception closer = null;

    /**
     * Determine whether there are more elements. If the queue is empty
     * but the enumeration remains open, the call blocks until
     * {@link #submit(Object)} or {@link #complete()} is called.
     * 
     * @return {@code true} if there are more elements; {@code false}
     * otherwise
     */
    @Override
    public synchronized boolean hasMoreElements() {
        return hasMoreElementsInternal();
    }

    private boolean hasMoreElementsInternal() {
        assert Thread.holdsLock(this);
        boolean empty = true;
        boolean interrupted = false;
        while ((empty = queue.isEmpty()) && closer == null) {
            try {
                wait();
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }
        if (interrupted) Thread.currentThread().interrupt();
        return !empty;
    }

    /**
     * Get the next element. If the queue is empty but the enumeration
     * remains open, the call blocks until {@link #submit(Object)} or
     * {@link #complete()} is called.
     * 
     * @return the next element
     * 
     * @throws NoSuchElementException if the enumeration has been
     * completed and all previously submitted elements have been
     * retrieved
     */
    @Override
    public synchronized E nextElement() {
        if (!hasMoreElementsInternal()) throw new NoSuchElementException();
        E result = queue.remove(0);
        System.err.printf("served %s%n", result);
        return result;
    }

    /**
     * Submit an element to the enumeration.
     * 
     * @param elem the element to be submitted
     * 
     * @throws IllegalStateException if the enumeration has already been
     * completed, with an exception recording the stack trace of the
     * completing thread as the cause
     */
    public void submit(E elem) {
        Objects.requireNonNull(elem, "elem");
        submitInternal(elem);
    }

    private synchronized void submitInternal(E elem) {
        assert Thread.holdsLock(this);
        if (closer != null) throw new IllegalStateException("closed", closer);
        queue.add(elem);
        notify();
    }

    /**
     * Complete this enumeration. The first call creates and stores a
     * dummy exception to record the stack trace. If
     * {@link #submit(Object)} is subsequently called, this exception
     * will appear as the cause of the thrown exception. Subsequent
     * calls do not update the dummy exception.
     */
    public synchronized void complete() {
        if (closer != null) return;
        closer = new Exception("stack");
        notify();
    }
}
