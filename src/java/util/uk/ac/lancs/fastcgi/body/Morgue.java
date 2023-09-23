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

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Stores bodies for later retrieval. A morgue can store binary bodies
 * ({@code byte} sequences) or text bodies ({@code char} sequences).
 * Discarding the references to the bodies is sufficient to discard any
 * underlying resources allocated to represent them.
 *
 * @author simpsons
 */
public interface Morgue {
    /**
     * Store a byte stream. The stream is not closed after use. The call
     * only returns after storing the entire stream.
     * 
     * @param data the source stream
     * 
     * @return a means to recover the data
     * 
     * @throws IOException if an I/O error occurs in storing the stream
     */
    BinaryBody store(InputStream data) throws IOException;

    /**
     * Store part of a byte array.
     * 
     * @param buf the array containing the byte sequence to be stored
     * 
     * @param offset the offset into the array of the first byte to be
     * stored
     * 
     * @param length the number of bytes to be stored
     * 
     * @return a means to recover the data
     * 
     * @throws IOException if an I/O error occurs in storing the
     * sequence
     * 
     * @default This implementation wraps a {@link ByteArrayInputStream}
     * around the supplied array, and passes it to
     * {@link #store(InputStream)}, returning the result.
     */
    default BinaryBody store(byte[] buf, int offset, int length)
        throws IOException {
        try (var in = new ByteArrayInputStream(buf, offset, length)) {
            return this.store(in);
        }
    }

    /**
     * Store a byte array.
     * 
     * @param buf the array containing the byte sequence to be stored
     * 
     * @return a means to recover the data
     * 
     * @throws IOException if an I/O error occurs in storing the
     * sequence
     * 
     * @default This implementation calls
     * <code>{@linkplain #store(byte[], int, int)}(buf, 0, buf.length)</code>,
     * returning the result.
     */
    default BinaryBody store(byte[] buf) throws IOException {
        return this.store(buf, 0, buf.length);
    }

    /**
     * Store a character stream. The stream is not closed after use. The
     * call only returns after storing the entire stream.
     * 
     * @param data the source stream
     * 
     * @return a means to recover the data
     * 
     * @throws IOException if an I/O error occurs in storing the stream
     */
    TextBody store(Reader data) throws IOException;

    /**
     * Store part of a character array.
     * 
     * @param buf the array containing the character sequence to be
     * stored
     * 
     * @param offset the offset into the array of the first character to
     * be stored
     * 
     * @param length the number of character to be stored
     * 
     * @return a means to recover the data
     * 
     * @throws IOException if an I/O error occurs in storing the
     * sequence
     * 
     * @default This implementation wraps a {@link CharArrayInputStream}
     * around the supplied array, and passes it to
     * {@link #store(Reader)}, returning the result.
     */
    default TextBody store(char[] buf, int offset, int length)
        throws IOException {
        try (var in = new CharArrayReader(buf, offset, length)) {
            return this.store(in);
        }
    }

    /**
     * Store a character array.
     * 
     * @param buf the array containing the character sequence to be
     * stored
     * 
     * @return a means to recover the data
     * 
     * @throws IOException if an I/O error occurs in storing the
     * sequence
     * 
     * @default This implementation calls
     * <code>{@linkplain #store(char[], int, int)}(buf, 0, buf.length)</code>,
     * returning the result.
     */
    default TextBody store(char[] buf) throws IOException {
        return this.store(buf, 0, buf.length);
    }

    /**
     * Store a character sequence.
     * 
     * @param data the source sequence
     * 
     * @return a means to recover the data
     * 
     * @throws IOException if an I/O error occurs in storing the
     * sequence
     */
    default TextBody store(CharSequence data) throws IOException {
        try (var in = new CharSequenceReader(data)) {
            return this.store(in);
        }
    }
}
