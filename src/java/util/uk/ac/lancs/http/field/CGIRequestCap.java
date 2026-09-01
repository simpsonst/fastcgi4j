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
 * Presents a CGI environment as a namespace-aware request header.
 *
 * @author simpsons
 */
public class CGIRequestCap implements Cap {
    private static final String META_PREFIX = "HTTP_";

    private static final String EXPERIMENTAL_PREFIX = "X_";

    private final ExtensionManager extMgr;

    private final Map<String, String> env = new HashMap<>();

    private final Map<FieldNamespace, Map<String, String>> nsAttrs =
        new HashMap<>();

    private static final Pattern FIELD_PATTERN =
        Pattern.compile("^(?<n>CONTENT_(?:TYPE|LENGTH))|"

            + Pattern.quote(META_PREFIX) + "(?:"

            + "(?<c>C_)?(?<ns>OPT|MAN)"

            + "|"

            + "?(?<core>.+)"

            + ")$");

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
     * Create a request header from a CGI environment. The supplied
     * environment is scanned for entries beginning with
     * {@value #META_PREFIX}, as these pass the values of HTTP header
     * fields. The following entries are also detected:
     * 
     * <ul>
     * 
     * <li><samp>CONTENT_TYPE</samp> as the value of the standard
     * end-to-end field <samp>Content-Type</samp>, and
     * 
     * <li><samp>CONTENT_LENGTH</samp> as the value of the standard
     * end-to-end field <samp>Content-Length</samp> (or a version of it
     * fabricated by CGI).
     * 
     * </ul>
     * 
     * <p>
     * Namespace declarations are also recognized as entries
     * <samp>HTTP_OPT</samp>, <samp>HTTP_MAN</samp>,
     * <samp>HTTP_C_OPT</samp>, and <samp>HTTP_C_MAN</samp>,
     * corresponding to the HTTP header fields <samp>Opt</samp>,
     * <samp>Man</samp>, <samp>C-Opt</samp>, and <samp>C-Man</samp>,
     * which are not accessible through {@link #get(FieldId)}. These
     * allow fields belonging to a URI-identified namespace to be
     * accessed, without knowing the numeric prefix defined for the
     * scope of the message. Use {@link FieldExtension} to define a
     * namespace and access fields within it.
     * 
     * <p>
     * The supplied environment itself is not retained in full within
     * this object.
     * 
     * @param extMgr a record of extension definitions in the supplied
     * environment
     * 
     * @param env the CGI environment
     * 
     * @throws IllegalArgumentException if a namespace declaration is
     * badly formed
     */
    public CGIRequestCap(ExtensionManager extMgr,
                         Map<? extends CharSequence,
                             ? extends CharSequence> env) {
        this.extMgr = extMgr;

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
         * which describe HTTP header fields. */
        for (var ent : env.entrySet()) {
            var key = ent.getKey();
            var val = ent.getValue();

            /* Match the recognized patterns. */
            var m = FIELD_PATTERN.matcher(key);
            if (!m.matches()) continue;

            /* Detect special fields that are recognized by CGI. Store
             * them as they are. */
            var n = m.group("n");
            if (n != null) {
                this.env.put(n, val.toString());
                continue;
            }

            /* Detect namespace declarations. */
            var ns = m.group("ns");
            if (ns != null) {
                /* Determine whether this is an end-to-end or hop-by-hop
                 * namespace. */
                boolean conn = m.group("c") != null; // true if
                                                     // hop-by-hop

                /* Determine whether this is an optional or mandatory
                 * namespace. */
                boolean mand = ns.charAt(0) == 'M'; // false if OPT

                /* We only have one variable to hold a comma-separated
                 * concatenation of fields. Keep parsing a quoted string
                 * containing the namespace URI, then
                 * semicolon-separated parameters. */
                var tokens = new Tokenizer(val);
                do {
                    /* Attempt to parse a parameterized quoted
                     * string. */
                    Map<String, String> params = new HashMap<>();
                    var nsuri =
                        tokens.whitespaceQuotedStringParameters(0, params);
                    if (nsuri == null)
                        throw new IllegalArgumentException("bad extension definition: "
                            + key + " -> " + val);
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
                continue;
            }

            /* Whatever's left has no special value for us, so store it
             * as is. */
            var core = m.group("core");
            assert core != null;
            this.env.put(core, val.toString());
        }

        /* Freeze the additional attributes of each defined
         * namespace. */
        for (var ent : nsAttrs.entrySet())
            ent.setValue(Map.copyOf(ent.getValue()));
    }

    @Override
    public List<String> get(FieldId id) {
        /* Map the field id to a variable name. The end of that name is
         * an upper-case version of the field name with hyphens
         * converted to underscores. */
        String sfx = id.gatewayName();

        /* Prefix the name according to the namespace of the field. */
        var ns = id.namespace();
        final String key;
        switch (ns.kind()) {
        case STANDARD:
            /* Standard fields require no prefix. */
            key = sfx;
            break;

        /* Experimental fields require a prefix of X_. */
        case EXPERIMENTAL:
            key = EXPERIMENTAL_PREFIX + sfx;
            break;

        /* Extension fields require a prefix of HTTP_, a number of at
         * least 2 digits, and another _. */
        case EXTENSION:
            var ext = ns.asExtension();
            assert ext != null;
            var pfx = extMgr.seek(ext);
            if (pfx == null) return null;
            key = pfx.toString() + '_' + sfx;
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
        return Collections.singletonList(raw);
    }

    @Override
    public Map<String, String> attributes(FieldNamespace ns) {
        return nsAttrs.getOrDefault(ns, Collections.emptyMap());
    }
}
