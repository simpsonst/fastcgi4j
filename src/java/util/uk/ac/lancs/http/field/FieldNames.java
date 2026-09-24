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
 * Defines symbols for standard HTTP field names. Each field name is
 * held in a symbolic constant named by converting the HTTP field name
 * to upper case, and replacing dashes with underscores.
 * 
 * @author simpsons
 */
public final class FieldNames {
    private FieldNames() {}

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.connection
     * RFC9110 {@value "%s"}
     */
    public static final String CONNECTION = "Connection";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.content-type
     * RFC9110 {@value "%s"}
     */
    public static final String CONTENT_TYPE = "Content-Type";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc6265/#section-4.1
     * RFC6265 {@value "%s"}
     */
    public static final String SET_COOKIE = "Set-Cookie";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc6265/#section-4.2
     * RFC6265 {@value "%s"}
     */
    public static final String COOKIE = "Cookie";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.trailer
     * RFC9110 {@value "%s"}
     */
    public static final String TRAILER = "Trailer";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.transfer-encoding
     * RFC9110 {@value "%s"}
     */
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.content-length
     * RFC9110 {@value "%s"}
     */
    public static final String CONTENT_LENGTH = "Content-Length";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc2774/#section-4.2
     * RFC2774 Hop-by-Hop Extensions
     */
    public static final String C_MAN = "C-Man";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc2774/#section-4.2
     * RFC2774 Hop-by-Hop Extensions
     */
    public static final String C_OPT = "C-Opt";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc2774/#section-5.1
     * RFC2774 Fulfilling a Mandatory Request
     */
    public static final String C_EXT = "C-Ext";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc2774/#section-5.1
     * RFC2774 Fulfilling a Mandatory Request
     */
    public static final String EXT = "Ext";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc2774/#section-4.1
     * RFC2774 End-to-End Extensions
     */
    public static final String MAN = "Man";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc2774/#section-4.1
     * RFC2774 End-to-End Extensions
     */
    public static final String OPT = "Opt";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#field.cache-control
     * RFC9111 {@value "%s"}
     */
    public static final String CACHE_CONTROL = "Cache-Control";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.date RFC9110
     * {@value "%s"}
     */
    public static final String DATE = "Date";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     */
    public static final String PRAGMA = "Pragma";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.upgrade
     * RFC9110 {@value "%s"}
     */
    public static final String UPGRADE = "Upgrade";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.via RFC9110
     * {@value "%s"}
     */
    public static final String VIA = "Via";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.proxy-authenticate
     * RFC9110 {@value "%s"}
     */
    public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     */
    public static final String PUBLIC = "Public";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.allow
     * RFC9110 {@value "%s"}
     */
    public static final String ALLOW = "Allow";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     */
    public static final String CONTENT_BASE = "Content-Base";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.content-encoding
     * RFC9110 {@value "%s"}
     */
    public static final String CONTENT_ENCODING = "Content-Encoding";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.content-language
     * RFC9110 {@value "%s"}
     */
    public static final String CONTENT_LANGUAGE = "Content-Language";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.content-location
     * RFC9110 {@value "%s"}
     */
    public static final String CONTENT_LOCATION = "Content-Location";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     */
    public static final String CONTENT_MD5 = "Content-MD5";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.content-range
     * RFC9110 {@value "%s"}
     */
    public static final String CONTENT_RANGE = "Content-Range";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.etag RFC9110
     * {@value "%s"}
     */
    public static final String ETAG = "Etag";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     */
    public static final String EXPIRES = "Expires";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.last-modified
     * RFC9110 {@value "%s"}
     */
    public static final String LAST_MODIFIED = "Last-Modified";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.accept
     * RFC9110 {@value "%s"}
     */
    public static final String ACCEPT = "Accept";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.accept-charset
     * RFC9110 {@value "%s"}
     */
    public static final String ACCEPT_CHARSET = "Accept-Charset";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.accept-encoding
     * RFC9110 {@value "%s"}
     */
    public static final String ACCEPT_ENCODING = "Accept-Encoding";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.accept-language
     * RFC9110 {@value "%s"}
     */
    public static final String ACCEPT_LANGUAGE = "Accept-Language";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.authorization
     * RFC9110 {@value "%s"}
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.authentication-info
     * RFC9110 {@value "%s"}
     */
    public static final String AUTHENTICATION_INFO = "Authentication-Info";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.proxy-authentication-info
     * RFC9110 {@value "%s"}
     */
    public static final String PROXY_AUTHENTICATION_INFO =
        "Proxy-Authentication-Info";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.from RFC9110
     * {@value "%s"}
     */
    public static final String FROM = "From";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.host RFC9110
     * {@value "%s"} and :authority
     */
    public static final String HOST = "Host";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.if-unmodified-since
     * RFC9110 {@value "%s"}
     */
    public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.if-match
     * RFC9110 {@value "%s"}
     */
    public static final String IF_MATCH = "If-Match";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.if-none-match
     * RFC9110 {@value "%s"}
     */
    public static final String IF_NONE_MATCH = "If-None-Match";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.if-range
     * RFC9110 {@value "%s"}
     */
    public static final String IF_RANGE = "If-Range";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.if-modified-since
     * RFC9110 {@value "%s"}
     */
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.range
     * RFC9110 {@value "%s"}
     */
    public static final String RANGE = "Range";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.referer
     * RFC9110 {@value "%s"}
     */
    public static final String REFERER = "Referer";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.user-agent
     * RFC9110 {@value "%s"}
     */
    public static final String USER_AGENT = "User-Agent";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.te RFC9110
     * {@value "%s"}
     */
    public static final String TE = "TE";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.max-forwards
     * RFC9110 {@value "%s"}
     */
    public static final String MAX_FORWARDS = "Max-Forwards";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.proxy-authorization
     * RFC9110 {@value "%s"}
     */
    public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9111/#field.age RFC9111
     * {@value "%s"}
     */
    public static final String AGE = "Age";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.location
     * RFC9110 {@value "%s"}
     */
    public static final String LOCATION = "Location";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.retry-after
     * RFC9110 {@value "%s"}
     */
    public static final String RETRY_AFTER = "Retry-After";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.server
     * RFC9110 {@value "%s"}
     */
    public static final String SERVER = "Server";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.vary RFC9110
     * {@value "%s"}
     */
    public static final String VARY = "Vary";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     */
    public static final String WARNING = "Warning";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @spec https://www.rfc-editor.org/info/rfc9110/#field.www-authenticate
     * RFC9110 {@value "%s"}
     */
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @todo Verify anchor.
     * 
     * @spec https://www.rfc-editor.org/info/rfc9530/#field.content-digest
     * RFC9530 {@value "%s"}
     */
    public static final String CONTENT_DIGEST = "Content-Digest";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @todo Verify anchor.
     * 
     * @spec https://www.rfc-editor.org/info/rfc9530/#field.repr-digest
     * RFC9530 {@value "%s"}
     */
    public static final String REPR_DIGEST = "Repr-Digest";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @todo Verify anchor.
     * 
     * @spec https://www.rfc-editor.org/info/rfc9530/#field.want-content-digest
     * RFC9530 {@value "%s"}
     */
    public static final String WANT_CONTENT_DIGEST = "Want-Content-Digest";

    /**
     * The name of the standard HTTP field <samp>{@value "%s"}</samp>
     * 
     * @todo Verify anchor.
     * 
     * @spec https://www.rfc-editor.org/info/rfc9530/#field.want-repr-digest
     * RFC9530 {@value "%s"}
     */
    public static final String WANT_REPR_DIGEST = "Want-Repr-Digest";
}
