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

package uk.ac.lancs.http;

/**
 * Defines HTTP status codes, maps them to human-readable text, and
 * parses and generates HTTP timestamps.
 *
 * @author simpsons
 */
public final class ResponseCodes {
    /**
     * Indicates that the request body may be transmitted.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.100
     * RFC9110 &mdash; 100 Continue
     */
    public static final int CONTINUE = 100;

    /**
     * Indicates that protocol is changing after this response.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.101
     * RFC9110 &mdash; 101 Switching Protocols
     */
    public static final int SWITCHING_PROTOCOLS = 101;

    /**
     * Indicates that processing of the request is not complete.
     * 
     * @spec https://www.rfc-editor.org/info/rfc2518/#section-10.1
     * RFC2518 &mdash; 102 Processing
     * 
     * @deprecated This was removed in
     * <a href="https://www.rfc-editor.org/info/rfc4918/">RFC4918</a>.
     */
    @Deprecated
    public static final int PROCESSING = 102;

    /**
     * Indicates that a request has been served successfully.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.200
     * RFC9110 &mdash; 200 OK
     */
    public static final int OK = 200;

    /**
     * Indicates that a resource was created.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.201
     * RFC9110 &mdash; 201 Created
     */
    public static final int CREATED = 201;

    /**
     * Indicates that a request has been accepted for processing.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.202
     * RFC9110 &mdash; 202 Accepted
     */
    public static final int ACCEPTED = 202;

    /**
     * Indicates that the response content has been modified by a proxy.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.203
     * RFC9110 &mdash; 203 Non-Authoritative Information
     */
    public static final int NON_AUTHORITATIVE_INFORMATION = 203;

    /**
     * Indicates that a request is successful, and that the client need
     * not traverse away from its current document view.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.204
     * RFC9110 &mdash; 204 No Content
     */
    public static final int NO_CONTENT = 204;

    /**
     * Tells the client to reset its document view.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.205
     * RFC9110 &mdash; 205 Reset Content
     */
    public static final int RESET_CONTENT = 205;

    /**
     * Indicates that not all requested parts are present in the
     * response.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.206
     * RFC9110 &mdash; 206 Partial Content
     */
    public static final int PARTIAL_CONTENT = 206;

    /**
     * Provides statuses got multiple independent operations.
     * 
     * @spec https://www.rfc-editor.org/info/rfc4918/#section-11.1
     * RFC4918 &mdash; Multi-Status
     */
    public static final int MULTI_STATUS = 207;

    /**
     * Indicates that DAV information was provided in a previous
     * response.
     * 
     * @spec https://www.rfc-editor.org/info/rfc5842/#section-7.1
     * RFC5842 &mdash; 208 Already Reported
     */
    public static final int ALREADY_REPORTED = 208;

    /**
     * Indicates that the response is the result of
     * instance-manipulation.
     * 
     * @spec https://www.rfc-editor.org/info/rfc3229/#section-10.4.1
     * RFC3229 &mdash; 226 IM Used
     */
    public static final int IM_USED = 226;

    /**
     * Tells the client that it must make another request to get the
     * resource, by selecting from information in the response.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.300
     * RFC9110 &mdash; 300 Multiple Choices
     */
    public static final int MULTIPLE_CHOICES = 300;

    /**
     * Tells the client that the URI it used should be replaced by
     * another.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.301
     * RFC9110 &mdash; 301 Moved Permanently
     */
    public static final int MOVED_PERMANENTLY = 301;

    /**
     * Tells the client that it should use a different URI to fulfil the
     * request.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.302
     * RFC9110 &mdash; 302 Found
     */
    public static final int FOUND = 302;

    /**
     * Tells the client that it should get a representation of the
     * result of its request by performing a GET on a URI, even if the
     * original request used a different method.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.303
     * RFC9110 &mdash; 303 See Other
     */
    public static final int SEE_OTHER = 303;

    /**
     * Indicates that the client can obtain the resource from its own
     * cache.
     * 
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.304
     * RFC9110 &mdash; 304 Not Modified
     */
    public static final int NOT_MODIFIED = 304;

    /**
     * Indicates that the client must re-issue the request through a
     * given proxy.
     * 
     * @spec https://www.rfc-editor.org/info/rfc2616/#section-10.3.6
     * RFC2616 &mdash; 305 Use Proxy
     * 
     * @deprecated This is <a href=
     * "https://www.rfc-editor.org/info/rfc7231/#section-6.4.5">deprecated
     * in RFC7231</a>.
     */
    @Deprecated
    public static final int USE_PROXY = 305;

    /**
     * Indicates that the client should access the resource under a
     * different URI, but that the current URI remains valid.
     * 
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.307
     * RFC9110 &mdash; 307 Temporary Redirect
     */
    public static final int TEMPORARY_REDIRECT = 307;

    /**
     * Indicates that the client should access the resource under a
     * different URI, and that the current URI should no longer be used.
     * 
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.308
     * RFC9110 &mdash; 308 Permanent Redirect
     */
    public static final int PERMANENT_REDIRECT = 308;

    /**
     * Indicates that the client seems to have made a bad request.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.400
     * RFC9110 &mdash; 400 Bad Request
     */
    public static final int BAD_REQUEST = 400;

    /**
     * Tells the client that access is not granted, and that it may try
     * again with new credentials.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.401
     * RFC9110 &mdash; 401 Unauthorized
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * Reserved for future use.
     * 
     * <p>
     * This is mentioned at least as far back as RFC2616.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.402
     * RFC9110 &mdash; 402 Payment Required
     */
    public static final int PAYMENT_REQUIRED = 402;

    /**
     * Tells the client that access is not granted, and that it should
     * not automatically re-attempt the request with new credentials.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.403
     * RFC9110 &mdash; 403 Forbidden
     */
    public static final int FORBIDDEN = 403;

    /**
     * Indicates that the URI does not refer to a known resource.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.404
     * RFC9110 &mdash; 404 Not Found
     */
    public static final int NOT_FOUND = 404;

    /**
     * Indicates that the request method is forbidden on the resource.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.405
     * RFC9110 &mdash; 405 Method Not Allowed
     */
    public static final int METHOD_NOT_ALLOWED = 405;

    /**
     * Indicates that no available representation would be acceptable to
     * the client.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.406
     * RFC9110 &mdash; 406 Not Acceptable
     */
    public static final int NOT_ACCEPTABLE = 406;

    /**
     * Indicates that the client must authenticate with a proxy.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.406
     * RFC9110 &mdash; 407 Proxy Authentication Required
     */
    public static final int PROXY_AUTHENTICATION_REQUIRED = 407;

    /**
     * Indicates that the server gave up waiting for a complete request
     * message.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.408
     * RFC9110 &mdash; 408 Request Timeout
     */
    public static final int REQUEST_TIMEOUT = 408;

    /**
     * Indicates that the request is in conflict with that target
     * resource's current state.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.409
     * RFC9110 &mdash; 409 Conflict
     */
    public static final int CONFLICT = 409;

    /**
     * Indicates that the target resource is no longer available.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.410
     * RFC9110 &mdash; 410 Gone
     */
    public static final int GONE = 410;

    /**
     * Indicates that the request must include a content length.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.411
     * RFC9110 &mdash; 411 Length Required
     */
    public static final int LENGTH_REQUIRED = 411;

    /**
     * Indicates that the client's preconditions were not met.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.412
     * RFC9110 &mdash; 412 Precondition Failed
     */
    public static final int PRECONDITION_FAILED = 412;

    /**
     * Indicates that the request content is too large.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.413
     * RFC9110 &mdash; 413 Content Too Large
     */
    public static final int CONTENT_TOO_LARGE = 413;

    /**
     * Indicates that the request URI is too large.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.414
     * RFC9110 &mdash; 414 URI Too Long
     */
    public static final int URI_TOO_LONG = 414;

    /**
     * Indicates that the request content's format is unsupported.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.415
     * RFC9110 &mdash; 415 Unsupported Media Type
     */
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;

    /**
     * Indicates that none of the requested ranges is satisfiable.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.416
     * RFC9110 &mdash; 416 Range Not Satisfiable
     */
    public static final int RANGE_NOT_SATISFIABLE = 416;

    /**
     * Indicates that the request's expectations could not be met.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.417
     * RFC9110 &mdash; 417 Expectation Failed
     */
    public static final int EXPECTATION_FAILED = 417;

    /**
     * This is an April Fool's joke.
     * 
     * @spec https://www.rfc-editor.org/info/rfc2324/#section-2.3.2
     * RFC2324 &mdash; I'm a teapot
     * 
     * @deprecated This was a joke.
     */
    @Deprecated
    public static final int IM_A_TEAPOT = 418;

    /**
     * Indicates that the server cannot produce an authoritative
     * response for the target.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.421
     * RFC9110 &mdash; 421 Misdirected Request
     */
    public static final int MISDIRECTED_REQUEST = 421;

    /**
     * Indicates that the server will not process semantically incorrect
     * content.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.422
     * RFC9110 &mdash; 422 Unprocessable Content
     */
    public static final int UNPROCESSABLE_CONTENT = 422;

    /**
     * Indicates that the source or destination resource for a given
     * method is locked.
     * 
     * @spec https://www.rfc-editor.org/info/rfc4918/#section-11.3
     * RFC4918 &mdash; 423 Locked
     */
    public static final int LOCKED = 423;

    /**
     * Indicates that the request depends on another action which
     * failed.
     * 
     * @spec https://www.rfc-editor.org/info/rfc4918/#section-11.4
     * RFC4918 &mdash; 424 Failed Dependency
     */
    public static final int FAILED_DEPENDENCY = 424;

    /**
     * Indicates that the server will not process the request without
     * switching to another protocol.
     *
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.426
     * RFC9110 &mdash; 426 Upgrade Required
     */
    public static final int UPGRADE_REQUIRED = 426;

    /**
     * Indicates that the request must be conditional.
     * 
     * @spec https://www.rfc-editor.org/info/rfc6585/#section-3 RFC6585
     * &mdash; 428 Precondition Required
     */
    public static final int PRECONDITION_REQUIRED = 428;

    /**
     * Indicates that the client is issuing too many requests.
     * 
     * @spec https://www.rfc-editor.org/info/rfc6585/#section-4 RFC6585
     * &mdash; 429 Too Many Requests
     */
    public static final int TOO_MANY_REQUESTS = 429;

    /**
     * Indicates that some of the request header fields are too large.
     * 
     * @spec https://www.rfc-editor.org/info/rfc6585/#section-5 RFC6585
     * &mdash; 431 Request Header Fields Too Large
     */
    public static final int REQUEST_HEADER_FIELDS_TOO_LARGE = 431;

    /**
     * Indicates that access is denied for legal reasons.
     * 
     * @spec https://www.rfc-editor.org/info/rfc7725/#section-3 RFC7725
     * &mdash; 451 Unavailable For Legal Reasons
     */
    public static final int UNAVAILABLE_FOR_LEGAL_REASONS = 451;

    /**
     * Indicates an unexpected condition in the server.
     * 
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.500
     * RFC9110 &mdash; 500 Internal Server Error
     */
    public static final int INTERNAL_SERVER_ERROR = 500;

    /**
     * Indicates a lack of functionality in the server.
     * 
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.501
     * RFC9110 &mdash; 501 Not Implemented
     */
    public static final int NOT_IMPLEMENTED = 501;

    /**
     * Indicates that the server received an invalid response from an
     * upstream server.
     * 
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.502
     * RFC9110 &mdash; 502 Bad Gateway
     */
    public static final int BAD_GATEWAY = 502;

    /**
     * Indicates that the server is temporarily unable to handle the
     * request.
     * 
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.503
     * RFC9110 &mdash; 503 Service Unavailable
     */
    public static final int SERVICE_UNAVILABLE = 503;

    /**
     * Indicates that the server did not receive a timely response from
     * an upstream server.
     * 
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.504
     * RFC9110 &mdash; 504 Gateway Timeout
     */
    public static final int GATEWAY_TIMEOUT = 504;

    /**
     * Indicates that the server does not support the major version of
     * the protocol.
     * 
     * @spec https://www.rfc-editor.org/rfc/rfc9110.html#status.505
     * RFC9110 &mdash; 505 HTTP Version Not Supported
     */
    public static final int HTTP_VERSION_NOT_SUPPORTED = 505;

    /**
     * Indicates that the chosen variant is itself configured to engage
     * in content negotiation.
     * 
     * @spec https://www.rfc-editor.org/info/rfc2295/#section-8.1
     * RFC2295 &mdash; 506 Variant Also Negotiates
     */
    public static final int VARIANT_ALSO_NEGOTIATES = 506;

    /**
     * Indicates that the server is temporarily unable to store the
     * representation.
     * 
     * @spec https://datatracker.ietf.org/doc/html/rfc4918#section-11.5
     * RFC4918 &mdash; Insufficient Storage
     */
    public static final int INSUFFICIENT_STORAGE = 507;

    /**
     * Indicates that the server terminated the operation because it
     * detected an infinite loop.
     * 
     * @spec https://www.rfc-editor.org/info/rfc5842/#section-7.2
     * RFC5842 &mdash; 508 Loop Detected
     */
    public static final int LOOP_DETECTED = 508;

    /**
     * Indicates that at least one mandatory extension is not supported.
     * 
     * @spec https://www.rfc-editor.org/info/rfc2774/#section-7 RFC2774
     * &mdash; 510 Not Extended
     */
    public static final int NOT_EXTENDED = 510;

    /**
     * Indicates that a proxy requires the client to authenticate.
     * 
     * @spec https://www.rfc-editor.org/info/rfc6585/#section-6 RFC6585
     * &mdash; 511 Network Authentication Required
     */
    public static final int NETWORK_AUTHENTICATION_REQUIRED = 511;

    private static final String UNKNOWN_RESPONSE_PREFIX = "UNKNOWN-RESPONSE-";

    /**
     * Convert an HTTP status code into its human-readable equivalent.
     *
     * @param code the code to convert
     *
     * @return the equivalent message; or
     * <code>{@value #UNKNOWN_RESPONSE_PREFIX}</code> followed by the
     * decimal code.
     */
    public static String getStatusMessage(int code) {
        return switch (code) {
        default -> UNKNOWN_RESPONSE_PREFIX + code;
        case CONTINUE -> "Continue";
        case SWITCHING_PROTOCOLS -> "Switching Protocols";
        case PROCESSING -> "Processing";
        case OK -> "OK";
        case CREATED -> "Created";
        case ACCEPTED -> "Accepted";
        case NON_AUTHORITATIVE_INFORMATION -> "Non-authoritative Information";
        case NO_CONTENT -> "No Content";
        case RESET_CONTENT -> "Reset Content";
        case PARTIAL_CONTENT -> "Partial Content";
        case MULTI_STATUS -> "Multi-Status";
        case ALREADY_REPORTED -> "Already Reported";
        case IM_USED -> "IM Used";
        case MULTIPLE_CHOICES -> "Multiple Choices";
        case MOVED_PERMANENTLY -> "Moved Permanently";
        case FOUND -> "Found";
        case SEE_OTHER -> "See Other";
        case NOT_MODIFIED -> "Not Modified";
        case USE_PROXY -> "Use Proxy";
        case TEMPORARY_REDIRECT -> "Temporary Redirect";
        case PERMANENT_REDIRECT -> "Permanent Redirect";
        case BAD_REQUEST -> "Bad Request";
        case UNAUTHORIZED -> "Unauthorized";
        case PAYMENT_REQUIRED -> "Payment Required";
        case FORBIDDEN -> "Forbidden";
        case NOT_FOUND -> "Not Found";
        case METHOD_NOT_ALLOWED -> "Method Not Allowed";
        case NOT_ACCEPTABLE -> "Not Acceptable";
        case PROXY_AUTHENTICATION_REQUIRED -> "Proxy Authentication Required";
        case REQUEST_TIMEOUT -> "Request Timeout";
        case CONFLICT -> "Conflict";
        case GONE -> "Gone";
        case LENGTH_REQUIRED -> "Length Required";
        case PRECONDITION_FAILED -> "Precondition Failed";
        case CONTENT_TOO_LARGE -> "Content Too Large";
        case URI_TOO_LONG -> "URI Too Long";
        case UNSUPPORTED_MEDIA_TYPE -> "Unsupported Media Type";
        case RANGE_NOT_SATISFIABLE -> "Range Not Satisfiable";
        case EXPECTATION_FAILED -> "Expectation Failed";
        case IM_A_TEAPOT -> "I'm a teapot";
        case MISDIRECTED_REQUEST -> "Misdirected Request";
        case UNPROCESSABLE_CONTENT -> "Unprocessable Content";
        case LOCKED -> "Locked";
        case FAILED_DEPENDENCY -> "Failed Dependency";
        case UPGRADE_REQUIRED -> "Upgrade Required";
        case PRECONDITION_REQUIRED -> "Precondition Required";
        case TOO_MANY_REQUESTS -> "Too Many Requests";
        case REQUEST_HEADER_FIELDS_TOO_LARGE ->
            "Request Header Fields Too Large";
        case UNAVAILABLE_FOR_LEGAL_REASONS -> "Unavailable For Legal Reasons";
        case INTERNAL_SERVER_ERROR -> "Internal Server Error";
        case NOT_IMPLEMENTED -> "Not Implemented";
        case BAD_GATEWAY -> "Bad Gateway";
        case SERVICE_UNAVILABLE -> "Service Unavailable";
        case GATEWAY_TIMEOUT -> "Gateway Timeout";
        case HTTP_VERSION_NOT_SUPPORTED -> "HTTP Version Not Supported";
        case VARIANT_ALSO_NEGOTIATES -> "Variant Also Negotiates";
        case INSUFFICIENT_STORAGE -> "Insufficient Storage";
        case LOOP_DETECTED -> "Loop Detected";
        case NOT_EXTENDED -> "Not Extended";
        case NETWORK_AUTHENTICATION_REQUIRED ->
            "Network Authentication Required";
        };
    }

    private ResponseCodes() {}
}
