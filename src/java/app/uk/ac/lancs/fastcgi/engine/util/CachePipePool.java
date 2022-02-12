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

package uk.ac.lancs.fastcgi.engine.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.SequenceInputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates pipes that store small amounts in RAM and the rest to the
 * file system.
 *
 * @author simpsons
 */
public final class CachePipePool implements PipePool {
    private final Path dir;

    private final String prefix;

    private final String suffix;

    private final long maxFileSize;

    private final int memChunkSize;

    private final AtomicLong memoryUsage = new AtomicLong(0);

    private final int ramThreshold;

    /**
     * The default threshold in bytes to switch to caching stream data
     * in file chunks, namely {@value}, overridden by
     * {@link Builder#ramThreshold(int)}
     */
    public static final int RAM_THRESHOLD = 1 * 1024 * 1024;

    /**
     * The default prefix for chunk files, namely {@value}, overridden
     * by the first argument to {@link Builder#format(String, String)}
     */
    public static final String PREFIX = "fastcgi-";

    /**
     * The default suffix for chunk files, namely {@value}, overridden
     * by the second argument to {@link Builder#format(String, String)}
     */
    public static final String SUFFIX = ".chunk";

    /**
     * The default size in bytes for chunk files, namely {@value},
     * overridden by {@link Builder#maxFileSize(long)}
     */
    public static final long MAX_FILE_SIZE = 1 * 1024 * 1024;

    /**
     * The default size in bytes of each memory chunk, namely {@value},
     * overridden by {@link Builder#memChunkSize(int)}
     */
    public static final int MEM_CHUNK_SIZE = 1024;

    private static final String TMPDIR_SYSPROP = "java.io.tmpdir";

    /**
     * Start building a pool.
     * 
     * @return the new builder
     */
    public static Builder start() {
        return new Builder();
    }

    /**
     * Collects the parameters for building a pipe pool.
     */
    public static class Builder {
        private Path dir = Paths.get(System.getProperty(TMPDIR_SYSPROP));

        private String prefix = PREFIX;

        private String suffix = SUFFIX;

        private int ramThreshold = RAM_THRESHOLD;

        private int memChunkSize = MEM_CHUNK_SIZE;

        private long maxFileSize = MAX_FILE_SIZE;

        Builder() {}

        /**
         * Set the directory for creating temporary files. The default
         * is specified by the {@linkplain System#getProperties() system
         * property} {@value #TMPDIR_SYSPROP}.
         * 
         * @param dir the new directory
         * 
         * @return this builder
         * 
         * @throws NullPointerException if the argument is {@code null}
         */
        public Builder directory(Path dir) {
            Objects.requireNonNull(dir, "dir");
            this.dir = dir;
            return this;
        }

        /**
         * Set the prefix and suffix of chunk files.
         * 
         * @param prefix the prefix of all chunk files
         * 
         * @param suffix the suffix of all chunk files
         * 
         * @return this builder
         * 
         * @throws NullPointerException if any argument is {@code null}
         */
        public Builder format(String prefix, String suffix) {
            Objects.requireNonNull(prefix, "prefix");
            Objects.requireNonNull(suffix, "suffix");
            this.prefix = prefix;
            this.suffix = suffix;
            return this;
        }

        /**
         * Set the RAM threshold. The pool keeps track of the number of
         * RAM chunks currently in use. While the number of bytes used
         * by them exceeds this threshold, file chunks will be created
         * instead.
         * 
         * @param ramThreshold the RAM threshold in bytes
         * 
         * @return this builder
         * 
         * @throws IllegalArgumentException if the argument is negative
         */
        public Builder ramThreshold(int ramThreshold) {
            if (ramThreshold < 0)
                throw new IllegalArgumentException("-ve RAM threshold "
                    + ramThreshold);
            this.ramThreshold = ramThreshold;
            return this;
        }

        /**
         * Set the maximum size of file chunks. When a chunk file has
         * this many bytes written to it, a new chunk is created and
         * used instead.
         * 
         * @param maxFileSize the chunk file size in bytes
         * 
         * @return this builder
         * 
         * @throws IllegalArgumentException if the argument is negative
         */
        public Builder maxFileSize(long maxFileSize) {
            if (maxFileSize < 0)
                throw new IllegalArgumentException("-ve file chunk size "
                    + maxFileSize);
            this.maxFileSize = maxFileSize;
            return this;
        }

        /**
         * Set the size of RAM chunks. Each new chunk keeps a buffer no
         * bigger than this.
         * 
         * @param memChunkSize the RAM chunk size
         * 
         * @return this builder
         * 
         * @throws IllegalArgumentException if the argument is negative
         */
        public Builder memChunkSize(int memChunkSize) {
            if (memChunkSize < 0)
                throw new IllegalArgumentException("-ve RAM chunk size");
            this.memChunkSize = memChunkSize;
            return this;
        }

        /**
         * Create a pool with the current configuration.
         * 
         * @return a pool with the required configuration
         */
        public CachePipePool create() {
            return new CachePipePool(dir, prefix, suffix, maxFileSize,
                                     memChunkSize, ramThreshold);
        }
    }

    /**
     * Create a pool.
     * 
     * @param dir the directory in which larger chunks are stored
     * 
     * @param prefix the prefix of filenames of chunk files
     * 
     * @param suffix the suffix of filenames of chunk files
     * 
     * @param maxFileSize the maximum size of a chunk file
     * 
     * @param memChunkSize the size of an internal chunk
     */
    CachePipePool(Path dir, String prefix, String suffix, long maxFileSize,
                  int memChunkSize, int ramThreshold) {
        this.dir = dir;
        this.prefix = prefix;
        this.suffix = suffix;
        this.maxFileSize = maxFileSize;
        this.memChunkSize = memChunkSize;
        this.ramThreshold = ramThreshold;
    }

    private static class ActionReference<T> extends WeakReference<T> {
        private final Runnable action;

        public ActionReference(T referent, Runnable action) {
            super(referent, oldChunks);
            this.action = action;
        }
    }

    private static final ReferenceQueue<Object> oldChunks =
        new ReferenceQueue<>();

    private static ActionReference<?> nextRef() {
        return (ActionReference<?>) oldChunks.poll();
    }

    private static void clearActions() {
        ActionReference<?> ref;
        while ((ref = nextRef()) != null)
            ref.action.run();
    }

    @Override
    public Pipe newPipe() {
        final QueuedEnumeration<InputStream> queue = new QueuedEnumeration<>();
        final SequenceInputStream inputStream = new SequenceInputStream(queue);

        return new Pipe() {
            private Chunk lastChunk;

            private Chunk getLastChunk() throws IOException {
                if (lastChunk != null) return lastChunk;
                if (memoryUsage.get() >= ramThreshold) {
                    /* Create a temporary random-access file. */
                    Path path = Files.createTempFile(dir, prefix, suffix);
                    RandomAccessFile file =
                        new RandomAccessFile(path.toFile(), "rw");

                    /* When the file handle is garbage-collected, delete
                     * the file. */
                    path.toFile().deleteOnExit();
                    new ActionReference<RandomAccessFile>(file, () -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            Logger.getLogger(CachePipePool.class.getName())
                                .log(Level.SEVERE, null, ex);
                        }
                    });

                    /* Get rid of old files now. */
                    clearActions();

                    lastChunk = new FileChunk(file, maxFileSize);
                } else {
                    lastChunk = new MemoryChunk(memChunkSize, memoryUsage);
                }
                queue.submit(lastChunk.getStream());
                return lastChunk;
            }

            private void clearLastChunk() throws IOException {
                lastChunk.complete();
                lastChunk = null;
            }

            private final OutputStream outputStream = new OutputStream() {
                @Override
                public void close() throws IOException {
                    clearLastChunk();
                    queue.complete();
                }

                @Override
                public void write(byte[] b, int off, int len)
                    throws IOException {
                    while (len > 0) {
                        Chunk chunk = getLastChunk();
                        int done = chunk.write(b, off, len);
                        if (done == 0) clearLastChunk();
                        off += done;
                        len -= done;
                    }
                }

                private final byte[] buf = new byte[1];

                @Override
                public void write(int b) throws IOException {
                    buf[0] = (byte) b;
                    write(buf, 0, 1);
                }
            };

            @Override
            public OutputStream getOutputStream() {
                return outputStream;
            }

            @Override
            public void abort(Throwable reason) {
                if (lastChunk == null) {
                    lastChunk = new MemoryChunk(1, memoryUsage);
                    queue.submit(lastChunk.getStream());
                    queue.complete();
                }
                lastChunk.abort(reason);
            }

            @Override
            public InputStream getInputStream() {
                return inputStream;
            }
        };
    }
}
