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

package uk.ac.lancs.fastcgi.proto.serial;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
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
            return ByteBuffer.allocate((65535 + 8) & ~7);
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
    private final byte[] padding = new byte[8];

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
        buf.putInt(0); // unknown length fields

        /* Write as many requested values as will fit. */
        for (var entry : values.entrySet())
            if (!writeNameValue(buf, entry.getKey(), entry.getValue())) break;

        /* Compute and store the record length. */
        final int end = buf.position();
        final int len = end - 8;
        assert len <= 65535;
        buf.putShort(4, (short) len);

        /* Add padding. */
        buf.put(padding, 0, ((buf.position() + 7) & ~7) - buf.position());
        final int pad = buf.position() - end;
        assert pad <= 255;
        buf.put(6, (byte) pad);

        assert buf.position() % 8 == 0;
        try {
            synchronized (this) {
                out.write(buf.array(), 0, buf.position());
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

        buf.put((byte) type);
        buf.put(padding, 0, 7); // reserved

        assert buf.position() % 8 == 0;
        try {
            synchronized (this) {
                out.write(buf.array(), 0, buf.position());
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

        buf.putInt(appStatus);
        buf.put((byte) protoStatus);
        buf.put(padding, 0, 3);

        assert buf.position() % 8 == 0;
        try {
            synchronized (this) {
                out.write(buf.array(), 0, buf.position());
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
        if (len == 0) return 0;
        final int amount = Integer.min(len, 65535 & ~7);
        final int pad = ((amount + 7) & ~7) - amount;

        assert amount <= 65535;
        assert pad <= 255;
        assert (amount + pad) % 8 == 0;

        ByteBuffer bf = buffer.get();
        bf.clear();

        bf.put((byte) 1); // version
        bf.put(rt); // type
        bf.putShort((short) id); // request id
        bf.putShort((short) amount); // content length
        bf.put((byte) pad); // padding length
        bf.put((byte) 0); // reserved
        String pos = "hdr";
        try {
            synchronized (this) {
                out.write(bf.array(), 0, bf.position());

                pos = "data";
                out.write(buf, off, amount);

                if (pad > 0) {
                    pos = "pad";
                    out.write(padding, 0, pad);
                }
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

        try {
            synchronized (this) {
                out.write(bf.array(), 0, bf.position());
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
