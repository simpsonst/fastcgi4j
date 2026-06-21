// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2024, Lancaster University
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

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds a valid header or trailer field name, possibly with a
 * namespace. A field identifier can also be deemed just for use in a
 * response header, a response trailer, or both.
 *
 * @author simpsons
 */
public final class FieldId {
    private static final String FIELD_PATTERN_TEXT =
        "^[a-zA-Z][0-9a-zA-Z]*(-[a-zA-Z][0-9a-zA-Z]*)*$";

    private static final Pattern FIELD_PATTERN =
        Pattern.compile(FIELD_PATTERN_TEXT);

    private final FieldNamespace namespace;

    private final String core;

    /**
     * Identify a field by namespace and core name.
     * 
     * @param namespace the namespace
     * 
     * @param core the core name
     * 
     * @throws IllegalArgumentException if the core name fails to match
     * the regular expression {@value #FIELD_PATTERN_TEXT}
     * 
     * @throws NullPointerException if either argument is {@code null}
     */
    FieldId(FieldNamespace namespace, CharSequence core) {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(core, "core");
        Matcher m = FIELD_PATTERN.matcher(core);
        if (m.matches())
            throw new IllegalArgumentException("bad field id core: " + core);
        this.namespace = namespace;
        this.core = core.toString();
    }

    /**
     * Get the field's namespace.
     * 
     * @return the field's namespace
     */
    public FieldNamespace namespace() {
        return namespace;
    }

    /**
     * Get the field name core.
     * 
     * @return the core used to name the field
     */
    public String name() {
        return core;
    }

    /**
     * Get the name as it appears in the CGI environment. That is, the
     * plain name is converted to upper case, dashes are replaced with
     * underscores.
     * 
     * @return the converted name
     */
    public String gatewayName() {
        return core.replace('-', '_').toUpperCase();
    }

    /**
     * Get a string representation of this identifier.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(core);
        if (namespace != null) result.append('/').append(namespace);
        return result.toString();
    }

    /**
     * Get the hash code of this object. This combines the hash codes of
     * its namespace and core components.
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.namespace);
        hash = 23 * hash + Objects.hashCode(this.core.toLowerCase(Locale.ROOT));
        return hash;
    }

    /**
     * Test whether another object is equal to this object.
     * 
     * @param obj the object to test
     * 
     * @return {@code true} if the object is a {@link FieldId}, and has
     * an identical namespace and core name (case-insensitive);
     * {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final FieldId other = (FieldId) obj;
        if (!Objects.equals(this.core.toLowerCase(Locale.ROOT),
                            other.core.toLowerCase(Locale.ROOT)))
            return false;
        return Objects.equals(this.namespace, other.namespace);
    }
}
