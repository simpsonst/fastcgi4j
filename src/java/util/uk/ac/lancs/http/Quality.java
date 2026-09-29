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

package uk.ac.lancs.http;

/**
 * Holds a real value of up to 3 decimal places in the range [0,1].
 *
 * @author simpsons
 */
class Quality extends Number implements Comparable<Quality> {
    private final short rawValue;

    private Quality(int rawValue) {
        this.rawValue = (short) rawValue;
    }

    /**
     * The maximum quality value
     */
    public static final Quality ONE = new Quality(1000);

    private static final Quality Q900 = new Quality(900);

    private static final Quality Q800 = new Quality(800);

    private static final Quality Q700 = new Quality(700);

    private static final Quality Q600 = new Quality(600);

    private static final Quality Q500 = new Quality(500);

    private static final Quality Q400 = new Quality(400);

    private static final Quality Q300 = new Quality(300);

    private static final Quality Q200 = new Quality(200);

    private static final Quality Q100 = new Quality(100);

    /**
     * The minimum quality value
     */
    public static final Quality ZERO = new Quality(0);

    /**
     * Get the quality value from a textual representation. The
     * representation is simply any decimal value in the range [0,1].
     * Digits beyond the third decimal place are truncated.
     * 
     * @param text the textual representation
     * 
     * @return the quality value; or the value of {@code 1.0} if the
     * argument is {@code null}
     * 
     * @throws NumberFormatException if the argument is not a decimal in
     * the range [0,1]
     */
    public static Quality valueOf(String text) {
        if (text == null) return ONE;
        double d = Double.parseDouble(text);
        if (d < 0.0 || d > 1.0)
            throw new NumberFormatException("bad quality: " + text);
        short raw = (short) (d * 1000.0);
        return switch (raw) {
        case 0 -> ZERO;
        case 100 -> Q100;
        case 200 -> Q200;
        case 300 -> Q300;
        case 400 -> Q400;
        case 500 -> Q500;
        case 600 -> Q600;
        case 700 -> Q700;
        case 800 -> Q800;
        case 900 -> Q900;
        case 1000 -> ONE;
        default -> new Quality(raw);
        };
    }

    /**
     * Get the hash code of this object.
     * 
     * @return the hash code of this object
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + this.rawValue;
        return hash;
    }

    /**
     * Test whether this object equals another.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is a quality with the
     * same value; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Quality other = (Quality) obj;
        return this.rawValue == other.rawValue;
    }

    /**
     * Get a string representation of this object.
     * 
     * @return the string representation of this object, which is a
     * decimal with 3 decimal places
     */
    @Override
    public String toString() {
        return String.format("%.3f", rawValue / 1000.0);
    }

    /**
     * Get this value as an {@code int}. As the only values of
     * {@code int} that intersect with this type are 0 and 1, this
     * method discards most of the precision, and should be avoided.
     * 
     * @return this quality value as an {@code int}, rounded to 0 or 1
     */
    @Override
    public int intValue() {
        return rawValue / 1000;
    }

    /**
     * Get this value as a {@code long}. As the only values of
     * {@code long} that intersect with this type are 0 and 1, this
     * method discards most of the precision, and should be avoided.
     * 
     * @return this quality value as a {@code long}, rounded to 0 or 1
     */
    @Override
    public long longValue() {
        return rawValue / 1000L;
    }

    /**
     * Get this value as a {@code float}.
     * 
     * @return this quality value as a {@code float}
     */
    @Override
    public float floatValue() {
        return rawValue / 1000.0f;
    }

    /**
     * Get this value as a {@code double}.
     * 
     * @return this quality value as a {@code double}
     */
    @Override
    public double doubleValue() {
        return rawValue / 1000.0;
    }

    /**
     * Compare this quality to another.
     * 
     * @param o the other quality
     * 
     * @return a negative value if this is a lower quality than the
     * argument; positive if higher; or zero if they are the same
     */
    @Override
    public int compareTo(Quality o) {
        return Short.compare(rawValue, o.rawValue);
    }
}
