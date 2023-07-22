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

package uk.ac.lancs.fastcgi.mime;

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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stores large bodies in files, and keeps smaller ones in memory,
 * provided it's not already using a lot of memory.
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
    public SmartMorgue(Path cache, int singleThreshold, long memoryThreshold) {
        this.cache = cache;
        this.singleThreshold = singleThreshold;
        this.memoryThreshold = memoryThreshold;
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
        while ((got = data.read(buf, len, buf.length - len)) >= 0)
            len += got;
        if (len <= singleThreshold)
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
        while ((got = data.read(buf, len, buf.length - len)) >= 0)
            len += got;
        if (len * 2 <= singleThreshold)
            return new MemoryTextBody(cleaner, Arrays.copyOf(buf, len),
                                      memUsage);

        return createExternalBody(buf, len, data);
    }
}
