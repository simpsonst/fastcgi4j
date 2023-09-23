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
import java.io.Reader;
import java.io.Writer;

/**
 * Presents a stream view of a character sequence. This should only be
 * used to provide a short-term view of a {@link CharSequence}, during
 * which the contents are not expected to change. Otherwise, it is
 * better to convert the {@link CharSequence} to a {@link String} to
 * freeze the contents, and use a {@link java.io.StringReader} on it.
 * 
 * @author simpsons
 */
class CharSequenceReader extends Reader {
    private final CharSequence base;

    private int pos = 0, mark = -1;

    /**
     * Create a stream view of a character sequence.
     * 
     * @param base the character sequence
     */
    public CharSequenceReader(CharSequence base) {
        this.base = base;
    }

    /**
     * Read several characters into an array.
     * 
     * @param cbuf the destination array
     * 
     * @param off the index into the array to store the first character
     * read
     * 
     * @param len the maximum number of characters to read
     * 
     * @return the number of characters read, or {@code -1} if the end
     * of the underlying character sequence has been reached
     */
    @Override
    public int read(char[] cbuf, int off, int len) {
        final int rem = base.length() - pos;
        if (rem == 0) return -1;
        final int amount = Math.min(rem, len);
        for (int i = 0; i < amount; i++)
            cbuf[off + i] = base.charAt(pos + i);
        pos += amount;
        return amount;
    }

    /**
     * Close the stream. In fact, this call has no effect.
     */
    @Override
    public void close() {}

    /**
     * Transfer the remainder of this stream to another.
     * 
     * @default This implementation creates a small buffer, then
     * alternately calls {@link #read(char[])} on this object to fill
     * the buffer, then {@link Writer#write(char[], int, int)} on the
     * destination.
     * 
     * @param out the destination stream
     * 
     * @return the number of characters transferred
     * 
     * @throws IOException if an I/O error occurs in writing to the
     * destination
     */
    @Override
    public long transferTo(Writer out) throws IOException {
        final int rem = base.length() - pos;
        final int space = Math.min(rem, 1024);
        char[] buf = new char[space];
        long amount = 0;
        int rc;
        while ((rc = this.read(buf)) >= 0) {
            out.write(buf, 0, rc);
            amount += rc;
        }
        return amount;
    }

    /**
     * Reset the stream pointer back to the marked position.
     * 
     * @throws IOException if the stream's mark has not been set (with
     * {@link #mark(int)})
     */
    @Override
    public void reset() throws IOException {
        if (mark < 0) throw new IOException("unmarked");
        pos = mark;
    }

    /**
     * Set the stream's mark to the current position.
     * 
     * @param readAheadLimit ignored
     */
    @Override
    public void mark(int readAheadLimit) {
        mark = pos;
    }

    /**
     * Determine whether this stream supports marking.
     * 
     * @default It does.
     * 
     * @return {@code true}
     */
    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * Skip over a given number of characters.
     * 
     * @param n the maximum number of characters to skip
     * 
     * @return the number of characters skipped
     */
    @Override
    public long skip(long n) {
        final int rem = base.length() - pos;
        final int amount = (int) Math.min(rem, n);
        pos += amount;
        return amount;
    }

    /**
     * Read a single character from the base sequence.
     * 
     * @return the next character from the sequence; or {@code -1} if
     * the end of the underlying sequence has been reached
     */
    @Override
    public int read() {
        if (pos == base.length()) return -1;
        return base.charAt(pos++) & 0xff;
    }
}
