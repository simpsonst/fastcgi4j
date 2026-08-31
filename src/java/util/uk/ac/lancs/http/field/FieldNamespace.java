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
 * Allows fields with the same core name to be distinguished. Regular
 * fields can be defined using
 * <code>{@link #STANDARD_END_TO_END}.{@linkplain #of(CharSequence) of}(core)</code>
 * or
 * <code>{@link #STANDARD_HOP_BY_HOP}.{@linkplain #of(CharSequence) of}(core)</code>.
 * A {@link FieldExtension} is a user-defined namespace.
 * 
 * @author simpsons
 */
public abstract class FieldNamespace {
    FieldNamespace() {}

    /**
     * Describes the kind of a namespace.
     */
    public static enum Kind {
        /**
         * Identifies the standard namespace. Fields in this namespace
         * have no prefix, and no namespace URI.
         */
        STANDARD,

        /**
         * Identifies the experimental namespace. Fields in this
         * namespace have the prefix <samp>X-</samp>, but no namespace
         * URI.
         */
        EXPERIMENTAL,

        /**
         * Identifies an extension namespace. Fields in this namespace
         * have a numeric prefix of at least two digits and a dash, and
         * are distinguished by a namespace URI.
         */
        EXTENSION;
    }

    /**
     * Get the kind of this namespace.
     * 
     * @return the namespace's kind
     */
    public abstract Kind kind();

    /**
     * Get the scope of this namespace.
     * 
     * @return the namespace scope
     */
    public abstract FieldScope scope();

    /**
     * Get this namespace as an extension.
     * 
     * @return this object as an extension if it is such; {@code null}
     * otherwise
     */
    public abstract FieldExtension asExtension();

    /**
     * Identify a field within this namespace.
     * 
     * @param core the core name for the field
     * 
     * @return the requested field id
     * 
     * @throws IllegalArgumentException if the core name is invalid
     * 
     * @throws NullPointerException if the argument is {@code null}
     * 
     * @constructor
     */
    public FieldId of(CharSequence core) {
        return new FieldId(this, core);
    }

    /**
     * Identifies the standard end-to-end namespace. This rejects core
     * names beginning with two or more digits and a dash, or with
     * <samp>X-</samp> (case-insensitive).
     */
    public static final FieldNamespace STANDARD_END_TO_END =
        new StandardNamespace() {
            @Override
            public FieldScope scope() {
                return FieldScope.END_TO_END;
            }
        };

    /**
     * Identifies the standard hop-by-hop namespace. This rejects core
     * names beginning with two or more digits and a dash, or with
     * <samp>X-</samp> (case-insensitive).
     */
    public static final FieldNamespace STANDARD_HOP_BY_HOP =
        new StandardNamespace() {
            @Override
            public FieldScope scope() {
                return FieldScope.HOP_BY_HOP;
            }
        };

    /**
     * Identifies the experimental hop-by-hop namespace. The core names
     * of fields in this namespace are prefixed with <samp>X-</samp>.
     */
    public static final FieldNamespace EXPERIMENTAL_HOP_BY_HOP =
        new ExperimentalNamespace() {
            @Override
            public FieldScope scope() {
                return FieldScope.HOP_BY_HOP;
            }
        };

    /**
     * Identifies the experimental end-to-end namespace. The core names
     * of fields in this namespace are prefixed with <samp>X-</samp>.
     */
    public static final FieldNamespace EXPERIMENTAL_END_TO_END =
        new ExperimentalNamespace() {
            @Override
            public FieldScope scope() {
                return FieldScope.END_TO_END;
            }
        };

}
