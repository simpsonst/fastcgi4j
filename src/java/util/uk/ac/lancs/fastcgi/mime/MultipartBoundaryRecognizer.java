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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import uk.ac.lancs.fastcgi.io.BoundaryRecognizer;
import static uk.ac.lancs.fastcgi.io.BoundaryRecognizer.expand;
import static uk.ac.lancs.fastcgi.io.BoundaryRecognizer.skip;
import uk.ac.lancs.fastcgi.io.BoundarySequence;

/**
 * Recognizes MIME multipart boundaries.
 * 
 * <p>
 * A MIME multipart boundary is defined in terms of a boundary
 * parameter, usually of around 20-60 US-ASCII bytes. The boundary
 * itself consists of a carriage return (CR), linefeed (LF), two dashes
 * (0x2d), the boundary parameter, optionally two more dashes, then
 * linear whitespace terminated by another CRLF. (This implementation
 * recognizes CR and LF in the whitespace that do not form a CR-LF
 * sequence.) The optional trailing dashes indicate the terminal
 * boundary. Once this has been identified, no more boundaries are
 * recognized.
 * 
 * @author simpsons
 */
public final class MultipartBoundaryRecognizer implements BoundaryRecognizer {

    private final byte[] PREFIX =
        { ASCII.CR, ASCII.LF, ASCII.DASH, ASCII.DASH };

    private final byte[] boundary;

    /**
     * Create a boundary recognizer from part of a byte array.
     * 
     * @param boundary an array containing the bytes of the boundary,
     * excluding the leading CR LF and dash pair, or any trailing linear
     * whitespace and CR LF
     * 
     * @param off the offset into the array of the first byte of the
     * boundary
     * 
     * @param len the length of the boundary in bytes
     * 
     * @throws IllegalArgumentException if the boundary contains bytes
     * unsuitable for a boundary
     */
    public MultipartBoundaryRecognizer(byte[] boundary, final int off,
                                       final int len) {
        /* Check that the boundary bytes correspond to US-ASCII
         * bytes. */
        for (int i = off; i < off + len; i++)
            if (boundary[i] <= ASCII.SPACE || boundary[i] >= ASCII.DEL)
                throw new IllegalArgumentException("bad boundary byte[" + i
                    + "] " + (boundary[i] & 0xff));

        /* Create the full boundary by prefixing with CR LF and two
         * dashes. */
        this.boundary = new byte[len + PREFIX.length];
        System.arraycopy(PREFIX, 0, this.boundary, 0, PREFIX.length);
        System.arraycopy(boundary, off, this.boundary, PREFIX.length, len);
    }

    /**
     * Create a boundary recognizer from a byte array.
     * 
     * @param boundary an array containing the bytes of the boundary
     * 
     * @throws IllegalArgumentException if the boundary contains bytes
     * unsuitable for a boundary
     */
    public MultipartBoundaryRecognizer(byte[] boundary) {
        this(boundary, 0, boundary.length);
    }

    /**
     * Create a boundary recognizer from a character sequence.
     * 
     * @param boundary the characters of the boundary
     * 
     * @throws CharacterCodingException if the boundary contains
     * non-US-ASCII characters
     * 
     * @throws IllegalArgumentException if the boundary contains
     * characters unsuitable for a boundary
     */
    public MultipartBoundaryRecognizer(CharSequence boundary)
        throws CharacterCodingException {
        this(encodeAscii(boundary));
    }

    private static byte[] encodeAscii(CharSequence text)
        throws CharacterCodingException {
        CharsetEncoder enc = StandardCharsets.US_ASCII.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT);
        ByteBuffer buf = enc.encode(CharBuffer.wrap(text));
        byte[] res = new byte[buf.remaining()];
        buf.get(res);
        return res;
    }

    private static boolean isLinearWhiteSpace(byte b) {
        return switch (b) {
        case ASCII.TAB, ASCII.LF, ASCII.CR, ASCII.SPACE -> true;
        default -> false;
        };
    }

    @Override
    @SuppressWarnings("empty-statement")
    public int recognize(final byte[] buf, final int start, final int done,
                         final int limit, final boolean more, boolean init) {
        assert start <= done;
        assert done <= limit;
        assert limit <= buf.length;

        if (terminated) return skip(limit - start);

        final int candOff;
        if (init && buf[start] == ASCII.DASH) {
            candOff = 2;
        } else {
            candOff = 0;
        }

        /* Where should the fixed part of the boundary end? */
        final int fixedEnd = start + boundary.length - candOff;

        if (false) {
            System.err.printf("%n%d-%d-%d%s fixed end=%d%n", start, done, limit,
                              more ? "" : " (end)", fixedEnd);
            for (int i = start; i < limit; i += 16) {
                System.err.printf("%06X", i);
                for (int j = 0; j < 16; j++) {
                    final int p = i + j;
                    if (p < limit)
                        System.err.printf(
                                          "%c%02X", p == fixedEnd ? '!' :
                                              p < done ? '-' : ' ',
                                          buf[p] & 0xff);
                    else
                        System.err.print("   ");
                }
                System.err.print(' ');
                for (int j = 0; j < 16; j++) {
                    final int p = i + j;
                    if (p < limit) {
                        int b = buf[p] & 0xff;
                        System.err.printf("%c",
                                          b > ASCII.SPACE && b < ASCII.DEL ?
                                              (char) b : '.');
                    }
                }
                System.err.println();
            }
        }

        int pos = done;
        terminal = false;

        if (fixedEnd >= limit) {
            /* We haven't got enough bytes to determine whether we have
             * a boundary. However, if there are no more bytes, we know
             * we haven't got a boundary. */
            if (!more) return skip(limit - start);

            /* Check the bytes we do have. */
            while (pos < limit && buf[pos] == boundary[pos - start + candOff])
                pos++;

            /* If they all match, they are part of the candidate. */
            if (pos == limit) return expand(limit - done);

            /* There was a mismatch. Find the start of the next
             * candidate (a CR), and release earlier bytes for
             * delivery. */
            for (; pos < limit && buf[pos] != ASCII.CR; pos++)
                ;
            return skip(pos - start);
        }
        /* We have enough bytes to complete the fixed part of the
         * boundary. */

        /* See if we can match the remaining bytes of the fixed part of
         * the boundary. */
        while (pos < fixedEnd && buf[pos] == boundary[pos - start + candOff])
            pos++;
        if (pos < fixedEnd) {
            /* We haven't got a boundary, but search for one in what's
             * left. */
            for (; pos < limit && buf[pos] != ASCII.CR; pos++)
                ;
            return skip(pos - start);
        }
        /* We've matched the fixed part of the boundary. */

        /* Regardless of how far we got last time, (re)scan the variable
         * part of the boundary. Have we got a terminal boundary? */
        pos = fixedEnd;
        if (limit - pos == 1 && buf[pos] == ASCII.DASH) {
            /* There's only one byte left, and it's a dash. We could
             * still have a boundary. */
            return expand(limit - done);
        } else if (limit - pos >= 2 && buf[pos] == ASCII.DASH &&
            buf[pos + 1] == ASCII.DASH) {
            /* We have a double dash, so this would be part of the
             * terminating boundary. Remember that, and skip over the
             * dashes. */
            pos += 2;
            terminal = true;
        }

        /* Skip whitespace, but scan for CRLF. */
        byte last = -1;
        byte c;
        while (pos < limit && isLinearWhiteSpace(c = buf[pos])) {
            pos++;
            if (last == ASCII.CR && c == ASCII.LF) {
                /* We've found whitespace terminated by a CRLF. We have
                 * a boundary. */
                if (terminal) terminated = true;
                return expand(pos - done);
            }
            last = c;
        }
        /* We never found the terminating CRLF. */

        if (pos < limit) {
            /* We found something other than CRLF, so skip to the next
             * CR or the end of the buffer. The next candidate can't
             * start until then. None of the CRs we passed on the way
             * can be the start of a terminator, or they would have been
             * the end of the current one. */
            for (; pos < limit && buf[pos] != ASCII.CR; pos++)
                ;
            return skip(pos - start);
        }
        /* We got to the end of the buffer. */

        if (!more) {
            /* There will be no more data, so we don't have a boundary.
             * Release all remaining data. */
            return skip(limit - start);
        }

        /* We need more data to see if this is a boundary. */
        return expand(limit - done);
    }

    private boolean terminated = false;

    private boolean terminal;

    /**
     * Determine what kind of boundary the sequence is at.
     * 
     * @return {@code true} if the current position is a terminal
     * boundary; {@code false} if it is a normal boundary; indeterminate
     * if not at a boundary
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * @undocumented
     */
    public static void main(String[] args) throws Exception {
        MultipartBoundaryRecognizer recognizer =
            new MultipartBoundaryRecognizer(args[0]);
        byte[] buf = new byte[16];
        int part = 0;
        for (var shadow : BoundarySequence.of(System.in, recognizer, 10)) {
            try (var in = shadow) {
                long off = 0;
                int got;
                System.out.printf("Part %d:%n", part);
                while ((got = in.readNBytes(buf, 0, buf.length)) > 0) {
                    System.out.printf("%04x ", off);
                    for (int i = 0; i < buf.length; i++) {
                        if (i < got)
                            System.out.printf("%02X ", buf[i] & 0xff);
                        else
                            System.out.print("   ");
                    }
                    for (int i = 0; i < buf.length; i++) {
                        if (i < got) {
                            int c = buf[i] & 0xff;
                            if (c <= ASCII.SPACE || c >= ASCII.DEL)
                                System.out.print('.');
                            else
                                System.out.print((char) c);
                        }
                    }
                    System.out.println();
                    off += got;
                }
                System.out.printf("Total: %d%s%n%n", off,
                                  recognizer.isTerminal() ? " (terminal)" : "");
                part++;
            }
        }
    }
}
