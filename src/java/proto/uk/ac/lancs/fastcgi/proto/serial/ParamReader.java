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
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Reads byte-encoded parameters from a sequence of input streams.
 *
 * @author simpsons
 */
public class ParamReader {
    private final Map<? super String, ? super String> params;

    private final Consumer<byte[]> pool;

    private final Charset charset;

    private final String tag;

    private byte[] buf;

    private int len = 0;

    /**
     * Prepare to read byte-encoded parameters.
     * 
     * @param dest the map to be populated with decoded parameters
     * 
     * @param charset the character encoding for parameter names and
     * values
     * 
     * @param pool a place to discard the internal buffer
     * 
     * @param buf an initial buffer to use
     * 
     * @param tag a tag to include in logging messages
     */
    public ParamReader(Map<? super String, ? super String> dest,
                       Charset charset, byte[] buf, Consumer<byte[]> pool,
                       String tag) {
        this.params = dest;
        this.charset = charset;
        this.buf = buf;
        this.pool = pool;
        this.tag = tag;
    }

    /**
     * Read all bytes from a stream, and decode them as parameters.
     * 
     * @param in the stream containing encoded parameter data
     * 
     * @throws IOException if an I/O error occurred in reading the
     * stream
     */
    public void consume(InputStream in) throws IOException {
        while (recordParam(in))
            while (decodeParam())
                ;
        logger.fine(() -> msg("params: %s", params));
    }

    /**
     * Indicate that no more encoded parameter data is forthcoming.
     * 
     * @throws IllegalStateException if this method has already been
     * called, or if there are trailing bytes in the internal buffer
     */
    public void complete() {
        /* Detect a duplicate call, and save away the parameter buffer
         * for later use. */
        if (buf == null)
            throw new IllegalStateException("parameters ended twice");
        pool.accept(buf);
        buf = null;

        /* Check that we have no excess parameter data. */
        if (len > 0)
            throw new IllegalStateException("trailing parameter bytes: " + len);
    }

    /**
     * Attempt to fill the parameter buffer.
     * 
     * @param in the byte source
     * 
     * @return {@code false} if the stream has reported EOF;
     * {@code true} otherwise
     * 
     * @throws IOException if an I/O error occurs reading from the
     * stream
     */
    private boolean recordParam(InputStream in) throws IOException {
        assert len <= buf.length;

        /* If our array is too small, allocate a little more space. */
        if (len == buf.length) {
            len += 128;
            len *= 2;
            buf = Arrays.copyOf(buf, len);
        }

        /* Read as many bytes into the remaining space of the array as
         * possible. */
        final int rem = buf.length - len;
        assert rem > 0;
        int got = in.read(buf, len, rem);
        if (got >= 0) {
            len += got;
            return true;
        }

        /* Indicate that no more bytes are available. */
        return false;
    }

    /**
     * Attempt to decode one parameter at the start of the buffer. If a
     * parameter is decoded, its bytes are removed from the buffer, and
     * the trailing bytes are moved to the head of the buffer.
     * 
     * @return {@code true} if the method should be called again;
     * {@code false} otherwise, e.g., if there are insufficient bytes to
     * determine whether a complete parameter has loaded
     */
    private boolean decodeParam() {
        /* Work out the lengths of the name and value in bytes. Each
         * length is either 1 byte (in the range 0 to 127) or 4 bytes
         * (big-endian, with the top bit set in the first, which must be
         * cleared.). */
        if (len < 2) return false;
        final int nameLen, valueLen, nameStart;
        if (buf[0] < 0) {
            /* The name length is 4 bytes. Are there sufficient to
             * encode the value length? */
            if (len < 5) return false;

            if (buf[4] < 0) {
                /* The value length is also 4 bytes. Are there
                 * sufficient to encode it? */
                if (len < 8) return false;
                /* Decode the value length. The name starts at 4+4. */
                valueLen = getInt(buf, 4);
                nameStart = 8;
            } else {
                /* The value length is a single byte. The name starts at
                 * 4+1. */
                valueLen = buf[4] & 0xff;
                nameStart = 5;
            }
            nameLen = getInt(buf, 0);
        } else {
            /* The name length is 1 byte. We already know we have at
             * least 2. */
            if (buf[1] < 0) {
                /* The value length is 4 bytes. Are there sufficient to
                 * encode it? */
                if (len < 5) return false;
                /* Decode the value length. The name starts at 1+4. */
                valueLen = getInt(buf, 1);
                nameStart = 5;
            } else {
                /* The value length is 1 byte, so the name starts at
                 * position 2. */
                valueLen = buf[1] & 0xff;
                nameStart = 2;
            }
            nameLen = buf[0] & 0xff;
        }

        /* Work out how many bytes we need in total. If we don't have
         * enough, ask for some more. */
        final int end = nameStart + nameLen + valueLen;
        if (len < end) return false;

        /* We have all the bytes for a complete parameter. Decode and
         * store it. */
        final String name = new String(buf, nameStart, nameLen, charset);
        final int valueStart = nameStart + nameLen;
        final String value = new String(buf, valueStart, valueLen, charset);
        params.put(name, value);
        logger.finer(() -> msg("PARAM[%s]=%s", name, value));

        /* Move the trailing bytes to the head of the array. */
        final int rem = len - end;
        System.arraycopy(buf, end, buf, 0, rem);
        len = rem;
        return len >= 2;
    }

    /**
     * Read a 31-bit big-endian integer from a byte array.
     * 
     * @param buf the array containing the bytes
     * 
     * @param off the offset of the MSB, whose top bit will be cleared
     * 
     * @return the decoded value
     * 
     * @throws ArrayIndexOutOfBoundsException if there are insufficient
     * bytes in the array
     */
    private static int getInt(byte[] buf, int off) {
        int r = 0;
        for (int i = 0; i < 4; i++) {
            r <<= 8;
            r |= buf[off + i] & 0xff;
        }
        return r & 0x7fffffff;
    }

    private String msg(String fmt, Object... args) {
        return tag + ":" + String.format(fmt, args);
    }

    private static final Logger logger =
        Logger.getLogger(ParamReader.class.getPackageName());
}
