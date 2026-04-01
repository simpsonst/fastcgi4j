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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Defines an extension for a set of fields.
 *
 * @author simpsons
 */
public final class FieldExtension extends FieldNamespace {
    /**
     * Specifies the namespace URI of the extension.
     */
    public final URI nsuri;

    /**
     * Specifies the scope of fields in this extension.
     */
    public final FieldScope scope;

    /**
     * Specifies the strength of the extension.
     */
    public final FieldStrength strength;

    /**
     * Specifies attributes of the extension. The contents are
     * immutable.
     */
    public final Map<String, String> attrs;

    private FieldExtension(URI nsuri, FieldScope scope, FieldStrength strength,
                           Map<String, String> exts) {
        this.nsuri = nsuri;
        this.scope = scope;
        this.strength = strength;
        this.attrs = exts;
    }

    /**
     * Builds an extension in stages. The default scope is
     * {@link FieldScope#END_TO_END}. The default strength is
     * {@link FieldStrength#OPTIONAL}. The default set of extensions is
     * empty.
     */
    public static class Builder {
        final URI nsuri;

        FieldScope scope = FieldScope.END_TO_END;

        FieldStrength strength = FieldStrength.OPTIONAL;

        Map<String, String> attrs = new HashMap<>();

        Builder(URI nsuri) {
            this.nsuri = nsuri;
        }

        /**
         * Add or change an attribute.
         * 
         * @param name the attribute name
         * 
         * @param value the attribute value
         * 
         * @return this object
         * 
         * @throws IllegalArgumentException if either the name or value
         * are illegal
         * 
         * @throws NullPointerException if either argument is
         * {@code null}
         */
        public Builder attribute(CharSequence name, CharSequence value) {
            Objects.requireNonNull(name, "attr.name");
            Objects.requireNonNull(value, "attr.value");
            if ("ns".equals(name))
                throw new IllegalArgumentException("reserved attribute ns");
            attrs.put(name.toString(), value.toString());
            return this;
        }

        /**
         * Add several attributes.
         * 
         * @param attrs the set of attributes to add
         * 
         * @return this object
         */
        public Builder attributes(Map<? extends CharSequence,
                                      ? extends CharSequence> attrs) {
            for (var ent : attrs.entrySet())
                attribute(ent.getKey(), ent.getValue());
            return this;
        }

        /**
         * Set the strength.
         * 
         * @param state the new strength
         * 
         * @return this object
         * 
         * @throws NullPointerException if the argument is {@code null}
         */
        public Builder strength(FieldStrength state) {
            Objects.requireNonNull(state, "strength");
            this.strength = state;
            return this;
        }

        /**
         * Set the strength to &lsquo;mandatory&rsquo;.
         * 
         * @return this object
         * 
         * @see FieldStrength#MANDATORY
         */
        public Builder mandatory() {
            this.strength = FieldStrength.MANDATORY;
            return this;
        }

        /**
         * Set the strength to &lsquo;optional&rsquo;.
         * 
         * @return this object
         * 
         * @see FieldStrength#OPTIONAL
         */
        public Builder optional() {
            this.strength = FieldStrength.OPTIONAL;
            return this;
        }

        /**
         * Set the strength according to a flag indicating optionality.
         * 
         * @param state whether the strength is optional
         * 
         * @return this object
         */
        public Builder optional(boolean state) {
            return state ? optional() : mandatory();
        }

        /**
         * Set the strength according to a flag indicating
         * mandatoriness.
         * 
         * @param state whether the strength is mandatory
         * 
         * @return this object
         */
        public Builder mandatory(boolean state) {
            return state ? mandatory() : optional();
        }

        /**
         * Set the scope.
         * 
         * @param state the new scope
         * 
         * @return this object
         */
        public Builder scope(FieldScope state) {
            Objects.requireNonNull(state, "scope");
            this.scope = state;
            return this;
        }

        /**
         * Set the scope according to a flag indicating end-to-enditude.
         * 
         * @param state whether the scope if end-to-end
         * 
         * @return this object
         */
        public Builder endToEnd(boolean state) {
            return state ? endToEnd() : hopByHop();
        }

        /**
         * Set the scope according to a flag indicating
         * hop-by-hoppiness.
         * 
         * @param state whether the scope is hop-by-hop
         * 
         * @return this object
         */
        public Builder hopByHop(boolean state) {
            return state ? hopByHop() : endToEnd();
        }

        /**
         * Set the scope to &lsquo;end-to-end&rsquo;.
         * 
         * @return this object
         * 
         * @see FieldScope#END_TO_END
         */
        public Builder endToEnd() {
            this.scope = FieldScope.END_TO_END;
            return this;
        }

        /**
         * Set the scope to &lsquo;hop-by-hop&rsquo;.
         * 
         * @return this object
         * 
         * @see FieldScope#HOP_BY_HOP
         */
        public Builder hopByHop() {
            this.scope = FieldScope.HOP_BY_HOP;
            return this;
        }

        /**
         * Define an extension using the current parameters.
         * 
         * @return the completed extension
         * 
         * @constructor
         */
        public FieldExtension complete() {
            return new FieldExtension(nsuri, scope, strength,
                                      Map.copyOf(attrs));
        }
    }

    /**
     * Start defining an extension. Call {@link Builder#complete()} on
     * the result to generate the extension.
     * 
     * @param nsuri the namespace URI of the extension
     * 
     * @return an object for defining the extension in stages
     */
    public static Builder of(URI nsuri) {
        Objects.requireNonNull(nsuri, "ns");
        return new Builder(nsuri);
    }

    /**
     * Start defining an extension. Call {@link Builder#complete()} on
     * the result to generate the extension.
     * 
     * @param nsuri the namespace URI of the extension
     * 
     * @return an object for defining the extension in stages
     * 
     * @throws URISyntaxException if the argument is not a valid URI
     */
    public static Builder of(String nsuri) {
        Objects.requireNonNull(nsuri, "ns");
        return new Builder(URI.create(nsuri.trim()));
    }

    /**
     * Get the hash code for this object.
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.nsuri);
        hash = 29 * hash + Objects.hashCode(this.scope);
        hash = 29 * hash + Objects.hashCode(this.strength);
        hash = 29 * hash + Objects.hashCode(this.attrs);
        return hash;
    }

    /**
     * Test whether this id equals another object.
     * 
     * @param obj the object to test against
     * 
     * @return {@code true} if the other object is a field extension
     * with the same namespace URI, scope, depth and attributes;
     * {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final FieldExtension other = (FieldExtension) obj;
        if (!Objects.equals(this.nsuri, other.nsuri)) return false;
        if (this.scope != other.scope) return false;
        if (this.strength != other.strength) return false;
        return Objects.equals(this.attrs, other.attrs);
    }

    /**
     * {@inheritDoc}
     * 
     * @return always {@code false}
     */
    @Override
    public boolean isNative() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @return always {@code false}
     */
    @Override
    public boolean isExperimental() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @return always this object
     */
    @Override
    public FieldExtension asExtension() {
        return this;
    }

    @Override
    public FieldScope scope() {
        return scope;
    }
}
