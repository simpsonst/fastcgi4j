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

package uk.ac.lancs.fastcgi.misc;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import uk.ac.lancs.fastcgi.context.Session;
import uk.ac.lancs.fastcgi.mime.MediaGroup;
import uk.ac.lancs.fastcgi.mime.Tokenizer;
import uk.ac.lancs.fastcgi.util.HttpStatus;

/**
 * Provides an augmented session context.
 *
 * @author simpsons
 */
public final class SessionAugment {
    private final Session session;

    private static final Map<String, Float> COMPRESSION_OFFER =
        Map.copyOf(getCompressionOffer());

    private static Map<String, Float> getCompressionOffer() {
        Map<String, Float> offer = new HashMap<>();
        offer.put("gzip", 1.0f);
        offer.put("deflate", 0.7f);
        return offer;
    }

    /**
     * Get the client's encoding preference. This is simply an
     * application of {@link #getAtomPreference(CharSequence)} to the
     * <samp>Accept-Encoding</samp> request header field.
     * 
     * @return an immutable map of encodings to quality parameters
     */
    public Map<String, Float> getEncodingPreference() {
        return getAtomPreference(session.parameters()
            .get("HTTP_ACCEPT_ENCODING"));
    }

    /**
     * Split a string into comma-separated tokens and optional
     * parameters, selecting the quality parameter.
     * 
     * @param fieldValue the string to split, usually the value of a
     * request header field; may be {@code null}
     * 
     * @return an immutable map from token token to quality parameter;
     * an empty map if the argument is {@code null}
     */
    public static Map<String, Float>
        getAtomPreference(CharSequence fieldValue) {
        if (fieldValue == null) return Collections.emptyMap();
        Map<String, Float> result = new HashMap<>();
        Tokenizer toks = new Tokenizer(fieldValue);
        while (true) {
            var name = toks.whitespaceAtom(0);
            toks.whitespace(0);
            Map.Entry<String, String> param;
            float q = 1.0f;
            while (toks.character(';') && toks.whitespace(0) &&
                (param = toks.parameter()) != null) {
                if (param.getKey().equals("q"))
                    q = Float.parseFloat(param.getValue());
                toks.whitespace(0);
            }
            if (toks.character(',')) {
                result.put(name.toString(), q);
                continue;
            }
            if (toks.end()) break;
            throw new IllegalArgumentException("bad atom preference: "
                + fieldValue);
        }
        return Map.copyOf(result);
    }

    /**
     * Get the client's media-type preferences. This simply passes the
     * <samp>Accept</samp> request header field to
     * {@link #getMediaTypePreference(CharSequence)}.
     * 
     * @return an immutable of the client's media-type preferences
     */
    public Map<MediaGroup, Float> getMediaTypePreference() {
        return getMediaTypePreference(session.parameters().get("HTTP_ACCEPT"));
    }

    /**
     * Split a string into comma-separated MIME types and optional
     * parameters, selecting the quality parameter.
     * 
     * @param fieldValue the string to split, usually the value of a
     * request header field; may be {@code null}
     * 
     * @return an immutable map from token token to quality parameter;
     * an empty map if the argument is {@code null}
     */
    public static Map<MediaGroup, Float>
        getMediaTypePreference(CharSequence fieldValue) {
        if (fieldValue == null) return Collections.emptyMap();
        Map<MediaGroup, Float> result = new HashMap<>();
        Tokenizer toks = new Tokenizer(fieldValue);
        while (true) {
            var group = MediaGroup.from(toks);
            toks.whitespace(0);
            Map.Entry<String, String> param;
            float q = 1.0f;
            while (toks.character(';') && toks.whitespace(0) &&
                (param = toks.parameter()) != null) {
                if (param.getKey().equals("q"))
                    q = Float.parseFloat(param.getValue());
                toks.whitespace(0);
            }
            if (toks.character(',')) {
                result.put(group, q);
                continue;
            }
            if (toks.end()) break;
            throw new IllegalArgumentException("bad media-group preference: "
                + fieldValue);
        }
        return Map.copyOf(result);
    }

    /**
     * Create an augmented session context.
     * 
     * @param session the basic session
     */
    public SessionAugment(Session session) {
        this.session = session;
        this.out = session.out();
    }

    private OutputStream out;

    private boolean compressed = false;

    /**
     * Turn on compression if the client accepts it. If applied, the
     * output stream is wrapped in a compression filter, and the
     * encoding name is added to <samp>Content-Encoding</samp>.
     * 
     * <p>
     * This method must be called before {@link #out()}.
     * 
     * <p>
     * In the current implementation, only <samp>gzip</samp> and
     * <samp>deflate</samp> are offered.
     */
    public void offerCompression() throws IOException {
        if (compressed) return;
        compressed = true;
        Map<String, Float> pref = getEncodingPreference();
        String comp =
            Negotiation.resolveStringPreference(pref, COMPRESSION_OFFER);
        switch (comp) {
        case "gzip":
            out = new GZIPOutputStream(out);
            session.addHeader("Content-Encoding", comp);
            break;

        case "deflate":
            out = new DeflaterOutputStream(out);
            session.addHeader("Content-Encoding", comp);
            break;
        }
    }

    /**
     * Get the output stream. This is the head of what could be a long
     * chain of wrapping streams. For example,
     * {@link #offerCompression()} may wrap a {@link GZIPOutputStream}
     * around the current head.
     * 
     * @return the current head of the output stream chain
     */
    public OutputStream out() {
        return out;
    }

    private void setLocation(URI location, int code) {
        session.addHeader("Location", location.toASCIIString());
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
     * @see HttpStatus#SEE_OTHER
     */
    public void seeOther(URI location) {
        setLocation(location, HttpStatus.SEE_OTHER);
    }

    /**
     * Tell the client not to use the URI again, and use a different one
     * instead. The client is supposed to use the same request method,
     * but some might change it. Use
     * {@link #permanentRedirect(Session, URI)} instead to make using
     * the same method explicit.
     * 
     * @param location the location to redirect to
     * 
     * @see HttpStatus#MOVED_PERMANENTLY
     */
    public void movedPermanently(URI location) {
        setLocation(location, HttpStatus.MOVED_PERMANENTLY);
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
     * @see HttpStatus#FOUND
     */
    public void found(URI location) {
        setLocation(location, HttpStatus.FOUND);
    }

    /**
     * Tell the client to make the same request to another location. The
     * client should not change its reference, and it must use the same
     * request method.
     * 
     * @param location the location to redirect to
     * 
     * @see HttpStatus#TEMPORARY_REDIRECT
     */
    public void temporaryRedirect(URI location) {
        setLocation(location, HttpStatus.TEMPORARY_REDIRECT);
    }

    /**
     * Tell the client to not to use the URI again, and use a different
     * one instead. The client should not change its reference, and it
     * must use the same request method.
     * 
     * @param location the location to redirect to
     * 
     * @see HttpStatus#PERMANENT_REDIRECT
     */
    public void permanentRedirect(URI location) {
        setLocation(location, HttpStatus.PERMANENT_REDIRECT);
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
    public void sendDocument(Properties xformProps, Document doc)
        throws TransformerConfigurationException,
            TransformerException,
            IOException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer xf = tf.newTransformer();
        xf.setOutputProperties(xformProps);
        String contentType = xf.getOutputProperty(OutputKeys.MEDIA_TYPE);
        if (contentType == null) contentType = "text/xml";
        var src = new DOMSource(doc);

        try (var out = out()) {
            var dest = new StreamResult(out);
            session.setHeader("Content-Type", contentType);
            xf.transform(src, dest);
        }
    }
}
