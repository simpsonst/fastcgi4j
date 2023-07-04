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

package uk.ac.lancs.fastcgi.util;

import java.net.URI;
import uk.ac.lancs.fastcgi.context.Session;

/**
 * Provides constants and methods for handling HTTP requests.
 * 
 * @author simpsons
 */
public final class Http {
    private Http() {}

    /**
     * Tell the client to GET the content from another location. This
     * can be used in response to a POST. Many browsers will GET the
     * indicated content and display it, and also replace the POST with
     * it in the history, allowing the user to go back and forth within
     * the history without re-posting.
     * 
     * @param session the session to respond to
     * 
     * @param location the location to redirect to
     * 
     * @see #SEE_OTHER
     */
    public static void seeOther(Session session, URI location) {
        setLocation(session, location, SEE_OTHER);
    }

    /**
     * Tell the client not to use the URI again, and use a different one
     * instead. The client is supposed to use the same request method,
     * but some might change it. Use
     * {@link #permanentRedirect(Session, URI)} instead to make using
     * the same method explicit.
     * 
     * @param session the session to respond to
     * 
     * @param location the location to redirect to
     * 
     * @see #MOVED_PERMANENTLY
     */
    public static void movedPermanently(Session session, URI location) {
        setLocation(session, location, MOVED_PERMANENTLY);
    }

    /**
     * Tell the client to issue the request to another location. This
     * should be seen as a temporary redirection, and the client should
     * not change its reference. It should also use the same request
     * method, but some might change it. Use
     * {@link #temporaryRedirect(Session, URI)} instead to make using
     * the same method explicit.
     * 
     * @param session the session to respond to
     * 
     * @param location the location to redirect to
     * 
     * @see #FOUND
     */
    public static void found(Session session, URI location) {
        setLocation(session, location, FOUND);
    }

    /**
     * Tell the client to make the same request to another location. The
     * client should not change its reference, and it must use the same
     * request method.
     * 
     * @param session the session to respond to
     * 
     * @param location the location to redirect to
     * 
     * @see #TEMPORARY_REDIRECT
     */
    public static void temporaryRedirect(Session session, URI location) {
        setLocation(session, location, TEMPORARY_REDIRECT);
    }

    /**
     * Tell the client to not to use the URI again, and use a different
     * one instead. The client should not change its reference, and it
     * must use the same request method.
     * 
     * @param session the session to respond to
     * 
     * @param location the location to redirect to
     * 
     * @see #PERMANENT_REDIRECT
     */
    public static void permanentRedirect(Session session, URI location) {
        setLocation(session, location, PERMANENT_REDIRECT);
    }

    private static void setLocation(Session session, URI location, int code) {
        session.addHeader("Location", location.toASCIIString());
        session.setStatus(code);
    }

    /**
     * Indicates that a resource was created.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-201-created">RFC9110
     * 201 Created</a>
     */
    public static final int CREATED = 201;

    /**
     * Indicates that a request has been accepted for processing.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-202-accepted">RFC9110
     * 202 Accepted</a>
     */
    public static final int ACCEPTED = 202;

    /**
     * Indicates that a request is successful, and that the client need
     * not traverse away from its current document view.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-204-no-content">RFC9110
     * 204 No Content</a>
     */
    public static final int NO_CONTENT = 204;

    /**
     * Tells that client that it must make another request to get the
     * resource, by selecting from information in the response.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-300-multiple-choices">RFC9110
     * 300 Multiple Choices</a>
     */
    public static final int MULTIPLE_CHOICES = 300;

    /**
     * Tells the client that the URI it used should be replaced by
     * another.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-301-moved-permanently">RFC9110
     * Moved Permanently</a>
     */
    public static final int MOVED_PERMANENTLY = 301;

    /**
     * Tells the client that it should use a different URI to fulfil the
     * request.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-302-found">RFC9110
     * 302 Found</a>
     */
    public static final int FOUND = 302;

    /**
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-303-see-other">RFC9110
     * 303 See Other</a>
     */
    public static final int SEE_OTHER = 303;

    /**
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-304-not-modified">RFC9110
     * 304 Not Modified</a>
     */
    public static final int NOT_MODIFIED = 304;

    /**
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-307-temporary-redirect">RFC9110
     * 307 Temporary Redirect</a>
     */
    public static final int TEMPORARY_REDIRECT = 307;

    /**
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-308-permanent-redirect">RFC9110
     * 308 Permanent Redirect</a>
     */
    public static final int PERMANENT_REDIRECT = 308;

    /**
     * Tells the client that access is not granted, and that it may try
     * again with new credentials.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-401-unauthorized">RFC9110
     * 401 Unauthorized</a>
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * Tells the client that access is not granted, and that it should
     * not automatically re-attempt the request with new credentials.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-403-forbidden">RFC9110
     * 403 Forbidden</a>
     */
    public static final int FORBIDDEN = 403;

    /**
     * Indicates that the URI does not refer to a known resource.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-404-not-found">RFC9110
     * 404 Not Found</a>
     */
    public static final int NOT_FOUND = 404;

    /**
     * Indicates that the request method is forbidden on the resource.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-405-method-not-allowed">RFC9110
     * 405 Method Not Allowed</a>
     */
    public static final int METHOD_NOT_ALLOWED = 405;
}
