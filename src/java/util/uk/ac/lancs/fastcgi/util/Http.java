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

/**
 * Defines HTTP status codes, maps them to human-readable text, and
 * parses and generates HTTP timestamps.
 *
 * @author simpsons
 */
public final class Http {
    /**
     * Indicates that a request has been accepted for processing.
     *
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-202-accepted">RFC9110
     * 202 Accepted</a>
     */
    public static final int ACCEPTED = 202;

    /**
     * Tells the client that it should get a representation of the
     * result of its request by performing a GET on a URI, even if the
     * original request used a different method.
     *
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-303-see-other">RFC9110
     * 303 See Other</a>
     */
    public static final int SEE_OTHER = 303;

    /**
     * Indicates that the client seems to have made a bad request.
     * 
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-400-bad-request">RFC9110
     * 400 Bad Request</a>
     */
    public static final int BAD_REQUEST = 400;

    /**
     * Indicates that the URI does not refer to a known resource.
     *
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-404-not-found">RFC9110
     * 404 Not Found</a>
     */
    public static final int NOT_FOUND = 404;

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
     * Indicates that the request method is forbidden on the resource.
     *
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-405-method-not-allowed">RFC9110
     * 405 Method Not Allowed</a>
     */
    public static final int METHOD_NOT_ALLOWED = 405;

    /**
     *
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-308-permanent-redirect">RFC9110
     * 308 Permanent Redirect</a>
     */
    public static final int PERMANENT_REDIRECT = 308;

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
     * Tells the client that access is not granted, and that it should
     * not automatically re-attempt the request with new credentials.
     *
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-403-forbidden">RFC9110
     * 403 Forbidden</a>
     */
    public static final int FORBIDDEN = 403;

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
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-304-not-modified">RFC9110
     * 304 Not Modified</a>
     */
    public static final int NOT_MODIFIED = 304;

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
     *
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-307-temporary-redirect">RFC9110
     * 307 Temporary Redirect</a>
     */
    public static final int TEMPORARY_REDIRECT = 307;

    /**
     * Indicates that a resource was created.
     *
     * @see <a href=
     * "https://www.rfc-editor.org/rfc/rfc9110.html#name-201-created">RFC9110
     * 201 Created</a>
     */
    public static final int CREATED = 201;

    private static final String UNKNOWN_RESPONSE_PREFIX = "UNKNOWN-RESPONSE-";

    /**
     * Convert an HTTP status code into its human-readable equivalent.
     *
     * @param code the code to convert
     *
     * @return the equivalent message; or
     * {@value #UNKNOWN_RESPONSE_PREFIX} followed by the code.
     */
    public static String getStatusMessage(int code) {
        return switch (code) {
        default -> UNKNOWN_RESPONSE_PREFIX + code;
        case 100 -> "Continue";
        case 101 -> "Switching Protocols";
        case 102 -> "Processing";
        case 200 -> "OK";
        case 201 -> "Created";
        case 202 -> "Accepted";
        case 203 -> "Non-authoritative Information";
        case 204 -> "No Content";
        case 205 -> "Reset Content";
        case 206 -> "Partial Content";
        case 207 -> "Multi-Status";
        case 208 -> "Already Reported";
        case 226 -> "IM Used";
        case 300 -> "Multiple Choices";
        case 301 -> "Moved Permanently";
        case 302 -> "Found";
        case 303 -> "See Other";
        case 304 -> "Not Modified";
        case 305 -> "Use Proxy";
        case 307 -> "Temporary Redirect";
        case 308 -> "Permanent Redirect";
        case 400 -> "Bad Request";
        case 401 -> "Unauthorized";
        case 402 -> "Payment Required";
        case 403 -> "Forbidden";
        case 404 -> "Not Found";
        case 405 -> "Method Not Allowed";
        case 406 -> "Not Acceptable";
        case 407 -> "Proxy Authentication Required";
        case 408 -> "Request Timeout";
        case 409 -> "Conflict";
        case 410 -> "Gone";
        case 411 -> "Length Required";
        case 412 -> "Precondition Failed";
        case 413 -> "Payload Too Large";
        case 414 -> "Request-URI Too Long";
        case 415 -> "Unsupported Media Type";
        case 416 -> "Requested Range Not Satisfiable";
        case 417 -> "Expectation Failed";
        case 418 -> "I'm a teapot";
        case 421 -> "Misdirected Request";
        case 422 -> "Unprocessable Entity";
        case 423 -> "Locked";
        case 424 -> "Failed Dependency";
        case 426 -> "Upgrade Required";
        case 428 -> "Precondition Required";
        case 429 -> "Too Many Requests";
        case 431 -> "Request Header Fields Too Large";
        case 444 -> "Connection Closed Without Response";
        case 451 -> "Unavailable For Legal Reasons";
        case 499 -> "Client Closed Request";
        case 500 -> "Internal Server Error";
        case 501 -> "Not Implemented";
        case 502 -> "Bad Gateway";
        case 503 -> "Service Unavailable";
        case 504 -> "Gateway Timeout";
        case 505 -> "HTTP Version Not Supported";
        case 506 -> "Variant Also Negotiates";
        case 507 -> "Insufficient Storage";
        case 508 -> "Loop Detected";
        case 510 -> "Not Extended";
        case 511 -> "Network Authentication Required";
        case 599 -> "Network Connect Timeout Error";
        };
    }

    private Http() {}
}
