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

package uk.ac.lancs.http.field;

/**
 * Indicates why a raw field name is rejected.
 *
 * @author simpsons
 */
public enum RejectionReason {
    /**
     * Indicates that a raw field name does not parse.
     */
    MALFORMED,

    /**
     * Indicates that a raw field name uses an unknown extension prefix.
     * This happens when an incoming header or trailer includes a field
     * using a prefix that has no definition in a <samp>Opt</samp>,
     * <samp>Man</samp>, <samp>C-Opt</samp> or <samp>C-Man></samp>
     * field. In particular, such definitions can only appear in a
     * header and not a trailer, and cannot be referenced from a trailer
     * unless defined in the corresponding trailer.
     */
    UNKNOWN_EXTENSION,

    /**
     * Indicates that a raw field name belongs to an extension whose
     * scope does not match according to corroborating information. This
     * typically means that (say) a field <samp>18-Foo</samp> belongs to
     * a hop-by-hop namespace, but it has not been listed in the
     * <samp>Connection</samp> field, or <span class="la">vice
     * versa</span>.
     * 
     * @see FieldScope
     */
    SCOPE_MISMATCH;
}
