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

package uk.ac.lancs.fastcgi.proto.serial;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import uk.ac.lancs.fastcgi.proto.RecordTypes;

/**
 * Writes its contents as a series of FastCGI records of a given type.
 * FastCGI/1.0 streams are delivered using {@link RecordTypes#STDIN} and
 * {@link RecordTypes#PARAMS} from server to application, and using
 * {@link RecordTypes#STDOUT} and {@link RecordTypes#STDERR} from
 * application to server. Extensions to FastCGI may introduce other
 * stream record types, or permit existing types to be used in the
 * opposite direction.
 * 
 * <p>
 * A {@link java.io.BufferedOutputStream} should be wrapped around this
 * stream to minimize the number of records sent.
 * 
 * @author simpsons
 */
public class RecordOutputStream extends OutputStream {
    /**
     * Distinguishes this stream from others in logging messages.
     */
    protected final String label;

    /**
     * Specifies the record type. Only a stream type should be used.
     */
    protected final byte type;

    /**
     * Identifies the request.
     */
    protected final int id;

    /**
     * Converts byte arrays into record transmissions.
     */
    protected final RecordWriter base;

    private boolean closed = false;

    private final byte[] buf1 = new byte[1];

    /**
     * Create a stream that can be written as records over a FastCGI
     * connection.
     * 
     * @param label the diagnostic label distinguishing this stream from
     * others in the same request
     * 
     * @param id the request id
     * 
     * @param type the stream record type
     * 
     * @param base the transmitter of byte arrays as FastCGI records
     */
    public RecordOutputStream(String label, byte type, int id,
                              RecordWriter base) {
        this.label = label;
        this.id = id;
        this.type = type;
        this.base = base;
    }

    /**
     * Close the stream. If not already closed, a record of zero length
     * is transmitted, with the configured record type and request id.
     * 
     * @throws IOException if an I/O error occurs in trying to send the
     * record
     */
    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        base.writeEnd(label, type, id);
    }

    private static void checkBufferRange(byte[] b, String bName, int off,
                                         int len) {
        Objects.requireNonNull(b, bName);
        if (off < 0 || off > b.length) throw new IndexOutOfBoundsException(off);
        if (off + len > b.length)
            throw new IndexOutOfBoundsException(off + len);
    }

    /**
     * Write a portion of an array to the stream. Multiple record types
     * of non-zero length may be sent, using the configured record type
     * and request id.
     * 
     * @param b the array containing the bytes to be transmitted
     * 
     * @param off the index into the array of the first byte to be
     * transmitted
     * 
     * @param len the number of bytes to be transmitted
     * 
     * @throws IOException if an I/O error occurs in trying to send a
     * record
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) throw new IOException("closed");
        checkBufferRange(b, "b", off, len);

        /* Repeatedly write some data, and consume whatever was sent.
         * The data might be longer than what can be sent in a single
         * FastCGI frame. */
        while (len > 0) {
            int done = base.writeStream(label, type, id, b, off, len);
            assert done >= 0;
            assert done <= len;
            off += done;
            len -= done;
        }
    }

    /**
     * Write a single byte to the stream. One single-byte record is
     * sent, using the configured record type and request id.
     * 
     * @param b the byte to be sent
     * 
     * @throws IOException if an I/O error occurs in trying to send the
     * record
     */
    @Override
    public void write(int b) throws IOException {
        if (closed) throw new IOException("closed");
        buf1[0] = (byte) b;
        int done = base.writeStream(label, type, id, buf1, 0, 1);
        if (done != 1) throw new IOException("failed single byte");
    }
}
