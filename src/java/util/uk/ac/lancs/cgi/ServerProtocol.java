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

package uk.ac.lancs.cgi;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds the parsed components of a server protocol identification. This
 * normally takes the form
 * <samp><var>name</var>/<var>major</var>.<var>minor</var></samp>, where
 * <var>major</var> and <var>minor</var> are base-10 unsigned integers,
 * and <var>name</var> is a token consisting of the following
 * characters: <samp>0</samp>-<samp>9</samp> (U+0030-U+0039),
 * <samp>A</samp>-<samp>Z</samp> (U+0041-U+005A),
 * <samp>a</samp>-<samp>z</samp> (U+0061-U+007A) and
 * <samp>!#$%&amp;'*+-.`^_{|}~</samp>. Alternatively, the plain string
 * <samp>INCLUDED</samp> is accepted to identify processing as part of a
 * composite document; this is referred to here as the <dfn>inclusion
 * protocol</dfn>.
 *
 * @author simpsons
 * 
 * @see <a href=
 * "https://datatracker.ietf.org/doc/html/rfc3875#section-4.1.16">RFC3875
 * &mdash; SERVER_PROTOCOL</a>
 */
public final class ServerProtocol {
    private static final Pattern FORMAT =
        Pattern.compile("^(?<name>[-a-zA-Z0-9!#$%&'*+.`^_{|}~]+)"
            + "/(?<major>[0-9]+)" + "\\.(?<minor>[0-9]+)$");

    private final String name;

    private final int major;

    private final int minor;

    private ServerProtocol(CharSequence name, int major, int minor) {
        this.name = name.toString();
        this.major = major;
        this.minor = minor;
    }

    /**
     * Test whether this is the inclusion protocol.
     * 
     * @return {@code true}
     */
    public boolean isIncluded() {
        return "INCLUDED".equals(name) && major < 0 && minor < 0;
    }

    /**
     * Get the protocol name.
     * 
     * @return the protocol name; or <samp>INCLUDED</samp> if this is
     * the inclusion protocol
     */
    public String name() {
        return name;
    }

    /**
     * Get the major version number.
     * 
     * @return the major version number; or <code>-1</code> if this is
     * the inclusion protocol
     */
    public int major() {
        return major;
    }

    /**
     * Get the minor version number.
     * 
     * @return the minor version number; or <code>-1</code> if this is
     * the inclusion protocol
     */
    public int minor() {
        return minor;
    }

    /**
     * Parse a server protocol. Parsing is strict; no white space is
     * permitted anywhere.
     * 
     * @param text the source text
     * 
     * @return the parsed protocol
     * 
     * @throws IllegalArgumentException if the text cannot be parsed
     */
    public static ServerProtocol of(CharSequence text) {
        if ("INCLUDED".equals(text)) return new ServerProtocol(text, -1, -1);
        Matcher m = FORMAT.matcher(text);
        if (!m.matches())
            throw new IllegalArgumentException("not server protocol: " + text);
        var major = m.group("major");
        var minor = m.group("minor");
        return new ServerProtocol(m.group("name"),
                                  Integer.parseUnsignedInt(major, 0,
                                                           major.length(), 10),
                                  Integer.parseUnsignedInt(minor, 0,
                                                           minor.length(), 10));
    }

    private static final String PROTO_VAR = "SERVER_PROTOCOL";

    /**
     * Extract the server protocol from CGI parameters if present. The
     * value of the parameter {@value #PROTO_VAR} is parsed if present.
     * 
     * @param params the CGI parameters
     * 
     * @return the parsed protocol; or {@code null} if
     * {@value #PROTO_VAR} is not defined in the parameters
     */
    public static ServerProtocol
        ofOptional(Map<? super String, ? extends CharSequence> params) {
        var text = params.get(PROTO_VAR);
        if (text == null) return null;
        return of(text);
    }

    /**
     * Extract the server protocol from CGI parameters. The value of the
     * parameter {@value #PROTO_VAR} is parsed.
     * 
     * @param params the CGI parameters
     * 
     * @return the parsed protocol
     * 
     * @throws NullPointerException if {@value #PROTO_VAR} is not
     * defined in the parameters
     */
    public static ServerProtocol
        of(Map<? super String, ? extends CharSequence> params) {
        ServerProtocol r = ofOptional(params);
        Objects.requireNonNull(r, PROTO_VAR);
        return r;
    }

    /**
     * Get the hash code for this object.
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 13 * hash + Objects.hashCode(this.name);
        hash = 13 * hash + this.major;
        hash = 13 * hash + this.minor;
        return hash;
    }

    /**
     * Test whether this object equals another.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if objects have identical name, major and
     * minor fields; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final ServerProtocol other = (ServerProtocol) obj;
        if (this.major != other.major) return false;
        if (this.minor != other.minor) return false;
        return Objects.equals(this.name, other.name);
    }

    /**
     * Get a string representation of the server protocol. For the
     * inclusion protocol, the representation is simply
     * <samp>INCLUDED</samp>. For other values, the representation is
     * the name, followed by a slash <samp>/</samp> U+002F, the major
     * version in denary, a dot <samp>.</samp> U+002E, and the minor
     * version in denary, e.g. <smap>HTTP/1.0</samp>.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        if (major < 0) return name;
        return name + '/' + major + '.' + minor;
    }
}
