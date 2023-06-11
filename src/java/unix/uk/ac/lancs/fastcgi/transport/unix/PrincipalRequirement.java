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

package uk.ac.lancs.fastcgi.transport.unix;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.net.UnixDomainPrincipal;

/**
 * Specifies a required user, group or combination of both.
 *
 * @author simpsons
 */
final class PrincipalRequirement {
    static final String PRINCIPAL_TEXT = "^([^@]*)(?:@(.*))?$";

    private static final Pattern PRINCIPAL =
        Pattern.compile(PRINCIPAL_TEXT, Pattern.CASE_INSENSITIVE);

    private final String user;

    private final String group;

    private PrincipalRequirement(String user, String group) {
        this.user = user;
        this.group = group;
    }

    /**
     * Create a requirement for a specific user.
     * 
     * @param user the required user
     * 
     * @return the requested requirement
     */
    public static PrincipalRequirement ofUser(String user) {
        return new PrincipalRequirement(Objects.requireNonNull(user, "user"),
                                        null);
    }

    /**
     * Create a requirement for a specific group.
     * 
     * @param group the required group
     * 
     * @return the requested requirement
     */
    public static PrincipalRequirement ofGroup(String group) {
        return new PrincipalRequirement(null,
                                        Objects.requireNonNull(group, "group"));
    }

    /**
     * Create a requirement for a specific user in a specific group.
     * 
     * @param group the required group
     * 
     * @param user the required user
     * 
     * @return the requested requirement
     */
    public static PrincipalRequirement of(String user, String group) {
        return new PrincipalRequirement(Objects.requireNonNull(user, "user"),
                                        Objects.requireNonNull(group, "group"));
    }

    /**
     * Create a requirement from a textual specification. The text must
     * match the regular expression {@value #PRINCIPAL_TEXT}, and must
     * not be an empty string. Everything up to the <samp>&#64;</samp>
     * is taken as the user; if empty, any user matches. The group is
     * specified after the <samp>&#64;</samp>. If no <samp>&#64;</samp>
     * is given, the string is taken as a user, and any group matches.
     * 
     * @param text the specification
     * 
     * @return the requested requirement
     * 
     * @throws IllegalArgumentException if the text fails to meet the
     * specification pattern
     */
    public static PrincipalRequirement of(String text) {
        Matcher m = PRINCIPAL.matcher(text);
        if (!m.matches())
            throw new IllegalArgumentException("bad principal: " + text);
        String user = m.group(1);
        String group = m.group(2);
        if (user.isEmpty()) user = null;
        if (group != null && group.isEmpty()) group = null;
        if (user == null && group == null)
            throw new IllegalArgumentException("empty principal");
        return new PrincipalRequirement(user, group);
    }

    /**
     * Test a Unix-domain principal against this requirement.
     * 
     * @param cand the principal to test
     * 
     * @return {@code true} if the principal meets the requirement;
     * {@code false} otherwise
     */
    public boolean test(UnixDomainPrincipal cand) {
        if (user != null && !cand.user().getName().equals(user)) return false;
        if (group != null && !cand.group().getName().equals(group))
            return false;
        return true;
    }

    /**
     * Get the hash code for this object.
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.user);
        hash = 53 * hash + Objects.hashCode(this.group);
        return hash;
    }

    /**
     * Test whether this requirement is equivalent to another object.
     * 
     * @param obj the object to test
     * 
     * @return {@code true} if the object is a requirement, and requires
     * the same user and group; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final PrincipalRequirement other = (PrincipalRequirement) obj;
        if (!Objects.equals(this.user, other.user)) return false;
        return Objects.equals(this.group, other.group);
    }

    /**
     * Get a string representation of this requirement. This matches the
     * pattern accepted by {@link #of(String)}.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        if (user == null) return "@" + group;
        if (group == null) return user;
        return user + "@" + group;
    }
}
