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

import java.util.Objects;

/**
 * Identifies a group of media types.
 *
 * @author simpsons
 */
public final class MediaGroup {
    private final String major;

    private final String minor;

    private MediaGroup(String major, String minor) {
        assert major != null || minor == null;
        this.major = major;
        this.minor = minor;
    }

    private static boolean parseAny(Tokenizer tokenizer) {
        try (var mark = tokenizer.mark()) {
            if (tokenizer.character('*') && tokenizer.character('/') &&
                tokenizer.character('*')) {
                mark.pass();
                return true;
            }
            return false;
        }
    }

    /**
     * Get the hash code for this media group.
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 13 * hash + Objects.hashCode(this.major);
        hash = 13 * hash + Objects.hashCode(this.minor);
        return hash;
    }

    /**
     * Test whether this group equals another object. This performs an
     * exact match, and cannot test for membership.
     * 
     * @param obj the object to test against
     * 
     * @return {@code true} if the other object is a media group with
     * the same major and minor components
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final MediaGroup other = (MediaGroup) obj;
        if (!Objects.equals(this.major, other.major)) return false;
        return Objects.equals(this.minor, other.minor);
    }

    /**
     * Determine whether a MIME media type belongs in this group, and
     * how exactly.
     * 
     * @param type the media type to test
     * 
     * @return {@code 0} if the type does not belong to the group;
     * {@code 1} if the group matches all types; {@code 2} if the group
     * matches the major type and the minor part is wild; {@code 3} if
     * the group and type match exactly
     */
    public int contains(MediaType type) {
        if (this.major == null) return 1;
        if (!this.major.equals(type.major())) return 0;
        if (this.minor == null) return 2;
        return this.minor.equals(type.minor()) ? 3 : 0;
    }

    /**
     * Parse a media group from a MIME tokenizer. On failure, the
     * tokenizer is unchanged.
     * 
     * @param tokenizer the source of tokens
     * 
     * @return the media group; or {@code null} if not recognized
     */
    public static MediaGroup from(Tokenizer tokenizer) {
        if (parseAny(tokenizer)) return new MediaGroup(null, null);
        try (var mark = tokenizer.mark()) {
            CharSequence major = tokenizer.atom();
            if (major == null) return null;
            if (!tokenizer.character('/')) return null;
            if (tokenizer.character('*')) {
                mark.pass();
                return new MediaGroup(major.toString(), null);
            }
            CharSequence minor = tokenizer.atom();
            if (minor == null) return null;
            mark.pass();
            return new MediaGroup(major.toString(), minor.toString());
        }
    }

    /**
     * Get a string representation of this group. This is one of:
     * 
     * <ul>
     * 
     * <li><samp>*&#47;*</samp>, matching all types;
     * 
     * <li><samp><var>major</var>&#47;*</samp>, matching all types of a
     * given major type; or
     * 
     * <li><samp><var>major</var>/<var>minor</var></samp>, matching a
     * specific type.
     * 
     * </ul>
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        if (major == null) return "*/*";
        if (minor == null) return major + "/*";
        return major + '/' + minor;
    }
}
