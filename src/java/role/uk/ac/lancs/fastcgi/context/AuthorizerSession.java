/*
 * Copyright (C) 2022 Regents of the University of Lancaster
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ac.lancs.fastcgi.context;

/**
 * Presents the context of a FastCGI session to an application in the
 * Authorizer role.
 * 
 * @author simpsons
 * 
 * @see <a href=
 * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S6.3">FastCGI
 * Specification &mdash; Authorizer</a>
 */
public interface AuthorizerSession extends Session {
    /**
     * Set an authentication/authorization variable. The status is set
     * to 200.
     * 
     * @param name the variable's name
     * 
     * @param value the variable's value
     * 
     * @throws IllegalStateException if the response output has been
     * started (with {@link #out()})
     */
    void setVariable(String name, String value);

    /**
     * {@inheritDoc} The status is set to 401 if currently 200.
     * 
     * @throws IllegalArgumentException if the name case-insensitively
     * begins with {@value #VARIABLE_PREFIX} or matches
     * {@value Session#STATUS_FIELD}
     * 
     * @throws IllegalStateException if the response output has been
     * started (with {@link #out()})
     */
    @Override
    void setField(String name, String value);

    /**
     * {@inheritDoc} The response status is not changed.
     * 
     * @throws IllegalArgumentException if the name case-insensitively
     * begins with {@value #VARIABLE_PREFIX} or matches
     * {@value Session#STATUS_FIELD}
     * 
     * @throws IllegalStateException if the response output has been
     * started (with {@link #out()})
     */
    @Override
    public void clearField(String name);

    /**
     * {@inheritDoc} The status is set to 401 if currently 200.
     * 
     * @throws IllegalArgumentException if the name case-insensitively
     * begins with {@value #VARIABLE_PREFIX} or matches
     * {@value Session#STATUS_FIELD}
     * 
     * @throws IllegalStateException if the response output has been
     * started (with {@link #out()})
     */
    @Override
    public void addField(String name, String value);

    /**
     * Specifies the prefix for special header field names that
     * configure the server on a successful authorization. These fields
     * cannot be modified through {@link #setField(String, String)},
     * {@link #addField(String, String)} and
     * {@link #clearField(String)}. Use
     * {@link #setVariable(String, String)} instead, without the prefix.
     * The prefix is {@value}.
     */
    String VARIABLE_PREFIX = "Variable-";
}
