// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2022,2023,2026, Lancaster University
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

package uk.ac.lancs.http.field;

/**
 * Expresses an internal numeric identifier for a field extension. An
 * internal id is only valid within the scope of a single HTTP message,
 * but applies to both header and trailer. It is used as a shorthand for
 * an extension namespace for field names, and may also apply to
 * components of field values (especially when such values are defined
 * to hold field names).
 *
 * @author simpsons
 */
public final class InternalId {
    /**
     * The numeric value of the identifier
     */
    public final int value;

    /**
     * The width of the identifier in decimal digits
     */
    public final int width;

    private InternalId(int width, int value) {
        this.value = value;
        this.width = width;
    }

    /**
     * Get the extension identifier from a character sequence.
     * 
     * @param cs the source character sequence
     * 
     * @return the extension identifier
     * 
     * @throws IllegalArgumentException if the sequence does not begin
     * with at least two decimal digits, or the digits are followed by
     * anything other than a dash
     */
    public static InternalId of(CharSequence cs) {
        final int ln = cs.length();
        int val = 0, wid = 0;
        for (int i = 0; i < ln; i++) {
            var c = cs.charAt(i);
            switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                val *= 10;
                val += c - '0';
                wid++;
                break;

            case '-':
                if (wid < 2)
                    throw new IllegalArgumentException("narrow identifier: "
                        + cs.subSequence(0, i + 1));
                return new InternalId(wid, val);

            default:
                throw new IllegalArgumentException("bad identifier: "
                    + cs.subSequence(0, i + 1));
            }
        }
        return new InternalId(wid, val);
    }

    /**
     * Create an extension identifier from width and value.
     * 
     * @param width the width of the identifier in decimal digits
     * 
     * @param value the numeric value of the identifier
     * 
     * @return the extension identifier
     * 
     * @throws IllegalArgumentException if the width is less than 2; the
     * value is negative; or the value is too wide for the given width
     * 
     * @constructor
     */
    public static InternalId of(int width, int value) {
        if (width < 2)
            throw new IllegalArgumentException("bad extension identifier width: "
                + width);
        if (value < 0)
            throw new IllegalArgumentException("negative extension identifier: "
                + value);
        int max = 1;
        for (int i = 0; i < width; i++)
            max *= 10;
        if (value >= max)
            throw new IllegalArgumentException("extension identifier too wide: "
                + value + " in " + width);
        return new InternalId(width, value);
    }

    /**
     * Get a string representation of the identifier. The value is
     * converted to decimal with the given number of digits, including
     * leading zeroes if necessary.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        char[] r = new char[width];
        int v = value;
        for (int i = 0; i < r.length; i++) {
            r[r.length - 1 - i] = (char) ('0' + (v % 10));
            v /= 10;
        }
        return new String(r);
    }

    /**
     * Get the hash code of this object.
     * 
     * @return the object's hash code
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + this.value;
        hash = 41 * hash + this.width;
        return hash;
    }

    /**
     * Test whether another object is equal to this one.
     * 
     * @param obj the object to test
     * 
     * @return {@code true} if the object is an extension identifier
     * with the same width and value; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final InternalId other = (InternalId) obj;
        if (this.value != other.value) return false;
        return this.width == other.width;
    }
}
