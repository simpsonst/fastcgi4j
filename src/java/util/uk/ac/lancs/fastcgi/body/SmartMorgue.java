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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.Cleaner;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stores large bodies in files, and keeps smaller ones in memory,
 * provided it's not already using a lot of memory. Any files created
 * are set to delete on exit.
 *
 * @author simpsons
 */
public final class SmartMorgue implements Morgue {
    private static final Cleaner cleaner = Cleaner.create();

    private final Path cache;

    private final int singleThreshold;

    private final long memoryThreshold;

    private final AtomicLong fileUsage = new AtomicLong();

    private final AtomicLong memUsage = new AtomicLong();

    /**
     * Create a smart morgue.
     * 
     * @param cache the directory in which to store large bodies
     * 
     * @param singleThreshold the maximum size for storing a body in
     * memory
     * 
     * @param memoryThreshold the maximum amount of memory to consume
     */
    SmartMorgue(Path cache, int singleThreshold, long memoryThreshold) {
        this.cache = cache;
        this.singleThreshold = singleThreshold;
        this.memoryThreshold = memoryThreshold;
    }

    private static final String DEFAULT_DIR_PROPERTY = "java.io.tmpdir";

    private static final int DEFAULT_SINGLE_THRESHOLD = 4096;

    private static final int DEFAULT_MEMORY_THRESHOLD = 2 * 1024 * 1024;

    /**
     * Prepare to build a smart morgue.
     * 
     * @return a builder set to use the system property
     * {@value #DEFAULT_DIR_PROPERTY} to locate the directory for
     * transient files, {@value #DEFAULT_SINGLE_THRESHOLD} as the
     * single-body memory threshold, and
     * {@value #DEFAULT_MEMORY_THRESHOLD} as the multi-body memory
     * threshold
     */
    public static Builder start() {
        return new Builder();
    }

    /**
     * Constructs a smart morgue.
     */
    public static class Builder {
        Path cache = Paths.get(System.getProperty(DEFAULT_DIR_PROPERTY));

        int singleThreshold = DEFAULT_SINGLE_THRESHOLD;

        long memoryThreshold = DEFAULT_MEMORY_THRESHOLD;

        Builder() {}

        /**
         * Create the morgue with current settings.
         * 
         * @return the configured morgue
         */
        public SmartMorgue build() {
            return new SmartMorgue(cache, singleThreshold, memoryThreshold);
        }

        /**
         * Set the directory for transient files from a system property.
         * 
         * @param propName the name of the property
         * 
         * @return this object
         * 
         * @throws NullPointerException if the path is {@code null}
         */
        public Builder atProperty(String propName) {
            return at(System.getProperty(propName));
        }

        /**
         * Set the directory for transient files from a system property
         * or from a default.
         * 
         * @param assumed the value to assume if the property is not set
         * 
         * @param propName the name of the property
         * 
         * @return this object
         * 
         * @throws NullPointerException if the path is {@code null}
         */
        public Builder atProperty(String propName, String assumed) {
            return at(System.getProperty(propName, assumed));
        }

        /**
         * Set the directory for transient files.
         * 
         * @param path the directory path
         * 
         * @return this object
         * 
         * @throws NullPointerException if the path is {@code null}
         */
        public Builder at(String path) {
            return at(Paths.get(path));
        }

        /**
         * Set the directory for transient files.
         * 
         * @param path the directory path
         * 
         * @return this object
         * 
         * @throws NullPointerException if the path is {@code null}
         */
        public Builder at(Path path) {
            this.cache = Objects.requireNonNull(path, "at");
            return this;
        }

        /**
         * Set the single-body memory threshold.
         * 
         * @param amount the new amount
         * 
         * @return this object
         * 
         * @throws IllegalArgumentException if the amount is negative
         */
        public Builder singleThreshold(int amount) {
            if (amount < 0)
                throw new IllegalArgumentException("-ve threshold: " + amount);
            this.singleThreshold = amount;
            return this;
        }

        /**
         * Set the multi-body memory threshold.
         * 
         * @param amount the new amount
         * 
         * @return this object
         * 
         * @throws IllegalArgumentException if the amount is negative
         */
        public Builder memoryThreshold(int amount) {
            if (amount < 0)
                throw new IllegalArgumentException("-ve threshold: " + amount);
            this.memoryThreshold = amount;
            return this;
        }
    }

    public Cleaner.Cleanable register(Object ref, Runnable action) {
        return cleaner.register(ref, action);
    }

    private BinaryBody createExternalBody(byte[] buf, int len, InputStream data)
        throws IOException {
        Path path = Files.createTempFile(cache, "fastcgi4j-", ".body");
        try (OutputStream out = Files.newOutputStream(path)) {
            final long size;
            if (buf != null) {
                out.write(buf, 0, len);
                size = len + data.transferTo(out);
            } else {
                size = data.transferTo(out);
            }
            return new FileBinaryBody(cleaner, path, size, fileUsage);
        } catch (Exception ex) {
            Files.deleteIfExists(path);
            throw ex;
        }
    }

    @Override
    public BinaryBody store(InputStream data) throws IOException {
        if (memUsage.get() > memoryThreshold)
            return createExternalBody(null, 0, data);

        byte[] buf = new byte[singleThreshold + 1];
        int len = 0;
        int got;
        while (len < buf.length &&
            (got = data.read(buf, len, buf.length - len)) >= 0)
            len += got;
        if (len < buf.length)
            /* The stream has ended before filling the buffer. */
            return new MemoryBinaryBody(cleaner, Arrays.copyOf(buf, len),
                                        memUsage);

        return createExternalBody(buf, len, data);
    }

    private TextBody createExternalBody(char[] buf, int len, Reader data)
        throws IOException {
        final Charset cs = StandardCharsets.UTF_8;
        Path path = Files.createTempFile(cache, "fastcgi4j-", ".body");
        try (Writer out = Files.newBufferedWriter(path, cs)) {
            final long size;
            if (buf != null) {
                out.write(buf, 0, len);
                size = len + data.transferTo(out);
            } else {
                size = data.transferTo(out);
            }
            out.flush();
            return new FileTextBody(cleaner, path, cs, Files.size(path), size,
                                    fileUsage);
        } catch (Exception ex) {
            Files.deleteIfExists(path);
            throw ex;
        }
    }

    @Override
    public TextBody store(Reader data) throws IOException {
        if (memUsage.get() > memoryThreshold)
            return createExternalBody(null, 0, data);

        char[] buf = new char[singleThreshold / 2 + 1];
        int len = 0;
        int got;
        while (len < buf.length &&
            (got = data.read(buf, len, buf.length - len)) >= 0)
            len += got;
        if (len < buf.length)
            /* The stream has ended before filling the buffer. */
            return new MemoryTextBody(cleaner, Arrays.copyOf(buf, len),
                                      memUsage);

        return createExternalBody(buf, len, data);
    }
}
