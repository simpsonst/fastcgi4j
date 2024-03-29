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

/**
 * Defines symbols for significant US-ASCII characters' byte values.
 *
 * @author simpsons
 */
final class ASCII {
    private ASCII() {}

    /**
     * The byte value of the US-ASCII tab U+0009 character
     */
    static final byte TAB = 9;

    /**
     * The byte value of the US-ASCII linefeed U+000A character
     */
    static final byte LF = 10;

    /**
     * The byte value of the US-ASCII carriage-return U+000D character
     */
    static final byte CR = 13;

    /**
     * The byte value of the US-ASCII hyphen-minus U+002D character
     */
    static final byte DASH = 0x2d;

    /**
     * The byte value of the US-ASCII colon U+003A character
     */
    static final byte COLON = 0x3a;

    /**
     * The byte value of the US-ASCII equals U+003D character
     */
    static final byte EQUALS = 0x3d;

    /**
     * The byte value of the US-ASCII space U+0020 character
     */
    static final byte SPACE = 32;

    /**
     * The byte value of the US-ASCII delete U+007F character
     */
    static final byte DEL = 127;
}
