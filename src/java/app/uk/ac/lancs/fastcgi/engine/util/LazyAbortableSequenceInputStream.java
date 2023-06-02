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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import uk.ac.lancs.fastcgi.context.StreamAbortedException;

/**
 * Presents the contents of a sequence of source streams as its own
 * content. The sequence is defined later through calls to
 * {@link #submit(InputStream)}, and finally a call to
 * {@link #complete()} or {@link #abort(Throwable)}.
 * 
 * @author simpsons
 */
class LazyAbortableSequenceInputStream extends InputStream {
    private final Lock lock = new ReentrantLock();

    private final Condition ready = lock.newCondition();

    private final List<InputStream> sequence = new ArrayList<>();

    private final boolean closeOnError;

    private InputStream current;

    private boolean completed;

    private Throwable abortedReason;

    /**
     * Create a sequence input stream. This constructor does not invoke
     * the argument, so it will not block.
     * 
     * @param source the source of contributing streams
     * 
     * @param closeOnError {@code true} if all remaining source streams
     * are to be closed on the first exception
     */
    public LazyAbortableSequenceInputStream(boolean closeOnError) {
        this.closeOnError = closeOnError;
    }

    /**
     * Submit a new stream to the sequence.
     * 
     * @param stream the stream to be appended to the sequence
     * 
     * @throws IllegalStateException if the sequence has already been
     * completed or aborted
     */
    public void submit(InputStream stream) {
        Objects.requireNonNull(stream, "stream");
        try {
            lock.lock();
            if (abortedReason != null)
                throw new IllegalStateException("aborted", abortedReason);
            if (completed) throw new IllegalStateException("completed");
            sequence.add(stream);
            ready.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Abort the sequence. The supplied reason will be the cause of an
     * {@link IOException} thrown by further attempts to read the
     * stream. If the stream has already been completed or aborted, this
     * call has no effect.
     * 
     * @param reason the reason for abortion
     */
    public void abort(Throwable reason) {
        reason = Objects.requireNonNull(reason, "reason");
        try {
            lock.lock();
            if (abortedReason != null) return;
            if (completed) return;
            abortedReason = reason;
            ready.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Complete the sequence. Further attempts to read beyond the
     * streams already supplied will yield end-of-file. If the stream
     * has already been completed or aborted, this call has no effect.
     */
    public void complete() {
        try {
            lock.lock();
            if (abortedReason != null) return;
            if (completed) return;
            completed = true;
            ready.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Ensure that a source stream is ready.
     * 
     * @return {@code false} if a source stream is ready; {@code true}
     * if there are no more source streams
     */
    private boolean ensure(boolean throwAbort) throws StreamAbortedException {
        if (current != null) return false;
        boolean interrupted = false;
        try {
            lock.lock();
            boolean r;
            while ((r = sequence.isEmpty()) && !completed &&
                abortedReason == null) {
                try {
                    ready.await();
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
            if (throwAbort && abortedReason != null)
                throw new StreamAbortedException(abortedReason);
            if (r) return true;
            current = sequence.remove(0);
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void clear() throws IOException {
        current.close();
        current = null;
    }

    /**
     * Read a byte. This call may block while the source enumeration
     * blocks. If the source stream throws an exception, and
     * close-on-error is set, all remaining streams are closed, and
     * subsequent exceptions are suppressed.
     * 
     * @return the next byte as a non-negative value; or {@code -1} if
     * there is no more data
     * 
     * @throws IOException if an I/O error occurred
     */
    @Override
    public int read() throws IOException {
        try {
            do {
                if (ensure(true)) return -1;
                int rc = current.read();
                if (rc >= 0) return rc;
                clear();
            } while (true);
        } catch (IOException | Error | RuntimeException ex) {
            optionalCleanUp(ex);
            throw new AssertionError("unreachable");
        }
    }

    private void optionalCleanUp(Throwable t) throws IOException {
        if (closeOnError) {
            cleanUp(t);
            throw new AssertionError("unreachable");
        }
        try {
            throw t;
        } catch (IOException | Error | RuntimeException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new AssertionError("bad call", ex);
        }
    }

    /**
     * Close all remaining source streams.
     * 
     * @param suppressor an exception that instigated the closing of the
     * remaining streams, and suppresses further exceptions; or
     * {@code null} if no exception instigated the closure
     * 
     * @throws IOException if an I/O error occurred in closing a source
     * stream; or the supplied suppressing exception
     */
    private void cleanUp(Throwable suppressor) throws IOException {
        final boolean throwOnAbort = suppressor != null;
        while (!ensure(throwOnAbort)) {
            try {
                clear();
            } catch (IOException ex) {
                if (suppressor == null)
                    suppressor = ex;
                else if (ex != suppressor) suppressor.addSuppressed(ex);
            }
        }
        if (suppressor != null) try {
            throw suppressor;
        } catch (IOException | Error | RuntimeException ex) {
            throw ex;
        } catch (Throwable t) {
            throw new AssertionError("bad call", t);
        }
    }

    /**
     * Close the stream. Each base stream is closed. This call may block
     * while the source enumeration blocks. If a source stream throws an
     * exception, it will be thrown, and exceptions from subsequent
     * streams are suppressed.
     * 
     * @throws IOException if an I/O error occurred
     */
    @Override
    public void close() throws IOException {
        cleanUp(null);
    }

    /**
     * Estimate the number of bytes available. This implementation
     * cannot give accurate results, though it will never overestimate
     * as long as the source streams don't either. It may return zero if
     * the current source stream is at end-of-file, for example. This
     * call may block while the source enumeration blocks. If the source
     * stream throws an exception, and close-on-error is set, all
     * remaining streams are closed, and subsequent exceptions are
     * suppressed.
     * 
     * @return an estimation of the number of bytes available
     * 
     * @throws IOException if an I/O error occurred
     */
    @Override
    public int available() throws IOException {
        try {
            if (ensure(true)) return 0;
            return current.available();
        } catch (IOException | Error | RuntimeException ex) {
            cleanUp(ex);
            throw new AssertionError("unreachable");
        }
    }

    /**
     * Skip a number of bytes. This call may block while the source
     * enumeration blocks. If the source stream throws an exception, and
     * close-on-error is set, all remaining streams are closed, and
     * subsequent exceptions are suppressed.
     * 
     * @param n the maximum number of bytes to skip
     * 
     * @return the number of bytes skipped
     * 
     * @throws IOException if an I/O error occurred
     */
    @Override
    public long skip(long n) throws IOException {
        try {
            if (n == 0) return 0;
            if (n < 0) throw new IllegalArgumentException("-ve skip " + n);
            if (ensure(true)) return 0;
            int rc = current.read();
            if (rc < 0) {
                clear();
                return 0;
            }
            if (n == 1) return 1;
            return 1 + current.skip(n - 1);
        } catch (IOException | Error | RuntimeException ex) {
            optionalCleanUp(ex);
            throw new AssertionError("unreachable");
        }
    }

    /**
     * Read bytes to a position in an array. This call may block while
     * the source enumeration blocks. If the source stream throws an
     * exception, and close-on-error is set, all remaining streams are
     * closed, and subsequent exceptions are suppressed.
     * 
     * @param b the containing array
     * 
     * @param off the offset into the array of the element to store the
     * first byte in
     * 
     * @param len the maximum number of bytes to read
     * 
     * @return the number of bytes read; or {@code -1} if end-of-file is
     * reached
     * 
     * @throws IOException if an I/O error occurred
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            do {
                if (ensure(true)) return -1;
                int rc = current.read(b, off, len);
                if (rc >= 0) return rc;
                clear();
            } while (true);
        } catch (IOException | Error | RuntimeException ex) {
            optionalCleanUp(ex);
            throw new AssertionError("unreachable");
        }
    }
}
