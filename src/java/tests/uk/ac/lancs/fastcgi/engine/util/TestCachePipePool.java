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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;
import org.junit.Test;
import uk.ac.lancs.fastcgi.StreamAbortedException;

/**
 *
 * @author simpsons
 */
public class TestCachePipePool extends TestCase {
    final PipePool pool =
        CachePipePool.start().ramThreshold(200).maxFileSize(1000).create();

    @Test
    public void testMemoryChunk() throws IOException {
        final long seed = 42;
        AtomicLong usage = new AtomicLong(1000);
        MemoryChunk chunk = new MemoryChunk(256, usage);
        byte[] buf = new byte[512];
        Random rng1 = new Random(seed);
        rng1.nextBytes(buf);
        {
            int rc1 = chunk.write(buf, 0, 80);
            assertEquals("write 1", 80, rc1);
            assertEquals("usage 1", 1080, usage.get());
            int rc2 = chunk.write(buf, rc1, 90);
            assertEquals("write 2", 90, rc2);
            assertEquals("usage 2", 1170, usage.get());
            int rc3 = chunk.write(buf, rc1 + rc2, 100);
            assertEquals("write 3 truncated", 86, rc3);
            assertEquals("usage 3", 1256, usage.get());
        }
        {
            byte[] buf2 = new byte[1024];
            int rc = chunk.read(buf2, 0, buf2.length);
            assertEquals("read all", 256, rc);
            assertEquals("usage read all", 1000, usage.get());
            assertEquals("preserved", 0,
                         Arrays.compare(buf, 0, rc, buf2, 0, rc));
        }
    }

    @Test
    public void testFileChunk() throws IOException {
        Path dir = Paths.get(System.getProperty(CachePipePool.TMPDIR_SYSPROP));
        final long seed = 42;
        Path path = Files.createTempFile(dir, CachePipePool.PREFIX,
                                         CachePipePool.SUFFIX);
        RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw");
        path.toFile().deleteOnExit();
        FileChunk chunk = new FileChunk(file, 256);
        byte[] buf = new byte[512];
        Random rng1 = new Random(seed);
        rng1.nextBytes(buf);
        {
            int rc1 = chunk.write(buf, 0, 80);
            assertEquals("write 1", 80, rc1);
            int rc2 = chunk.write(buf, rc1, 90);
            assertEquals("write 2", 90, rc2);
            int rc3 = chunk.write(buf, rc1 + rc2, 100);
            assertEquals("write 3 truncated", 86, rc3);
        }
        {
            byte[] buf2 = new byte[1024];
            int rc = chunk.read(buf2, 0, buf2.length);
            assertEquals("read all", 256, rc);
            assertEquals("preserved", 0,
                         Arrays.compare(buf, 0, rc, buf2, 0, rc));
        }
    }

    @Test
    public void testClosePoolImmediately() throws IOException {
        Pipe pipe = pool.newPipe();
        pipe.getOutputStream().close();
    }

    @Test
    public void testRunThroughPool() throws IOException, InterruptedException {
        Pipe pipe = pool.newPipe();
        final long seed = 71;
        Random rng1 = new Random(seed);
        Random rng2 = new Random(seed);

        AtomicLong totalOut = new AtomicLong(0);
        AtomicReference<Throwable> writeError = new AtomicReference<>();
        Thread writer = new Thread() {
            @Override
            public void run() {
                Random sizer = new Random(42);
                final int calls = 50 + sizer.nextInt(50);
                try (OutputStream out = pipe.getOutputStream()) {
                    for (int i = 0; i < calls; i++) {
                        byte[] buf = new byte[100 + sizer.nextInt(64) * 4];
                        assert buf.length % 4 == 0;
                        rng1.nextBytes(buf);
                        out.write(buf);
                        totalOut.addAndGet(buf.length);
                        Thread.sleep(40);
                    }
                } catch (Throwable ex) {
                    writeError.set(ex);
                }
            }
        };
        writer.start();

        try (InputStream in = pipe.getInputStream()) {
            byte[] buf = new byte[256];
            int c;
            long total = 0;
            while ((c = in.readNBytes(buf, 0, buf.length)) > 0) {
                byte[] exp = new byte[c];
                assert c % 4 == 0;
                rng2.nextBytes(exp);
                assertEquals("block " + total + " to " + (total + c), 0,
                             Arrays.compare(exp, 0, c, buf, 0, c));
                total += c;
                Thread.sleep(70);
            }
            assertEquals("same amount", totalOut.get(), total);
        }

        writer.join();
        assertNull("writing exception", writeError.get());
    }

    @Test
    public void
        testRunThroughPoolAndAbort() throws IOException, InterruptedException {
        Pipe pipe = pool.newPipe();
        final long seed = 71;
        Random rng1 = new Random(seed);
        Random rng2 = new Random(seed);

        AtomicLong totalOut = new AtomicLong(0);
        AtomicReference<Throwable> writeError = new AtomicReference<>();
        Thread writer = new Thread() {
            @Override
            public void run() {
                Random sizer = new Random(42);
                final int calls = 20 + sizer.nextInt(20);
                try (OutputStream out = pipe.getOutputStream()) {
                    for (int i = 0; i < calls; i++) {
                        byte[] buf = new byte[100 + sizer.nextInt(64) * 4];
                        assert buf.length % 4 == 0;
                        rng1.nextBytes(buf);
                        out.write(buf);
                        totalOut.addAndGet(buf.length);
                        Thread.sleep(40);
                    }
                    pipe.abort(new RuntimeException("dummy"));
                } catch (Throwable ex) {
                    writeError.set(ex);
                }
            }
        };
        writer.start();

        try (InputStream in = pipe.getInputStream()) {
            byte[] buf = new byte[32];
            int c;
            long total = 0;
            while ((c = in.readNBytes(buf, 0, buf.length)) > 0) {
                byte[] exp = new byte[c];
                rng2.nextBytes(exp);
                assertEquals("block " + total + " to " + (total + c), 0,
                             Arrays.compare(exp, 0, c, buf, 0, c));
                total += c;
                Thread.sleep(70);
            }
            assertFalse("unreached", true);
        } catch (StreamAbortedException ex) {
            assertTrue("reached", true);
        }

        writer.join();
        assertNull("writing exception", writeError.get());
    }
}
