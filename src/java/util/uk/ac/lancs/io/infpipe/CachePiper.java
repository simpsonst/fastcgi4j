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
 *  Author: Steven Simpson <s.simpson@lancaster.ac.uk>
 */

package uk.ac.lancs.io.infpipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ref.Cleaner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates pipes that store small amounts in RAM and the rest to the
 * file system. The stream is split into chunks as it is provided. Three
 * parameters determine the policy for using RAM or backing store for a
 * given chunk:
 * 
 * <dl>
 * 
 * <dt>memory chunk size
 * 
 * <dd>This is the size of each memory chunk (a byte array). Data exists
 * between a read index and a write index, and is moved back to the
 * start of the array to reclaim space. This parameter is controlled
 * with {@link Builder#memChunkSize}, and defaults to
 * {@value #MEM_CHUNK_SIZE}.
 * 
 * <dt>RAM threshold
 * 
 * <dd>The amount of memory in use by all chunks created by this piper
 * is tracked. When it exceeds this threshold, the next chunk created
 * will likely be a file chunk. This parameter is controlled with
 * {@link Builder#ramThreshold}, and defaults to
 * {@value #RAM_THRESHOLD}.
 * 
 * <dt>max file size
 * 
 * <dd>When the file backing a chunk reaches this size, no more bytes
 * are written to it, and a new chunk is required. This parameter is
 * controlled with {@link Builder#maxFileSize}, and defaults to
 * {@value #MAX_FILE_SIZE}.
 * 
 * </dl>
 * 
 * <p>
 * Note that if the reader of a pipe catches up with the writer while in
 * a file chunk, the chunk switches to a read-only mode, and a new chunk
 * is required. If the old chunk stored fewer bytes than the memory
 * chunk size, the reader is deemed to be close enough to the writer for
 * a memory chunk to be used, regardless of the current memory tally.
 *
 * @author simpsons
 */
public final class CachePiper implements Piper {
    private static final Cleaner cleaner = Cleaner.create();

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
     * The default size in bytes for chunk files, namely
     * {@value}, overridden by {@link Builder#maxFileSize(long)}
     */
    public static final long MAX_FILE_SIZE = 1 * 1024 * 1024;

    /**
     * The default size in bytes of each memory chunk, namely
     * {@value}, overridden by {@link Builder#memChunkSize(int)}
     */
    public static final int MEM_CHUNK_SIZE = 1024;

    /**
     * The {@linkplain System#getProperties() system property} whose
     * value is the default directory for chunk files
     */
    public static final String TMPDIR_SYSPROP = "java.io.tmpdir";

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
         * property} named by {@link #TMPDIR_SYSPROP}.
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
         * Set the prefix and suffix of chunk files. The defaults are
         * {@value #PREFIX} and {@value #SUFFIX}.
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
         * instead. The default is given by {@link #RAM_THRESHOLD}.
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
         * used instead. The default is given by {@link #MAX_FILE_SIZE}.
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
         * bigger than this. The default is given by
         * {@link #MEM_CHUNK_SIZE}.
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
         * 
         * @constructor
         */
        public CachePiper create() {
            return new CachePiper(dir, prefix, suffix, maxFileSize,
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
    CachePiper(Path dir, String prefix, String suffix, long maxFileSize,
               int memChunkSize, int ramThreshold) {
        this.dir = dir;
        this.prefix = prefix;
        this.suffix = suffix;
        this.maxFileSize = maxFileSize;
        this.memChunkSize = memChunkSize;
        this.ramThreshold = ramThreshold;
    }

    @Override
    public Pipe newPipe() {
        return new MyPipe();
    }

    private class MyPipe implements Pipe {
        final LazyAbortableSequenceInputStream sequence;

        public MyPipe() {
            /* Create an input stream from an extensible sequence. */
            sequence =
                new LazyAbortableSequenceInputStream(false, this::sinkClosed);
        }

        private volatile boolean sinkClosed = false;

        private void sinkClosed() {
            this.sinkClosed = true;
        }

        private Chunk lastChunk;

        private long lastWritten = -1;

        private Throwable abortedReason;

        private boolean closed;

        private Chunk getLastChunk() throws IOException {
            if (lastChunk != null) return lastChunk;
            if ((lastWritten < 0 || lastWritten >= memChunkSize) &&
                memoryUsage.get() >= ramThreshold) {
                /* Create a temporary random-access file. */
                Path path = Files.createTempFile(dir, prefix, suffix);
                RandomAccessFile file =
                    new RandomAccessFile(path.toFile(), "rw");

                /* When the file handle is garbage-collected, delete the
                 * file. */
                path.toFile().deleteOnExit();
                cleaner.register(file, () -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ex) {
                        Logger.getLogger(CachePiper.class.getName())
                            .log(Level.SEVERE,
                                 "deleting chunk " + path.toString(), ex);
                    }
                });

                lastChunk = new FileChunk(file, maxFileSize);
                lastWritten = 0;
            } else {
                lastChunk = new MemoryChunk(memChunkSize, memoryUsage);
                lastWritten = -1;
            }
            sequence.submit(lastChunk.getStream());
            return lastChunk;
        }

        private final OutputStream outputStream = new OutputStream() {
            @Override
            public void flush() throws IOException {
                if (closed) throw new IOException("closed");
                if (abortedReason != null)
                    throw new IOException("stream aborted", abortedReason);
            }

            @Override
            public void close() throws IOException {
                if (closed) return;
                closed = true;
                if (lastChunk != null) lastChunk.complete();
                lastChunk = null;
                sequence.complete();
            }

            private void innerWrite(byte[] b, int off, int len)
                throws IOException {
                while (len > 0) {
                    if (sinkClosed) {
                        /* The reader has closed the other end of the
                         * pipe. None of our data goes anywhere now */
                        if (lastChunk != null) lastChunk.complete();
                        lastChunk = null;
                        throw new SinkClosedException();
                    }
                    Chunk chunk = getLastChunk();
                    int done = chunk.write(b, off, len);
                    if (done == 0) lastChunk = null;
                    if (lastWritten >= 0) lastWritten += done;
                    off += done;
                    len -= done;
                }
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                Objects.checkFromIndexSize(off, len, b.length);
                if (closed) throw new IOException("closed");
                if (abortedReason != null)
                    throw new IOException("stream aborted", abortedReason);
                innerWrite(b, off, len);
            }

            private final byte[] buf = new byte[1];

            @Override
            public void write(int b) throws IOException {
                if (closed) throw new IOException("closed");
                if (abortedReason != null)
                    throw new IOException("stream aborted", abortedReason);
                buf[0] = (byte) b;
                innerWrite(buf, 0, 1);
            }
        };

        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public void abort(Throwable reason) {
            if (closed) return;
            if (abortedReason != null) return;
            abortedReason = reason;
            sequence.abort(reason);
        }

        @Override
        public InputStream getInputStream() {
            return sequence;
        }
    }
}
