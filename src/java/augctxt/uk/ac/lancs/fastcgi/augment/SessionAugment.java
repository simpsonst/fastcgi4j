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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import uk.ac.lancs.cgi.Http;
import uk.ac.lancs.fastcgi.Session;
import uk.ac.lancs.http.Negotiation;
import uk.ac.lancs.http.ResponseCodes;
import uk.ac.lancs.http.encoding.EncodingContext;
import uk.ac.lancs.http.encoding.OutputEncoding;
import uk.ac.lancs.http.encoding.ResponseEncoder;
import uk.ac.lancs.mime.MediaGroup;
import uk.ac.lancs.mime.MediaType;

/**
 * Provides an augmented session context.
 *
 * @author simpsons
 * 
 * @deprecated Use {@link HttpResponderSession} instead, even though
 * it's not ready.
 */
@Deprecated
public final class SessionAugment {
    private final Session session;

    private static final String ACCEPT_ENCODING_FIELD = "Accept-Encoding";

    private static final String ACCEPT_ENCODING_VAR =
        Http.fieldNameAsCGI(ACCEPT_ENCODING_FIELD);

    private static final String TE_VAR = Http.fieldNameAsCGI("TE");

    /**
     * Get the client's encoding preference. This is extracted from the
     * {@value #ACCEPT_ENCODING_FIELD} request header field if present.
     * {@link Negotiation#getAtomPreference(CharSequence)} is used to
     * parse the field value.
     * 
     * @return an immutable map of encodings to quality parameters
     */
    public Map<String, Float> getContentEncodingPreference() {
        return getEncodingPreference(ACCEPT_ENCODING_VAR);
    }

    private Map<String, Float> getEncodingPreference(String varName) {
        return Negotiation.getAtomPreference(session.parameters().get(varName));
    }

    private static final String ACCEPT_FIELD = "Accept";

    private static final String ACCEPT_VAR = Http.fieldNameAsCGI(ACCEPT_FIELD);

    /**
     * Get the client's media-type preferences. This is extracted from
     * the {@value #ACCEPT_FIELD} request header field if present.
     * {@link Negotiation#getMediaTypePreference(CharSequence)} is used
     * to parse the field value.
     * 
     * @return an immutable of the client's media-type preferences
     */
    public Map<MediaGroup, Float> getMediaTypePreference() {
        return Negotiation
            .getMediaTypePreference(session.parameters().get(ACCEPT_VAR));
    }

    /**
     * Set a response header field to a comma-separated list of tokens.
     * 
     * @param field the HTTP response field name
     * 
     * @param tokens the tokens to be listed
     */
    private void setEncoding(String field,
                             List<? extends CharSequence> tokens) {
        session.setField(field,
                         tokens.stream().collect(Collectors.joining(", ")));
    }

    /**
     * Create an augmented session context.
     * 
     * @param session the basic session
     */
    public SessionAugment(Session session) {
        this.session = session;
        this.responseEncoder =
            new ResponseEncoder(contentEncodingOffer, transferEncodingOffer,
                                responseEncoderContext);
    }

    private static Map<String, Map.Entry<OutputEncoding, Number>>
        getEncodingOffer(EncodingContext ec, String pfx) {
        return OutputEncoding.getMapping(ec, System.getProperties(),
                                         "uk.ac.lancs.fastcgi." + pfx);
    }

    private static final Map<String,
                             Map.Entry<OutputEncoding,
                                       Number>> contentEncodingOffer =
                                           getEncodingOffer(EncodingContext.CONTENT,
                                                            "content.");

    private static final Map<String,
                             Map.Entry<OutputEncoding,
                                       Number>> transferEncodingOffer =
                                           getEncodingOffer(EncodingContext.TRANSFER,
                                                            "transfer.");

    private static final String CONTENT_ENCODING_VAR = "Content-Encoding";

    private static final String TRANSFER_ENCODING_VAR = "Transfer-Encoding";

    private final ResponseEncoder.Context responseEncoderContext =
        new ResponseEncoder.Context() {
            @Override
            public OutputStream raw() {
                return session.out();
            }

            @Override
            public Map<? extends String, ? extends Number> contentPreference() {
                return getContentEncodingPreference();
            }

            @Override
            public Map<? extends String, ? extends Number>
                transferPreference() {
                return getEncodingPreference(TE_VAR);
            }

            @Override
            public void setContentEncoding(List<? extends CharSequence> names) {
                setEncoding(CONTENT_ENCODING_VAR, names);
            }

            @Override
            public void
                setTransferEncoding(List<? extends CharSequence> names) {
                setEncoding(TRANSFER_ENCODING_VAR, names);
            }
        };

    private final ResponseEncoder responseEncoder;

    /**
     * Get the encoder for the response body.
     * 
     * @return the response body encoder
     */
    public ResponseEncoder responseEncoder() {
        return responseEncoder;
    }

    /**
     * Get the output stream with encodings applied. On the first call,
     * encodings specified by other calls are applied to the basic
     * session's stream, and the {@value #CONTENT_ENCODING_VAR} and
     * {@value #TRANSFER_ENCODING_VAR} header fields are set. Subsequent
     * calls will yield the same stream. Calling this method prevents
     * the calling of other methods that modify encoding.
     * 
     * <p>
     * Methods that implicitly call this method include:
     * 
     * <ul>
     * 
     * <li>{@link #sendDocument(Properties, Document))}
     * 
     * <li>{@link #textOut(String, Charset)}
     * 
     * <li>{@link #textOut(String)}
     * 
     * </ul>
     * 
     * @return the current head of the output stream chain
     * 
     * @throws IOException if an I/O error occurs in applying an
     * encoding
     */
    public OutputStream out() throws IOException {
        return responseEncoder.out();
    }

    /**
     * Get a character stream for writing the response body.
     * 
     * @param minor the MIME subtype of <samp>text/*</samp>
     * 
     * @param charset the character encoding
     * 
     * @return the requested writer
     * 
     * @throws IOException if an I/O error occurs in applying an
     * encoding
     */
    public PrintWriter textOut(String minor, Charset charset)
        throws IOException {
        MediaType mt = MediaType.of("text", minor).modify()
            .set("charset", charset.name()).apply();
        session.setField("Content-Type", mt.toString());
        return new PrintWriter(new OutputStreamWriter(out(), charset));
    }

    /**
     * Get a character stream for writing the response body as UTF-8.
     * 
     * @param minor the MIME subtype of <samp>text/*</samp>
     * 
     * @return the requested writer
     * 
     * @throws IOException if an I/O error occurs in applying an
     * encoding
     */
    public PrintWriter textOut(String minor) throws IOException {
        return textOut(minor, StandardCharsets.UTF_8);
    }

    /**
     * Tell the client that the response has no content. The status is
     * set to {@link ResponseCodes#NO_CONTENT}, and the basic session's
     * output stream is closed. Several content-related header fields
     * are also cleared.
     * 
     * @throws IOException if an I/O error occurs opening or closing the
     * stream
     */
    public void noContent() throws IOException {
        session.setStatus(ResponseCodes.NO_CONTENT);
        session.clearField("Content-Type");
        session.clearField("Content-Length");
        session.clearField("Content-Encoding");
        session.out().close();
    }

    private void setLocation(URI location, int code) {
        session.addField("Location", location.toASCIIString());
        session.setStatus(code);
    }

    /**
     * Tell the client to GET the content from another location. This
     * can be used in response to a POST. Many browsers will GET the
     * indicated content and display it, and also replace the POST with
     * it in the history, allowing the user to go back and forth within
     * the history without re-posting.
     * 
     * @param location the location to redirect to
     * 
     * @see ResponseCodes#SEE_OTHER
     */
    public void seeOther(URI location) {
        setLocation(location, ResponseCodes.SEE_OTHER);
    }

    /**
     * Tell the client not to use the URI again, and use a different one
     * instead. The client is supposed to use the same request method,
     * but some might change it. Use {@link #permanentRedirect(URI)
     * instead to make using the same method explicit.
     * 
     * @param location the location to redirect to
     * 
     * @see ResponseCodes#MOVED_PERMANENTLY
     */
    public void movedPermanently(URI location) {
        setLocation(location, ResponseCodes.MOVED_PERMANENTLY);
    }

    /**
     * Tell the client to issue the request to another location. This
     * should be seen as a temporary redirection, and the client should
     * not change its reference. It should also use the same request
     * method, but some might change it. Use
     * {@link #temporaryRedirect(URI)} instead to make using the same
     * method explicit.
     * 
     * @param session the session to respond to
     * 
     * @param location the location to redirect to
     * 
     * @see ResponseCodes#FOUND
     */
    public void found(URI location) {
        setLocation(location, ResponseCodes.FOUND);
    }

    /**
     * Tell the client to make the same request to another location. The
     * client should not change its reference, and it must use the same
     * request method.
     * 
     * @param location the location to redirect to
     * 
     * @see ResponseCodes#TEMPORARY_REDIRECT
     */
    public void temporaryRedirect(URI location) {
        setLocation(location, ResponseCodes.TEMPORARY_REDIRECT);
    }

    /**
     * Tell the client to not to use the URI again, and use a different
     * one instead. The client should not change its reference, and it
     * must use the same request method.
     * 
     * @param location the location to redirect to
     * 
     * @see ResponseCodes#PERMANENT_REDIRECT
     */
    public void permanentRedirect(URI location) {
        setLocation(location, ResponseCodes.PERMANENT_REDIRECT);
    }

    /**
     * Set the content type and deliver an XML document. The output
     * stream is then closed.
     * 
     * @param xformProps additional transformer properties
     * 
     * @param doc the document to output
     * 
     * @throws TransformerConfigurationException if building the
     * document transformer fails
     * 
     * @throws TransformerException if an unrecoverable error occurs
     * during the transformation
     * 
     * @throws IOException if an I/O error occurs in closing the
     * response
     */
    public void transmit(Document doc, Properties xformProps)
        throws TransformerException,
            IOException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer xf = tf.newTransformer();
        xf.setOutputProperties(xformProps);
        String contentType = xf.getOutputProperty(OutputKeys.MEDIA_TYPE);
        if (contentType == null) contentType = "text/xml";
        var src = new DOMSource(doc);

        try (var out = out()) {
            var dest = new StreamResult(out);
            session.setField("Content-Type", contentType);
            xf.transform(src, dest);
        }
    }
}
