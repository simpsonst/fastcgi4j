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

package uk.ac.lancs.fastcgi.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import uk.ac.lancs.fastcgi.context.ResponderSession;
import uk.ac.lancs.http.ChunkedInputStream;
import uk.ac.lancs.http.cache.InboundCacheControl;
import uk.ac.lancs.http.encoding.Encoding;
import uk.ac.lancs.http.field.Cap;
import uk.ac.lancs.http.field.ExtensionManager;
import uk.ac.lancs.http.field.FieldExtension;
import uk.ac.lancs.http.field.FieldId;
import uk.ac.lancs.http.field.InputStreamCap;
import uk.ac.lancs.io.PrecedingInputStream;
import uk.ac.lancs.mime.MediaType;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Provides an HTTP-specific view of a responder session. This includes
 * transparently de-chunking the request body and reading the request
 * trailer, chunking the response body and writing the response trailer,
 * namespaced access to header/trailer fields, and so on.
 *
 * @author simpsons
 */
public class HttpResponderSession {
    /**
     * The FastCGI responder session on which this HTTP session is based
     */
    protected final ResponderSession base;

    /**
     * Re-usable context that can be shared between sessions
     */
    protected final HttpResponderContext ctxt;

    /**
     * Create an HTTP responder session from an unspecialized session.
     * 
     * @param base the base session
     * 
     * @param context re-usable context that can be shared between
     * sessions
     * 
     * @throws IllegalArgumentException if the base session is not HTTP
     */
    public HttpResponderSession(ResponderSession base,
                                HttpResponderContext ctxt) {
        /* TODO: Verify that the base session is compatible with
         * HTTP. */
        this.base = base;

        this.ctxt = ctxt;
    }

    /**
     * Test the request method.
     * 
     * @param meths the expected methods
     * 
     * @return {@code true} if the request method is one of those
     * provided; {@code false} otherwise
     */
    public boolean methodIs(CharSequence... meths) {
        String method = base.parameters().get("REQUEST_METHOD");
        for (var meth : meths)
            if (method.equals(meth)) return true;
        return false;
    }

    private List<String> tokens(CharSequence fieldName) {
        String field = base.parameters().get("HTTP_TRANSFER_ENCODING");
        if (field == null) return Collections.emptyList();
        List<String> result = new ArrayList<>(field.length() / 5);
        /* TODO: We should really use an *HTTP* tokenizer, not MIME.
         * However, how different are they really? */
        Tokenizer tokens = new Tokenizer(field);
        tokens.whitespace(0);
        for (;;) {
            var token = tokens.atom();
            if (token == null)
                /* TODO: Including remaining string in error message. */
                throw new IllegalArgumentException("no atom at ");
            result.add(token.toString());
            tokens.whitespace(0);
            if (tokens.end()) break;
            if (!tokens.character(','))
                /* TODO: Including remaining string in error message. */
                throw new IllegalArgumentException("no comma at ");
            tokens.whitespace(0);
        }
        return result;
    }

    private InputStream trailerIn = null;

    private List<String> rawRequestEncodings = null;

    private InputStream makeIn() throws IOException {
        InputStream in = base.in();

        /* Apply all transfer encodings. */
        List<String> transferEncodings = tokens("HTTP_TRANSFER_ENCODING");
        if (!transferEncodings.isEmpty()) {
            var sz = transferEncodings.size();

            /* Only the last element can be 'chunked'. Handle it
             * first. */
            var last = transferEncodings.remove(--sz);
            if (last.equalsIgnoreCase("chunked")) {
                var head = new PrecedingInputStream(in);
                trailerIn = head.tail();
                in = new ChunkedInputStream(head);
            }

            /* Handle all non-chunked encodings. */
            while (!transferEncodings.isEmpty()) {
                String name = transferEncodings.remove(--sz);
                Encoding enc = ctxt.decoders().get(name);
                if (enc == null)
                    /* TODO: Throw some specific exception that triggers
                     * a Bad Request response automatically. */
                    throw new IOException("unknown encoding " + name);
                in = enc.decode(in);
            }
        }

        /* Apply unhandled content decoding. */
        if (rawRequestEncodings == null)
            rawRequestEncodings = tokens("HTTP_CONTENT_ENCODIING");
        unhandledRequestEncodings =
            getApplicationHandledEncodings(rawRequestEncodings);
        int sz = rawRequestEncodings.size();
        while (!rawRequestEncodings.isEmpty()) {
            String name = rawRequestEncodings.remove(--sz);
            Encoding enc = ctxt.decoders().get(name);
            if (enc == null) {
                /* Move the unhandled encodings to those the application
                 * is expected to deal with. */
                unhandledRequestEncodings.addAll(rawRequestEncodings);
                rawRequestEncodings.clear();
                unhandledRequestEncodings.add(name);
                break;
            }
            in = enc.decode(in);
        }

        return in;
    }

    /**
     * Get the stream for the request body. This excludes the trailer,
     * which is present in the underlying stream if the body is chunked.
     * Closing this stream discards any remaining request body, and
     * causes the trailer to be parsed. {@link #requestTrailer()} then
     * becomes available.
     * 
     * <p>
     * As specified by the <code>Content-Encoding</code> field in the
     * request header, all content encodings are removed, unless the
     * implementation can't handle one, or if the application has
     * indicated that it can cope with the only remaining encodings by
     * previously calling {@link #acceptEncodings(CharSequence...)} or
     * {@link #acceptEncodings(Collection)}.
     * 
     * <p>
     * All transfer encodings are also removed, including chunking.
     * 
     * @return the request body (including an empty stream if there is
     * no body)
     * 
     * @throws IOException if there was an error in decoding the input
     * stream
     */
    public InputStream in() throws IOException {
        if (in == null) in = makeIn();
        return in;
    }

    private InputStream in = null;

    private MediaType requestType = null;

    /**
     * Get the content type of the request body. If a request body is
     * expected, but not request content type has been specified,
     * <samp>application/octet-stream</samp> is returned.
     * 
     * @return the request body's content type; or {@code null} if there
     * is no body
     */
    public MediaType requestType() {
        if (methodIs("GET", "HEAD")) return null;
        if (requestType == null) {
            String field = base.parameters().get("CONTENT_TYPE");
            requestType =
                field == null ? MediaType.of("application", "octet-stream") :
                    MediaType.fromString(field);
        }
        return requestType;
    }

    /**
     * Determine whether the client is using a minimum version of HTTP.
     * 
     * @param v the minimum required version
     * 
     * @return {@code true} if the client is using at least the
     * specified version; {@code false} otherwise
     */
    public boolean minimumVersion(HttpVersion v) {
        /* TODO */
        throw new UnsupportedOperationException("unimplemented");
    }

    private final Collection<String> acceptedEncodings =
        new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private List<String> unhandledRequestEncodings = null;

    /**
     * Get the sequence of encodings required to decode the request. The
     * first entry was applied first.
     * 
     * <p>
     * This method takes into account the application's calls to
     * {@link #acceptEncodings(CharSequence...)} and
     * {@link #acceptEncodings(Collection)}. It also makes no internal
     * changes to the session state, and can be called before or after
     * {@link #in()}.
     * 
     * @return a mutable sequence of encodings required to decode the
     * request stream, if {@link #in()} were about to be called for the
     * first time
     */
    public List<String> requestEncodings() {
        if (unhandledRequestEncodings != null)
            return new ArrayList<>(unhandledRequestEncodings);
        if (rawRequestEncodings == null)
            rawRequestEncodings = tokens("HTTP_CONTENT_ENCODIING");
        List<String> contentEncodings = new ArrayList<>(rawRequestEncodings);
        return getApplicationHandledEncodings(contentEncodings);
    }

    /**
     * Remove from the list of encodings a head of those handled by the
     * application.
     * 
     * @param encodings the list to be modified
     * 
     * @return a mutable list of the encodings that form the head of the
     * original list, and which the application is prepared to deal with
     * itself
     */
    private List<String>
        getApplicationHandledEncodings(List<String> encodings) {
        List<String> unhandled = new ArrayList<>();
        for (var iter = encodings.iterator(); iter.hasNext();) {
            var name = iter.next();
            if (acceptedEncodings.contains(name)) {
                unhandled.add(name);
                iter.remove();
                continue;
            }
            break;
        }
        return unhandled;
    }

    /**
     * Indicate that the application can deal with some content
     * encodings.
     * 
     * @param names the names of content encodings that can be left on
     * the request body when presented as a stream
     * 
     * @see #acceptEncodings(CharSequence...)
     * 
     * @throws IllegalStateException if the request stream has already
     * been opened with {@link #in()}
     */
    public void acceptEncodings(Collection<? extends CharSequence> names) {
        /* If the request stream has already been opened, throw an
         * exception. */
        if (in != null)
            throw new IllegalStateException("request already opened");
        for (var e : names)
            acceptedEncodings.add(e.toString());
    }

    /**
     * Indicate that the application can deal with some content
     * encodings.
     * 
     * @param names the names of content encodings that can be left on
     * the request body when presented as a stream
     * 
     * @see #acceptEncodings(Collection)
     * 
     * @throws IllegalStateException if the request stream has already
     * been opened with {@link #in()}
     */
    public void acceptEncodings(CharSequence... names) {
        /* If the request stream has already been opened, throw an
         * exception. */
        if (in != null)
            throw new IllegalStateException("request already opened");
        for (var e : names)
            acceptedEncodings.add(e.toString());
    }

    /**
     * Get the request body's length.
     * 
     * @return the request body's length in bytes; zero if there is no
     * body; negative if the length is unknown
     */
    public long requestLength() {
        /* TODO */
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Access the request header fields. Field values are obtained by
     * transforming the field name into an environment variable.
     *
     * <p>
     * To extract a field from the environment, a field name is
     * converted to upper case, hyphens are replaced with underscores,
     * and then <samp>HTTP_</samp> is prefixed. An additional namespace
     * identifier may be prefixed. The transformed name is then looked
     * up in the FastCGI environment.
     * 
     * @return access to the request header fields
     */
    public Cap requestHeader() {
        if (requestHeader == null) makeRequestHeader();
        return requestHeader;
    }

    private void makeRequestHeader() {
        assert requestHeader == null;
        /* TODO: Parse Cache-Control (or Pragma) fields. */
        /* TODO */
        throw new UnsupportedOperationException("unimplemented");
    }

    private final ExtensionManager requestExtMgr = new ExtensionManager();

    /**
     * Get the extension manager for the request.
     * 
     * @return the request extension manager
     */
    public ExtensionManager requestExtensions() {
        return requestExtMgr;
    }

    private Cap requestHeader = null;

    /**
     * Get the extension manager for the response.
     * 
     * @return the response extension manager
     */
    private final ExtensionManager responseExtMgr = new ExtensionManager();

    public ExtensionManager responseExtensions() {
        return responseExtMgr;
    }

    private Cap requestTrailer = null;

    /**
     * Access the request trailer fields. This is not available until
     * after the request body stream provided by {@link #in()} has been
     * closed.
     * 
     * <p>
     * To extract a field from the trailer, the body must first have
     * been read in with {@link Session#requestBody()}, and the stream
     * closed. Otherwise, an {@link IllegalStateException} may be
     * thrown.
     * 
     * <p>
     * Sought fields not present in the trailer are then sought in the
     * header automatically. For example, if the client has sent a
     * <code>Repr-Digest</code> field, it might only be added in the
     * trailer; requesting it via this method will find it, whether it
     * was provided before or after the body.
     * 
     * @return access to the request trailer fields
     * 
     * @throws IllegalStateException if the request body stream has not
     * been closed
     */
    public Cap requestTrailer() throws IOException {
        /* Provide the one already created, if it exists. */
        if (requestTrailer != null) return requestTrailer;

        if (trailerIn == null) {
            /* There's no stream to read from. If we haven't determined
             * the body stream, the application is accessing the trailer
             * too soon. */
            if (in == null) throw new IllegalStateException("request trailer"
                + " requested before body");
            /* We have the body stream, but no trailer stream. Just
             * default to the request header. */
            requestTrailer = requestHeader();
            return requestTrailer;
        }

        /* We must process the header first, to find out what's in the
         * trailer, and get any namespace definitions. That is, the
         * expectations are populated. */
        var hdr = requestHeader();
        requestTrailer = new InputStreamCap(requestExtMgr, in,
                                            requestTrailerExpectations, hdr);
        return requestTrailer;
    }

    /**
     * Holds a mapping from case-insensitive field name to field id.
     */
    private final Map<String, FieldId> requestTrailerExpectations =
        new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Obtain the modifiable field header. Modifications can be made
     * until output is sent to the response body stream {@link #out()}.
     * 
     * @return the field header
     */
    public Cap responseHeader() {
        /* TODO */
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Derive a digest from the submitted content after applying content
     * encoding, and include it as a <code>Content-Digest</code> trailer
     * field.
     * 
     * @throws IllegalStateException if the response header has already
     * been sent; if the client does not support response trailers
     */
    public void includeContentDigest(MessageDigest digest,
                                     Function<byte[], String> formatter) {
        /* TODO */
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Derive a digest from the submitted content before applying
     * content encoding, and include it as a <code>Repr-Digest</code>
     * trailer field.
     * 
     * @throws IllegalStateException if the response header has already
     * been sent; if the client does not support response trailers
     */
    public void
        includeRepresentationDigest(MessageDigest digest,
                                    Function<byte[], String> formatter) {
        /* TODO */
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Indicate that some fields are expected in the trailer. This
     * method may be called multiple times, but only until output is
     * sent to the response body stream {@link #out()}.
     * 
     * @param ids the ids to add to the expected set
     * 
     * @throws IllegalStateException if used after output has been sent
     * to the response body stream
     */
    public void expectTrailer(FieldId... ids) {
        /* TODO */
        throw new UnsupportedOperationException("unimplemented");
    };

    private OutputStream makeOut() {
        /* TODO */
        throw new UnsupportedOperationException("unimplemented");
    }

    private final List<String> outgoingEncodings = new ArrayList<>(4);

    /**
     * Set the content encodings that the application itself is applying
     * to the response body. These are assumed to have been applied to
     * any data written to the response stream {@link #out()}. The last
     * name indicates the most recent transformation.
     * 
     * <p>
     * This call replaces any previous setting. Encodings are not
     * accumulated.
     * 
     * @param names the sequence of encodings already applied to the
     * response body by the application
     * 
     * @see #setEncodings(CharSequence...)
     * 
     * @throws IllegalStateException if the response stream has already
     * been obtained by {@link #out()}
     */
    public void setEncodings(List<? extends CharSequence> names) {
        outgoingEncodings.clear();
        for (var n : names)
            outgoingEncodings.add(n.toString().toLowerCase());
    }

    /**
     * Set the content encodings that the application itself is applying
     * to the response body. These are assumed to have been applied to
     * any data written to the response stream {@link #out()}. The last
     * name indicates the most recent transformation.
     * 
     * <p>
     * This call replaces any previous setting. Encodings are not
     * accumulated.
     * 
     * @param names the sequence of encodings already applied to the
     * response body by the application
     * 
     * @see #setEncodings(List)
     * 
     * @throws IllegalStateException if the response stream has already
     * been obtained by {@link #out()}
     */
    public void setEncodings(CharSequence... names) {
        outgoingEncodings.clear();
        for (var n : names)
            outgoingEncodings.add(n.toString().toLowerCase());
    }

    /**
     * Get the stream for the response body. If the user has called
     * {@link #expectTrailer(FieldId...), the body will be transparently
     * chunked.
     * 
     * @return the output stream for writing an unchunked response body
     */
    public OutputStream out() {
        if (out == null) out = makeOut();
        return out;
    }

    private OutputStream out = null;

    /**
     * Activate and obtain the modifiable field trailer. Fields added to
     * the trailer must pass {@link FieldId#forResponseTrailer()}.
     * Modifications can be made until the response body stream
     * {@link #out()} has been closed.
     * 
     * @return the field trailer
     * 
     * @throws IllegalStateException if not activated before the
     * response header is sent; or if the response body stream has been
     * closed
     */
    public Cap responseTrailer() {
        /* TODO */
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Get details for an extension used in the request.
     * 
     * @param ns the identifying namespace URI
     * 
     * @return the extension details; or {@code null} if the extension
     * was not used in the request
     */
    public FieldExtension requestExtension(URI ns) {
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Configure an extension with a named property.
     * 
     * @param nsuri the namespace URI of the extension
     * 
     * @param name the property name
     * 
     * @param value the property value
     * 
     * @throws IllegalArgumentException if the property name is not an
     * HTTP token
     */
    public void requestExtensionProperty(URI nsuri, String name, String value) {
        Objects.requireNonNull(nsuri, "ns");
        Objects.requireNonNull(nsuri, "name");
        name = name.trim();
        /* TODO */
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Get the prefix for a request field extension.
     * 
     * @param nsuri the namespace URI of the extension
     * 
     * @return the defined prefix; or {@code null} if not defined
     */
    public String requestExtensionPrefix(URI nsuri) {
        /* TODO */
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Get a parameter of a field extension.
     * 
     * @param nsuri the namespace URI of the extension
     * 
     * @param name the property name
     * 
     * @return the property value; or {@code null} if not defined
     */
    public String requestExtensionProperty(URI nsuri, String name) {
        /* TODO */
        throw new UnsupportedOperationException("unimplemented");
    }

    private InboundCacheControl requestCacheControl = null;

    /**
     * Get <code>Cache-Control</code> directives specified by the
     * client.
     * 
     * @return the cache-control directives for the request
     */
    public InboundCacheControl requestCacheControl() {
        requestHeader();
        assert requestCacheControl != null;
        return requestCacheControl;
    }
}
