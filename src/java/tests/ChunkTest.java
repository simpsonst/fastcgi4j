
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.LongConsumer;
import uk.ac.lancs.http.ChunkedInputStream;
import uk.ac.lancs.http.ChunkedOutputStream;
import uk.ac.lancs.io.CountingInputStream;
import uk.ac.lancs.io.CountingOutputStream;

// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2026, Lancaster University
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

/**
 * Tests that a file is fully preserved when chunked and de-chunked.
 *
 * @author simpsons
 */
public class ChunkTest {
    private static class Counter implements LongConsumer {
        long total = 0;

        public long get() {
            return total;
        }

        @Override
        public void accept(long amount) {
            total += amount;
        }
    }

    private static OutputStream digest(OutputStream s, MessageDigest d) {
        return new DigestOutputStream(s, d);
    }

    private static InputStream digest(InputStream s, MessageDigest d) {
        return new DigestInputStream(s, d);
    }

    private static OutputStream count(OutputStream s, LongConsumer c) {
        return new CountingOutputStream(s, c);
    }

    private static InputStream count(InputStream s, LongConsumer c) {
        return new CountingInputStream(s, c);
    }

    private static InputStream decode(InputStream s) {
        return new ChunkedInputStream(s);
    }

    private static OutputStream encode(OutputStream s) {
        return new ChunkedOutputStream(s);
    }

    private static final String ALGO = "MD5";

    public static void main(String[] args) throws Exception {
        ExecutorService exec = Executors.newCachedThreadPool();
        var digIn = MessageDigest.getInstance(ALGO);
        var digOut = MessageDigest.getInstance(ALGO);
        var outCounter = new Counter();
        var inCounter = new Counter();
        var midCounter = new Counter();

        /* This end of the pipe gets wrapped, so we don't close it
         * directly. However, we do need a reference to it so the other
         * end of the pipe can bind to it. */
        var decodeOut = new PipedOutputStream();

        try (/* Count and digest the result. */ var out =
            count(digest(OutputStream.nullOutputStream(), digOut), outCounter);
             /* Decode the output of the pipe. */ var decodeIn =
                 decode(count(new PipedInputStream(decodeOut), midCounter));
             /* Count and digest the file. */
             var in = count(digest(new FileInputStream(args[0]), digIn),
                            inCounter)) {

            /* In the background, transfer the file through the encoding
             * end of the pipe. */
            exec.submit(() -> {
                try (var encodeOut = encode(decodeOut)) {
                    System.err.println("Starting encoding");
                    in.transferTo(encodeOut);
                    encodeOut.flush();
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                } finally {
                    System.err.printf("Encoding complete %d%n",
                                      inCounter.get());
                }
            });
            exec.shutdown();

            /* In the foreground, transfer from the decoding end of the
             * pipe to result processing. */
            System.err.println("Starting decoding");
            decodeIn.transferTo(out);
            out.flush();
            System.err.println("Shutting down");
        } finally {
            System.err.println("Decoding complete");
            System.err.printf(" in=%d%nmid=%d%nout=%d%n", inCounter.get(),
                              midCounter.get(), outCounter.get());
            System.err.printf(" in=%s%n",
                              HexFormat.of().formatHex(digIn.digest()));
            System.err.printf("out=%s%n",
                              HexFormat.of().formatHex(digOut.digest()));
        }
    }
}
