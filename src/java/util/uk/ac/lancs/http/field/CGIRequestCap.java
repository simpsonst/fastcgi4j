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

package uk.ac.lancs.http.field;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.ac.lancs.cgi.Http;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Presents a CGI environment as a namespace-aware request header. It
 * recognizes the following parameters as containing field values:
 * 
 * <table>
 * 
 * <thead>
 * 
 * <tr>
 * <th>CGI parameter</th>
 * <th>HTTP field</th>
 * </tr>
 * 
 * </thead>
 * 
 * <tbody>
 * 
 * <tr>
 * <td><samp>CONTENT_TYPE</samp></td>
 * <td><samp>Content-Type</samp></td>
 * </tr>
 * 
 * <tr>
 * <td><samp>CONTENT_LENGTH</samp></td>
 * <td><samp>Content-Length</samp> (or a fabrication of it)</td>
 * </tr>
 * 
 * <tr>
 * <td><samp>HTTP_CONNECTION</samp></td>
 * <td><samp>Connection</samp>, used to identify hop-by-hop fields, and
 * regarded as hop-by-hop itself</td>
 * </tr>
 * 
 * <tr>
 * <td><samp>HTTP_(C_)?(MAN|OPT)</samp></td>
 * <td><samp>(C-)?(Man|Opt)</samp> containing namespace
 * declarations</td>
 * </tr>
 * 
 * <tr>
 * <td><samp>HTTP_(.+)</samp></td>
 * <td><samp>&amp;1</samp> after upper-casing and converting
 * <samp>-<samp> to <samp>_</samp>, e.g., field
 * <samp>Accept-Encoding</samp> becomes
 * <samp>HTTP_ACCEPT_ENCODING</samp></td>
 * </tr>
 * 
 * </tbody>
 * 
 * </table>
 *
 * @author simpsons
 */
public class CGIRequestCap implements Cap {
    private static final String EXPERIMENTAL_PREFIX = "X-";

    private final ExtensionManager extMgr;

    private final Map<? extends String, ? extends CharSequence> env;

    private final Map<FieldNamespace, Map<String, String>> nsAttrs =
        new HashMap<>();

    private static final Pattern FIELD_PATTERN = Pattern.compile("^"
        + Pattern.quote(Http.META_PREFIX) + "(?<c>C_)?(?<ns>OPT|MAN)$");

    private static final String CONNECTION_FIELD_VAR =
        Http.fieldNameAsCGI("CONNECTION");

    private static boolean isConnectionFieldName(String s) {
        switch (s) {
        default:
            return true;

        case "close":
        case "keep-alive":
            return false;
        }
    }

    private final Set<String> hopByHopKeys;

    /**
     * Create a request header from a CGI environment.
     * 
     * @param extMgr a record of extension definitions in the supplied
     * environment
     * 
     * @param env the CGI environment, which must remain valid for the
     * lifetime of the new object
     * 
     * @throws IllegalArgumentException if a namespace declaration is
     * badly formed
     */
    public CGIRequestCap(ExtensionManager extMgr,
                         Map<? extends String, ? extends CharSequence> env) {
        this.extMgr = extMgr;
        this.env = env;

        /* Identify hop-by-hop fields. We get the Connection field,
         * split as comma-separated tokens, filter out special tokens
         * like "close" and "keep-alive", convert the rest to CGI
         * variable names, and add in the connection field itself. */
        this.hopByHopKeys = Stream
            .concat(Tokenizer.atomSequenceOf(env.get(CONNECTION_FIELD_VAR))
                .stream().filter(CGIRequestCap::isConnectionFieldName)
                .map(Http::fieldNameAsCGI), Stream.of(CONNECTION_FIELD_VAR))
            .collect(Collectors.toSet());

        /* Go through each of the environmental fields matching those
         * which describe HTTP field extensions. */
        for (var ent : env.entrySet()) {
            var key = ent.getKey();
            var val = ent.getValue();

            /* Match the recognized patterns. */
            var m = FIELD_PATTERN.matcher(key);
            if (!m.matches()) continue;

            /* Detect namespace declarations. */
            var ns = m.group("ns");
            assert ns != null;

            /* Determine whether this is an end-to-end or hop-by-hop
             * namespace. */
            boolean conn = m.group("c") != null; // true if
                                                 // hop-by-hop

            /* Determine whether this is an optional or mandatory
             * namespace. */
            boolean mand = ns.charAt(0) == 'M'; // false if OPT

            /* We only have one variable to hold a comma-separated
             * concatenation of fields. Keep parsing a quoted string
             * containing the namespace URI, then semicolon-separated
             * parameters. */
            var tokens = new Tokenizer(val);
            do {
                /* Attempt to parse a parameterized quoted string. */
                Map<String, String> params = new HashMap<>();
                var nsuri = tokens.whitespaceQuotedStringParameters(0, params);
                String pfxTxt = params.remove("ns");
                var pfx = ExtensionPrefix.of(pfxTxt);
                var ext = FieldExtension.of(nsuri).hopByHop(conn)
                    .mandatory(mand).complete();
                nsAttrs.put(ext, params);
                extMgr.define(ext, pfx);

                /* Detect another extension declaration, the end of
                 * declarations, or something unexpected. */
                tokens.whitespace(0);
                if (tokens.character(',')) continue;
                if (tokens.end()) break;
                throw new IllegalArgumentException("bad extension definition: "
                    + key + " -> " + val);
            } while (true);
        }

        /* Freeze the additional attributes of each defined
         * namespace. */
        for (var ent : nsAttrs.entrySet())
            ent.setValue(Map.copyOf(ent.getValue()));
    }

    /**
     * {@inheritDoc}
     * 
     * @implNote Because CGI is required to fold multiple fields with
     * the same name into one, this method only ever returns a singleton
     * list for a present field, or an empty list for an absent field.
     * 
     * @param id {@inheritDoc}
     * 
     * @return the field's raw values in transmission order, as a
     * singleton list; an empty list if the field is not present
     */
    @Override
    public List<String> get(FieldId id) {
        /* Prefix the name according to the namespace of the field. */
        var ns = id.namespace();
        final String key;
        switch (ns.kind()) {
        case STANDARD:
            /* Standard fields require no prefix. */
            key = Http.fieldNameAsCGI(id.name());
            break;

        /* Experimental fields require a prefix of X_. */
        case EXPERIMENTAL:
            key = Http.fieldNameAsCGI(EXPERIMENTAL_PREFIX + id.name());
            break;

        /* Extension fields require a prefix of HTTP_, a number of at
         * least 2 digits, and another _. */
        case EXTENSION:
            var ext = ns.asExtension();
            assert ext != null;
            var pfx = extMgr.seek(ext);
            if (pfx == null) return null;
            key = Http.fieldNameAsCGI(pfx.toString() + '-' + id.name());
            break;

        default:
            throw new AssertionError("unreachable");
        }

        /* Verify that the scope is as expected. If not, it's not the
         * same field, so it must be discarded. */
        var gotScope = hopByHopKeys.contains(key) ? FieldScope.HOP_BY_HOP :
            FieldScope.END_TO_END;
        if (gotScope != ns.scope()) return null;

        /* Get the field value. Because we don't know how to split, we
         * have to return a singleton. If the field is absent, we return
         * an empty list. */
        var raw = env.get(key);
        if (raw == null) return Collections.emptyList();
        return Collections.singletonList(raw.toString());
    }

    @Override
    public Map<String, String> attributes(FieldNamespace ns) {
        return nsAttrs.getOrDefault(ns, Collections.emptyMap());
    }
}
