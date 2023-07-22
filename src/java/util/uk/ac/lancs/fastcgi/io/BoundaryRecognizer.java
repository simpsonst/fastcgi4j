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

package uk.ac.lancs.fastcgi.io;

/**
 * Recognizes boundaries and partial boundaries in byte arrays. Portions
 * of a byte sequence are to be presented in order. On each call to
 * {@link #recognize(byte[], int, int, int, boolean)}, two adjacent
 * areas of a byte array are identified. The first begins at the
 * <dfn>start</dfn> position, and is being considered as the start of a
 * boundary. It ends at the <dfn>done</dfn> position, which reminds the
 * recognizer of bytes it has already identified as being the potential
 * start of a boundary (everything from the inclusive start to the
 * exclusive done position). The second area begins from here, and
 * terminates at the <dfn>limit</dfn>. Bytes outside these areas should
 * be considered undefined, and no bytes may be modified.
 * 
 * <p>
 * A recognizer implementation should be stateful, and provide
 * additional methods to determine whether the current sequence position
 * is the start of a boundary, and which kind if multiple are possible.
 * 
 * @author simpsons
 */
public interface BoundaryRecognizer {
    /**
     * Recognize a boundary in a byte array. The implementation may
     * respond in one of three ways:
     * 
     * <ul>
     * 
     * <li>
     * <p>
     * The start position is the start of a boundary. The length of the
     * boundary or prefix is returned.
     * 
     * <li>
     * <p>
     * The start position is (or remains) suspected as being the start
     * of a boundary, but there are insufficient bytes to determine
     * this. The number of bytes to extend the suspected area by is
     * returned. Note that the sequence may end at the limit, in which
     * case, this and the first case are indistinguishable by return
     * value. However, the recognizer can distinguish them by the
     * Boolean passed.
     * 
     * <li>
     * <p>
     * The start position is definitely not the start of a boundary. The
     * negative of the number of bytes to skip to find the next boundary
     * candidate is returned. {@link #skip(int)} can be used to generate
     * this return code.
     * 
     * </ul>
     * 
     * 
     * @param buf the array in which to seek a boundary
     * 
     * @param start the offset into the array where the boundary is
     * suspected to start
     * 
     * @param done the offset of the first byte which has not been
     * checked for a boundary
     * 
     * @param limit the offset past the last defined byte of the array
     * 
     * @param more {@code true} if there are potentially more bytes to
     * come; {@code false} if the byte before the limit is definitely
     * the last byte in the sequence
     * 
     * @return a negative number if the region of the array does not
     * start with a boundary, indicating how many bytes to skip before
     * suspecting again; or the (non-negative) number of bytes that
     * constitute a boundary or boundary prefix otherwise
     */
    int recognize(byte[] buf, int start, int done, int limit, boolean more);

    /**
     * Generate the code that signals expansion of the region containing
     * a (candidate) boundary.
     * 
     * @param amount the amount to expand by
     * 
     * @return the input amount, having asserted that it is non-negative
     */
    static int expand(int amount) {
        assert amount >= 0 : "amount=" + amount;
        return amount;
    }

    /**
     * Generate the code that signals that no boundary is in at the head
     * of the current region.
     * 
     * @param amount the amount to skip
     * 
     * @return the negated input amount, having asserted that it is
     * positive
     */
    static int skip(int amount) {
        assert amount > 0 : "amount=" + amount;
        return -amount;
    }
}
