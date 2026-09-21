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

package uk.ac.lancs.fastcgi.augment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.ac.lancs.cgi.CGIParameters;
import uk.ac.lancs.cgi.Http;
import uk.ac.lancs.cgi.ServerProtocol;
import uk.ac.lancs.fastcgi.RequestableSession;
import uk.ac.lancs.fastcgi.ResponderSession;
import uk.ac.lancs.fastcgi.Session;
import uk.ac.lancs.http.cache.InboundCacheControl;
import uk.ac.lancs.http.encoding.BodyDecoder;
import uk.ac.lancs.http.encoding.EncodingContext;
import uk.ac.lancs.http.encoding.InputEncoding;
import uk.ac.lancs.http.field.CGIRequestCap;
import uk.ac.lancs.http.field.Cap;
import uk.ac.lancs.http.field.EmptyCap;
import uk.ac.lancs.http.field.ExtensionManager;
import uk.ac.lancs.http.field.FieldExtension;
import uk.ac.lancs.http.field.FieldId;
import uk.ac.lancs.http.field.FieldNameSets;
import uk.ac.lancs.http.field.FieldNames;
import uk.ac.lancs.http.field.FieldNamespace;
import uk.ac.lancs.mime.MediaType;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Provides an HTTP-specific view of a responder session. This includes
 * transparently de-chunking the request body and reading the request
 * trailer, chunking the response body and writing the response trailer,
 * namespaced access to header/trailer fields, and so on.
 * 
 * <p>
 * <strong>This class is incomplete!</strong>
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
     * Identifies the protocol used by the client to talk to the server.
     * This is taken from the CGI parameter <samp>{@value "%s"
     * CGIParameters#SERVER_PROTOCOL_PARAM}</samp>.
     */
    protected final ServerProtocol protocol;

    /**
     * Create an HTTP responder session from an unspecialized session.
     * 
     * @param base the base session
     * 
     * @param ctxt re-usable context that can be shared between sessions
     * 
     * @throws IllegalArgumentException if the base session is not HTTP
     */
    public HttpResponderSession(ResponderSession base,
                                HttpResponderContext ctxt) {
        /* Verify that the base session is compatible with HTTP. */
        var srvProto = ServerProtocol.ofOptional(base.parameters());
        if (srvProto == null)
            throw new IllegalArgumentException("server protocol is not set");
        if (!srvProto.isMinimally("HTTP", 0, 9))
            throw new IllegalArgumentException(srvProto + " is not HTTP");
        protocol = srvProto;

        this.base = base;
        this.ctxt = ctxt;
        this.requestHeader =
            new CGIRequestCap(requestExtMgr, base.parameters());
    }

    /**
     * Test whether the method is one of several specified.
     * 
     * @param methods the set of expected methods
     * 
     * @return {@code true} if the method is among those listed;
     * {@code false} otherwise
     * 
     * @throws IllegalStateException if the request method is not set
     */
    public boolean methodIs(CharSequence... methods) {
        String method = method();
        for (var meth : methods)
            if (method.equals(meth)) return true;
        return false;
    }

    /**
     * Get the request method. The value of the CGI parameter
     * <samp>{@value "%s" CGIParameters#METHOD_PARAM}</samp> is
     * returned.
     * 
     * @return the request method
     * 
     * @throws IllegalStateException if the request method is not set
     */
    public String method() {
        var r = base.parameters().get(CGIParameters.METHOD_PARAM);
        if (r == null) throw new IllegalStateException("CGI parameter not set: "
            + CGIParameters.METHOD_PARAM);
        return r;
    }

    /**
     * Caches the parsed value of the <samp>{@value "%S"
     * FieldNames#TE}</samp> HTTP/1.1 request header field. Call
     * {@link #getAcceptedTransferEncodings()} to populate it lazily.
     */
    private Map<String, Map<String, String>> acceptedTransferEncodings = null;

    private static final String TE_PARAM = Http.fieldNameAsCGI(FieldNames.TE);

    /**
     * Parse the <samp>{@value "%s" #TE_FIELD}</samp> request header
     * field as a comma-separated sequence of tokens with optional
     * parameters. The field is obtained through the CGI parameter. The
     * result is stored in {@link #acceptedTransferEncodings} if it is
     * currently {@code null}, so only the first call actually does
     * anything.
     * 
     * <p>
     * For HTTP/2 and later, the field is ignored if it doesn't contain
     * the sole token <samp>{@value "%s" #TRAILERS_TOKEN}</samp>.
     */
    private void getAcceptedTransferEncodings() {
        if (acceptedTransferEncodings != null) return;
        var txt = base.parameters().get(TE_PARAM);
        Map<String, Map<String, String>> result = new HashMap<>();
        if (txt != null) {
            var toks = new Tokenizer(txt);
            CharSequence name;
            Map<String, String> params = new HashMap<>();
            while (toks.whitespace(0) &&
                (name = toks.atomParameters(params, Tokenizer.PARAMS_CLEAR))
                    != null) {
                result.put(name.toString(), Map.copyOf(params));
                if (!toks.whitespaceCharacter(0, ',')) break;
            }
        }
        if (protocol.isMinimally("HTTP", 2, 0) &&
            (!result.containsKey(TRAILERS_TOKEN) || result.size() == 1)) {
            /* HTTP/2 requires that this field only contain this single
             * token, or not be present. */
            acceptedTransferEncodings = Collections.emptyMap();
        } else {
            acceptedTransferEncodings = Map.copyOf(result);
        }
    }

    /**
     * Specifies the token <samp>{@value "%s"}</samp> to appear in the
     * <samp>{@value "%s" #TE_FIELD}</samp> header field to indicate
     * that the client accepts a response trailer.
     */
    private static final String TRAILERS_TOKEN = "trailers";

    /**
     * Determine whether a response trailer can be sent. A trailer is
     * permitted when HTTP/2.0 or later is used, or when the request
     * header field <samp>{@value "%s" FieldNames#TE}</samp> includes
     * the token <samp>{@value "%s" #TRAILERS_TOKEN}</samp>.
     * 
     * @return {@code true} if a trailer can be sent; {@code false}
     * otherwise
     */
    public boolean responseTrailerAllowed() {
        /* For HTTP/2, trailers are always permitted. */
        if (protocol.isMinimally("HTTP", 2, 0)) return true;

        /* For earlier versions, look for a "trailers" token in the "TE"
         * header field. */
        getAcceptedTransferEncodings();
        return acceptedTransferEncodings.containsKey(TRAILERS_TOKEN);
    }

    /**
     * Parse a CGI parameter as HTTP comma-separated tokens. If the
     * parameter is not set, an empty list is returned.
     * 
     * @param paramName the name of the request parameter
     * 
     * @return a list of tokens from the parameter
     * 
     * @throws IllegalArgumentException if the parameter is present, but
     * does not parse as comma-separated tokens
     */
    private List<String> tokens(String paramName) {
        String field = base.parameters().get(paramName);
        if (field == null) return Collections.emptyList();
        List<String> result = new ArrayList<>(field.length() / 5);
        /* TODO: We should really use an *HTTP* tokenizer, not MIME.
         * However, how different are they really? */
        Tokenizer tokens = new Tokenizer(field);
        tokens.whitespace(0);
        for (;;) {
            var token = tokens.atom();
            if (token == null) throw new IllegalArgumentException("no atom at "
                + tokens.remnant());
            result.add(token.toString());
            tokens.whitespace(0);
            if (tokens.end()) break;
            if (!tokens.character(','))
                throw new IllegalArgumentException("no comma at "
                    + tokens.remnant());
            tokens.whitespace(0);
        }
        return result;
    }

    static final String ENCODINGS_PREFIX = "uk.ac.lancs.fastcgi.encodings.";

    static final Map<String, InputEncoding> ALL_AVAILABLE_TRANSFER_DECODERS =
        Map.copyOf(InputEncoding.getMapping(EncodingContext.TRANSFER,
                                            System.getProperties(),
                                            ENCODINGS_PREFIX));

    private static final String CHUNKED_TOKEN = "chunked";

    private static final String TRANSFER_ENCODING_PARAM =
        Http.fieldNameAsCGI(FieldNames.TRANSFER_ENCODING);

    private InputStream makeIn() throws IOException {
        InputStream in = base.in();

        /* Apply all transfer encodings. */
        List<String> transferEncodings = tokens(TRANSFER_ENCODING_PARAM);
        if (!transferEncodings.isEmpty()) {
            var sz = transferEncodings.size();

            /* Only the last element can be 'chunked'. Handle it
             * first. */
            var last = transferEncodings.get(sz);
            if (last.equalsIgnoreCase(CHUNKED_TOKEN)) {
                transferEncodings.remove(--sz);

                /**
                 * Bizarrely, nginx and Apache decode the chunking
                 * themselves, but leave the 'chunked' token on the end
                 * of the Transfer-Encoding field. The trailer itself
                 * isn't even presented, so this is all pointless.
                 * Here's what we would have done:
                 * 
                 * <pre>
                 * var head = new PrecedingInputStream(in);
                 * trailerIn = head.tail();
                 * in = new ChunkedInputStream(head);
                 * </pre>
                 */
            }

            /* Using the provided transfer decoders, clear all the
             * transfer encodings. If we can't clear them all, it's an
             * error. */
            BodyDecoder xferDecoder =
                new BodyDecoder(ctxt.transferDecoders()::get);
            in = xferDecoder.decode(in, transferEncodings);
            if (!transferEncodings.isEmpty())
                throw new IOException("unknown transfer encoding "
                    + transferEncodings);
        }

        /* Apply unhandled content decoding, according to what the
         * application wants. */
        requestEncodings();
        in = InputEncoding.decode(in, handledRequestEncodings);

        return in;
    }

    /**
     * Get the stream for the request body. This excludes the trailer,
     * which is often hidden by the server, and so is not available to
     * any FastCGI/1.0 application. (This library proposes an extension
     * allowing the trailer to be passed separately.)
     * 
     * <p>
     * As specified by the <samp>{@value "%s"
     * FieldNames#CONTENT_ENCODING}</samp> field in the request header,
     * and by {@link HttpResponderContext#contentDecoders()} on the
     * configured context, recognized trailing content encodings are
     * removed. Any remaining encodings are provided by
     * {@link #requestEncodings()}.
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
     * Get the content type of the request body. The parameter
     * <samp>{@value "%s" CGIParameters#REQUEST_TYPE_PARAM}</samp> is
     * consulted. If a request body is expected, but no request content
     * type has been specified, <samp>application/octet-stream</samp> is
     * returned.
     * 
     * @return the request body's content type; or {@code null} if there
     * is no body
     */
    public MediaType requestType() {
        if (methodIs("GET", "HEAD")) return null;
        if (requestType == null) {
            String field =
                base.parameters().get(CGIParameters.REQUEST_TYPE_PARAM);
            requestType =
                field == null ? MediaType.of("application", "octet-stream") :
                    MediaType.fromString(field);
        }
        return requestType;
    }

    /**
     * Determine whether the client is using a minimum version of HTTP.
     * Note that if the session is part of an internal composition, the
     * version will appear to be 1.0.
     * 
     * @param major the minimum required major version
     * 
     * @param minor the minimum required minor version
     * 
     * @return {@code true} if the client is using at least the
     * specified version; {@code false} otherwise
     */
    public boolean minimumVersion(int major, int minor) {
        if (protocol.major() < major) return false;
        if (protocol.major() == major && protocol.minor() < minor) return false;
        return true;
    }

    /**
     * Determine whether this session is part of an internal
     * composition.
     * 
     * @return {@code true} if the session is included as part of an
     * internal composition; {@code false} otherwise
     */
    public boolean isIncluded() {
        return protocol.isIncluded();
    }

    private List<String> rawRequestEncodings = null;

    private List<InputEncoding> handledRequestEncodings = null;

    private static final String CONTENT_ENCODING_PARAM =
        Http.fieldNameAsCGI(FieldNames.CONTENT_ENCODING);

    /**
     * Get the sequence of content encodings required to decode the
     * request. This is obtained by parsing the HTTP request field
     * <samp>{@value "%s" FieldNames#CONTENT_ENCODING}</samp> as
     * comma-separated tokens. The first entry was applied first to
     * request body.
     * 
     * @return a mutable sequence of encodings required to decode the
     * request stream returned by {@link #in()}
     */
    public List<String> requestEncodings() {
        if (rawRequestEncodings == null) {
            assert handledRequestEncodings == null;
            rawRequestEncodings = tokens(CONTENT_ENCODING_PARAM);
            BodyDecoder contentDecoder =
                new BodyDecoder(ctxt.contentDecoders()::get);
            handledRequestEncodings =
                contentDecoder.recognize(rawRequestEncodings);
        }
        return rawRequestEncodings;
    }

    /**
     * Holds the cached request length. A value of {@code -1} means that
     * the request length is unknown. Other non-negative values indicate
     * the actual request length. Other negative values mean that no
     * value has been cached (the default). The method
     * {@link #requestLength()} consults and sets this field.
     */
    private long requestLength = -2;

    /**
     * Get the request body's length. The parameter <samp>{@value "%s"
     * CGIParameters#REQUEST_LENGTH_PARAM}</samp> is read as a decimal
     * integer. If not present, or an empty string, the length is deemed
     * unknown.
     * 
     * <p>
     * The result is cached.
     * 
     * @return the request body's length in bytes: zero if there is no
     * body; negative if the length is unknown
     * 
     * @throws IllegalStateException if the request body length has been
     * set to an invalid value
     */
    public long requestLength() {
        if (requestLength >= -1) return requestLength;
        String text = base.parameters().get(CGIParameters.REQUEST_LENGTH_PARAM);
        if (text == null || text.isEmpty()) return -1L;
        try {
            long r = Long.parseLong(text, 10);
            if (r >= 0) return requestLength = r;
        } catch (NumberFormatException ex) {
            // Fall through.
        }
        throw new IllegalStateException("invalid CGI parameter "
            + CGIParameters.REQUEST_LENGTH_PARAM + ": " + text);
    }

    /**
     * Access the request header fields. Field values are obtained by
     * transforming the field name into an environment variable.
     *
     * <p>
     * To extract a field from the environment, a field name is
     * converted to upper case, hyphens are replaced with underscores,
     * and optional namespace identifier may be prefixed, and then
     * <samp>HTTP_</samp> is prefixed. The transformed name is then
     * looked up in the FastCGI environment.
     * 
     * @return access to the request header fields
     */
    public Cap requestHeader() {
        return requestHeader;
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

    private final Cap requestHeader;

    /**
     * Get the extension manager for the response.
     * 
     * @return the response extension manager
     */
    private final ExtensionManager responseExtMgr = new ExtensionManager();

    /**
     * Get the manager for extensions used in the response header and
     * trailer.
     * 
     * @return the requested extension manager
     */
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
     * been read in with {@link RequestableSession#in()}, and the stream
     * closed. Otherwise, an {@link IllegalStateException} may be
     * thrown.
     * 
     * @return access to the request trailer fields
     * 
     * @throws IllegalStateException if the request body stream has not
     * been closed
     * 
     * @throws IOException if an I/O error occurs in reading the trailer
     * 
     * @todo This can't work without extensions to FastCGI, or
     * modifications to popular servers that would change existing
     * behaviour. Until then, an empty trailer is returned.
     */
    public Cap requestTrailer() throws IOException {
        /* Provide the one already created, if it exists. */
        if (requestTrailer != null) return requestTrailer;

        /* TODO: This would require a second PARAMS sequence to appear
         * after the STDIN sequence, and a flag (perhaps in
         * BEGIN_REQUEST) to indicate that such a sequence is to be
         * expected, and a capability in GET_VALUES(_RESULT) to indicate
         * that the application could handle it. */

        return EmptyCap.INSTANCE;
    }

    private final Map<FieldId, List<String>> responseHeaderFields =
        new HashMap<>();

    private final Collection<FieldId> responseTrailerExpectation =
        new HashSet<>();

    private final Map<FieldId, List<String>> responseTrailerFields =
        new HashMap<>();

    private static final Set<FieldId> FORBIDDEN_RESPONSE_FIELDS = Set
        .of(Session.STATUS_FIELD, FieldNames.CONNECTION,
            FieldNames.TRANSFER_ENCODING, FieldNames.TRAILER)
        .stream()
        .flatMap(s -> Stream.of(FieldNamespace.STANDARD_END_TO_END.of(s)))
        .collect(Collectors.toSet());

    private final Cap responseHeader = new Cap() {
        @Override
        public List<String> get(FieldId id) {
            /* Check for fields that we manage, and throw
             * IllegalArgumentException. */
            if (FORBIDDEN_RESPONSE_FIELDS.contains(id))
                throw new IllegalArgumentException("managed field: " + id);

            /* TODO: When it's too late to change any header fields,
             * return an immutable list. */

            return responseHeaderFields.computeIfAbsent(id,
                                                        k -> new ArrayList<>());
        }
    };

    private final Cap responseTrailer = new Cap() {
        @Override
        public List<String> get(FieldId id) {
            /* Check for fields that have not been declared before the
             * header has been written throw IllegalStateException. */
            if (!responseTrailerExpectation.contains(id))
                throw new IllegalStateException("unexpected trailer field: "
                    + id);

            /* TODO: When it's too late to change any trailer fields,
             * return an immutable list. */

            return responseTrailerFields
                .computeIfAbsent(id, k -> new ArrayList<>());
        }
    };

    /**
     * Obtain the modifiable field header. Modifications can be made
     * until output is sent to the response body stream {@link #out()}.
     * 
     * @return the field header
     */
    public Cap responseHeader() {
        return responseHeader;
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
     * Identifies standard fields which cannot be set in the trailer.
     * Note that each field name is included twice, once as end-to-end
     * and once as hop-by-hop.
     */
    private static final Set<FieldId> FORBIDDEN_TRAILER_FIELDS =
        FieldNameSets.HEADER.stream()
            .flatMap(s -> Stream.of(FieldNamespace.STANDARD_END_TO_END.of(s),
                                    FieldNamespace.STANDARD_HOP_BY_HOP.of(s)))
            .collect(Collectors.toSet());

    /**
     * Indicate that some fields are expected in the trailer. This
     * method may be called multiple times, but only until output is
     * sent to the response body stream {@link #out()}.
     * 
     * @param ids the ids to add to the expected set
     * 
     * @throws IllegalStateException if used after output has been sent
     * to the response body stream
     * 
     * @throws IllegalArgumentException if a field is not permitted in
     * the trailer
     */
    public void expectTrailer(FieldId... ids) {
        /* TODO: If the header has been sent, throw
         * IllegalStateException. */

        /* Fail if any id is not permitted in the trailer. */
        for (FieldId id : ids) {
            if (FieldId.hasIllegalScope(id))
                throw new IllegalArgumentException("field with bad scope: "
                    + id);
            if (FORBIDDEN_TRAILER_FIELDS.contains(id))
                throw new IllegalArgumentException("bad trailer field: " + id);
        }

        responseTrailerExpectation.addAll(Arrays.asList(ids));
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
     * Activate and obtain the modifiable field trailer. Modifications
     * can be made until the response body stream {@link #out()} has
     * been closed.
     * 
     * @return the field trailer
     * 
     * @throws IllegalStateException if not activated before the
     * response header is sent; or if the response body stream has been
     * closed
     */
    public Cap responseTrailer() {
        return responseTrailer;
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
