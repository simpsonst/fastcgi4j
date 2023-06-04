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

package uk.ac.lancs.fastcgi.proto.serial;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import uk.ac.lancs.fastcgi.proto.ProtocolStatuses;
import uk.ac.lancs.fastcgi.proto.RecordTypes;

/**
 * Writes complete FastCGI records from application to server.
 * 
 * @author simpsons
 */
public class RecordWriter {
    private final OutputStream out;

    private final Charset charset;

    private final Lock lock = new ReentrantLock();

    /**
     * Specifies how big the payload of a record can be, namely
     * {@value} bytes.
     */
    private static final int MAX_CONTENT_LENGTH = 0xffff;

    /**
     * Specifies the intended alignment of records.
     */
    private static final int ALIGNMENT = 8;

    /**
     * Specifies the expected size of a record header.
     */
    private static final int HEADER_LENGTH = 8;

    /**
     * Increase an amount to make it a multiple of the alignment.
     * 
     * @param amount the amount to be modified
     * 
     * @return the aligned amount, which is no less than the input
     */
    private static int align(final int amount) {
        final int plusXm1 = amount + (ALIGNMENT - 1);
        final int aligned = plusXm1 - plusXm1 % ALIGNMENT;
        assert aligned % ALIGNMENT == 0;
        assert aligned - amount < ALIGNMENT;
        return aligned;
    }

    private static final int OPTIMUM_PAYLOAD =
        alignBack(HEADER_LENGTH + MAX_CONTENT_LENGTH) - HEADER_LENGTH;

    /**
     * Get the optimum payload length for this writer. This is computed
     * by taking the maximum payload length, increasing it by the header
     * length, reducing by the minimum amount to achieve optimum
     * alignment, and then subtracting the header length. For example, a
     * header length of 8, a maximum payload of 65535, and an alignment
     * of 8 yields 65528. The user should consider offering multiples of
     * this amount when doing large transfers with
     * {@link #writeStdout(int, byte[], int, int)} or
     * {@link #writeStderr(int, byte[], int, int)}, to avoid padding.
     * 
     * @return the optimum payload length
     */
    public int optimumPayloadLength() {
        return OPTIMUM_PAYLOAD;
    }

    /**
     * Get the optimum alignment for this writer. The <a href=
     * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S3.3">specification
     * recommends 8</a>, so that's what you'll likely get here.
     * 
     * @return the optimum alignment
     */
    public int alignment() {
        return ALIGNMENT;
    }

    private static void checkHeaderLength(int amount) {
        assert amount == HEADER_LENGTH :
            "header not " + HEADER_LENGTH + ": " + amount;
    }

    /**
     * Decrease an amount to make it a multiple of the alignment.
     * 
     * @param amount the amount to be modified
     * 
     * @return the aligned amount, which is no more than the input
     */
    private static int alignBack(final int amount) {
        return amount - amount % ALIGNMENT;
    }

    private static void checkAlignment(int amount) {
        assert amount % ALIGNMENT == 0 :
            "not aligned to " + ALIGNMENT + ": " + amount;
    }

    private static void checkAlignment(ByteBuffer buf) {
        checkAlignment(buf.position());
    }

    /**
     * Prepare to write records.
     * 
     * @param out the destination for serialized records
     * 
     * @param charset the character encoding for writing name-value
     * pairs
     */
    public RecordWriter(OutputStream out, Charset charset) {
        this.out = out;
        this.charset = charset;
    }

    /**
     * Holds a buffer per calling thread.
     */
    private final ThreadLocal<ByteBuffer> buffer = new ThreadLocal<>() {
        @Override
        protected ByteBuffer initialValue() {
            /* The maximum number of bytes we can write in a single
             * record is 8 bytes for the header, and 65535 for the
             * payload, plus enough padding to a multiple of 8. */
            return ByteBuffer.allocate(align(8 + MAX_CONTENT_LENGTH));
        }
    };

    /**
     * Write a string length into a buffer. If the length is less than
     * 128, a single byte is written. Otherwise, bit 31 of the amount is
     * set, and four bytes are written; the first will be an unsigned
     * value of at least 128 because it contains that top bit.
     * 
     * @param buf the buffer to write in to
     * 
     * @param amount the length to write, which must not be negative
     */
    private static void writeStringLength(ByteBuffer buf, int amount) {
        assert amount >= 0;
        if (amount <= 127) {
            buf.put((byte) amount);
        } else {
            buf.putInt(amount | 0x80000000);
        }
    }

    /**
     * Write a name-value pair into a buffer. The name and value are
     * converted to byte arrays, and then their lengths are encoded into
     * the buffer as 1- or 4-byte values, followed by the name and then
     * the value. If the remaining space in the buffer is too small, the
     * buffer is unmodified, and {@code false} is returned.
     * 
     * @param buf the buffer to extend
     * 
     * @param name the name to encode
     * 
     * @param value the value to encode
     * 
     * @return {@code true} if no overflow occurred; {@code false}
     * otherwise
     */
    private boolean writeNameValue(ByteBuffer buf, String name, String value) {
        byte[] nameBytes = name.getBytes(charset);
        byte[] valueBytes = value.getBytes(charset);
        int len = 0;
        len += nameBytes.length > 127 ? 4 : 1;
        len += valueBytes.length > 127 ? 4 : 1;
        len += nameBytes.length;
        len += valueBytes.length;
        if (buf.remaining() < len) return false;
        writeStringLength(buf, nameBytes.length);
        writeStringLength(buf, valueBytes.length);
        buf.put(nameBytes);
        buf.put(valueBytes);
        return true;
    }

    /**
     * Holds bytes used for padding. This array should not be written
     * to.
     */
    private final byte[] padding = new byte[ALIGNMENT - 1];

    /**
     * Write application variables. An <code>FCGI_GET_VALUES</code>
     * record is transmitted. Only one record is sent.
     * 
     * @todo Is this the correct behaviour when the record overflows?
     * The <a href=
     * "http://www.mit.edu/~yandros/doc/specs/fcgi-spec.html#S4.1"
     * >specification</ a> says that unrecognized variables are simply
     * not sent from the application, so if the results are incomplete,
     * won't the server assume they are unrecognized? It could ask again
     * for anything missing, and only stop when an empty result is
     * returned. (Then, the only problem would be if a single variable's
     * name and value could not fit into a single record) The
     * alternative would be to send multiple records for only one
     * request.
     * 
     * @param values the name-value pairs to be written
     * 
     * @throws RecordIOException if an I/O error occurred
     * 
     * @see RecordTypes#GET_VALUES_RESULT
     */
    public void writeValues(Map<? extends String, ? extends String> values)
        throws RecordIOException {
        ByteBuffer buf = buffer.get();
        buf.clear();

        /* Write the header, leaving length fields empty. */
        buf.put((byte) 1);
        buf.put(RecordTypes.GET_VALUES_RESULT);
        buf.putShort((short) 0); // request id
        final int lenPos = buf.position();
        buf.putShort((short) 0); // unknown content length
        final int padPos = buf.position();
        buf.put((byte) 0); // unknown padding length
        buf.put((byte) 0); // reserved
        final int begin = buf.position();
        checkHeaderLength(begin);

        /* Write as many requested values as will fit. */
        buf.limit(buf.position() + MAX_CONTENT_LENGTH);
        for (var entry : values.entrySet())
            if (!writeNameValue(buf, entry.getKey(), entry.getValue())) break;

        /* Compute and store the record length. */
        final int end = buf.position();
        final int len = end - begin;
        assert len <= MAX_CONTENT_LENGTH;
        buf.putShort(lenPos, (short) len);

        /* Add padding. First, work out how much, then extend the
         * buffer, then write in the bytes, write in the padding data,
         * and record the padding length. */
        final int padEnd = align(buf.position());
        final int pad = padEnd - buf.position();
        buf.limit(padEnd);
        buf.put(padding, 0, pad);
        assert buf.position() == buf.limit();
        assert pad <= 255;
        buf.put(padPos, (byte) pad);

        /* Ensure our new computation is the same as the old. */
        final int pad2 = buf.position() - end;
        assert pad2 == pad;

        checkAlignment(buf);
        try {
            try {
                lock.lock();
                out.write(buf.array(), 0, buf.position());
            } finally {
                lock.unlock();
            }
        } catch (IOException ex) {
            throw new RecordIOException("writeValues", ex);
        }
    }

    /**
     * Report that an unknown record type was received. An
     * <code>FCGI_UNKNOWN_TYPE</code> record is transmitted.
     * 
     * @param type the unknown record type
     * 
     * @throws RecordIOException if an I/O error occurred
     * 
     * @see RecordTypes#UNKNOWN_TYPE
     */
    public void writeUnknownType(int type) throws RecordIOException {
        ByteBuffer buf = buffer.get();
        buf.clear();

        buf.put((byte) 1); // version
        buf.put(RecordTypes.UNKNOWN_TYPE); // type
        buf.putShort((short) 0); // request id
        buf.putShort((short) 8); // content length
        buf.put((byte) 0); // padding length
        buf.put((byte) 0); // reserved
        checkHeaderLength(buf.position());

        buf.put((byte) type);
        buf.put(padding, 0, 7); // reserved

        checkAlignment(buf);
        try {
            try {
                lock.lock();
                out.write(buf.array(), 0, buf.position());
            } finally {
                lock.unlock();
            }
        } catch (IOException ex) {
            throw new RecordIOException("writeUnknownType", ex);
        }
    }

    /**
     * Report that the application is completing or aborting a request.
     * An <code>FCGI_END_REQUEST</code> record is transmitted.
     * 
     * @param id the request id being closed
     * 
     * @param appStatus the exit status for the request; 0 for no error
     * 
     * @param protoStatus the reason for closing the request; see
     * {@link ProtocolStatuses}
     * 
     * @throws RecordIOException if an I/O error occurred
     * 
     * @see RecordTypes#END_REQUEST
     */
    public void writeEndRequest(int id, int appStatus, int protoStatus)
        throws RecordIOException {
        ByteBuffer buf = buffer.get();
        buf.clear();

        buf.put((byte) 1); // version
        buf.put(RecordTypes.END_REQUEST); // type
        buf.putShort((short) id); // request id
        buf.putShort((short) 8); // content length
        buf.put((byte) 0); // padding length
        buf.put((byte) 0); // reserved
        checkHeaderLength(buf.position());

        buf.putInt(appStatus);
        buf.put((byte) protoStatus);
        buf.put(padding, 0, 3);

        checkAlignment(buf);
        try {
            try {
                lock.lock();
                out.write(buf.array(), 0, buf.position());
            } finally {
                lock.unlock();
            }
        } catch (IOException ex) {
            throw new RecordIOException("writeEndRequest", ex);
        }
    }

    /**
     * Write bytes to one of the streams.
     * 
     * @param label a diagnostic label used in the formation of
     * log/exception messages
     * 
     * @param rt the FastCGI record type; either
     * {@link RecordTypes#STDOUT} or {@link RecordTypes#STDERR}, or any
     * other future application-to-server stream type
     * 
     * @param id the request id
     * 
     * @param buf an array containing the bytes to write
     * 
     * @param off the index into the array of the first byte to write
     * 
     * @param len the maximum number of bytes to write
     * 
     * @return the number of bytes from the array that were written
     * 
     * @throws RecordIOException if an I/O error occurred
     */
    private int writeStream(String label, byte rt, int id, byte[] buf, int off,
                            int len)
        throws RecordIOException {
        /* We must not send a zero-length message, as this is
         * interpreted as EOF. */
        if (len == 0) return 0;
        assert len > 0;

        ByteBuffer bf = buffer.get();
        bf.clear();

        /* Write the header, not knowing the amount of content or
         * padding to write. */
        bf.put((byte) 1); // version
        bf.put(rt); // type
        bf.putShort((short) id); // request id
        final int lenPos = bf.position();
        bf.putShort((short) 0); // unknown content length
        final int padPos = bf.position();
        bf.put((byte) 0); // unknown padding length
        bf.put((byte) 0); // reserved
        final int begin = bf.position();
        checkHeaderLength(begin);

        /* Determine how much content to actually send this time. */
        final int amount;
        if (len < MAX_CONTENT_LENGTH) {
            /* Since the amount is less than our strict limit, just send
             * the lot, and include whatever padding is required. It's
             * not worth sending another record to save a few bytes of
             * padding on this one. */
            amount = len;
        } else {
            /* Send fewer than our maximum to avoid padding. We might
             * not save anything in the end, but if the last segment
             * needs no padding, we can ensure that by adding no padding
             * here. */
            amount = alignBack(begin + MAX_CONTENT_LENGTH) - begin;
        }

        /* Store the determined content length. */
        assert amount <= MAX_CONTENT_LENGTH;
        bf.putShort(lenPos, (short) amount); // content length

        /* Work out the content end/padding start, and so the amount of
         * padding. Write it into the header. */
        final int end = begin + amount;
        final int padEnd = align(end);
        final int pad = padEnd - end;
        bf.put(padPos, (byte) pad);

        checkAlignment(begin + amount + pad);

        /* Holding the lock, write out the header, content and padding
         * as three operations. */
        String pos = "hdr";
        try {
            try {
                lock.lock();
                out.write(bf.array(), 0, begin);

                pos = "data";
                out.write(buf, off, amount);

                if (pad > 0) {
                    pos = "pad";
                    out.write(padding, 0, pad);
                }
            } finally {
                lock.unlock();
            }
        } catch (IOException ex) {
            throw new RecordIOException("write" + label + ":" + pos, ex);
        }
        return amount;
    }

    /**
     * Signal the end of a stream.
     * 
     * @param label a diagnostic label used in the formation of
     * log/exception messages
     * 
     * @param rt the FastCGI record type; either
     * {@link RecordTypes#STDOUT} or {@link RecordTypes#STDERR}, or any
     * other future application-to-server stream type
     * 
     * @param id the request id
     * 
     * @throws RecordIOException if an I/O error occurred
     */
    private void writeEnd(String label, byte rt, int id)
        throws RecordIOException {
        ByteBuffer bf = buffer.get();
        bf.clear();

        bf.put((byte) 1); // version
        bf.put(rt); // type
        bf.putShort((short) id); // request id
        bf.putShort((short) 0); // content length
        bf.put((byte) 0); // padding length
        bf.put((byte) 0); // reserved
        checkAlignment(bf);
        checkHeaderLength(bf.position());

        try {
            try {
                lock.lock();
                out.write(bf.array(), 0, bf.position());
            } finally {
                lock.unlock();
            }
        } catch (IOException ex) {
            throw new RecordIOException("write" + label + ":hdr0", ex);
        }
    }

    /**
     * Write bytes to the standard output of a request. An
     * <code>FCGI_STDOUT</code> record is transmitted. If the provided
     * length is 0, no record is sent. Call {@link #writeStdoutEnd(int)}
     * to indicate the end of the stream.
     * 
     * @param id the request id
     * 
     * @param buf the array containing the bytes to be written
     * 
     * @param off the index into the array of the first byte to be
     * written
     * 
     * @param len the number of bytes to be written
     * 
     * @return the number of bytes written
     * 
     * @throws RecordIOException if an I/O error occurred
     * 
     * @see RecordTypes#STDOUT
     */
    public int writeStdout(int id, byte[] buf, int off, int len)
        throws RecordIOException {
        return writeStream("Stdout", RecordTypes.STDOUT, id, buf, off, len);
    }

    /**
     * Indicate the end of standard output of a request. An empty
     * <code>FCGI_STDOUT</code> record is transmitted.
     * 
     * @param id the request id
     * 
     * @throws RecordIOException if an I/O error occurred
     * 
     * @see RecordTypes#STDOUT
     */
    public void writeStdoutEnd(int id) throws RecordIOException {
        writeEnd("Stdout", RecordTypes.STDOUT, id);
    }

    /**
     * Write bytes to the standard error output of a request. An
     * <code>FCGI_STDIN</code> record is transmitted. If the provided
     * length is 0, no record is sent. Call {@link #writeStderrEnd(int)}
     * to indicate the end of the stream.
     * 
     * @param id the request id
     * 
     * @param buf the array containing the bytes to be written
     * 
     * @param off the index into the array of the first byte to be
     * written
     * 
     * @param len the number of bytes to be written
     * 
     * @return the number of bytes written
     * 
     * @throws RecordIOException if an I/O error occurred
     * 
     * @see RecordTypes#STDERR
     */
    public int writeStderr(int id, byte[] buf, int off, int len)
        throws RecordIOException {
        return writeStream("Stderr", RecordTypes.STDERR, id, buf, off, len);
    }

    /**
     * Indicate the end of standard error output of a request. An empty
     * <code>FCGI_STDERR</code> record is transmitted.
     * 
     * @param id the request id
     * 
     * @throws RecordIOException if an I/O error occurred
     * 
     * @see RecordTypes#STDERR
     */
    public void writeStderrEnd(int id) throws RecordIOException {
        writeEnd("Stderr", RecordTypes.STDERR, id);
    }
}
