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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Presents a CGI environment as a namespace-aware request header.
 *
 * @author simpsons
 */
public class CGIRequestCap implements Cap {
    private static final String PREFIX = "HTTP_";

    private static final String EXPERIMENTAL_PREFIX = PREFIX + "X_";

    private static final String EXTDEF_PATTERN_TEXT =
        "^" + Pattern.quote(PREFIX) + "(C_)?(OPT|MAN)$";

    private static final Pattern EXTDEF_PATTERN =
        Pattern.compile(EXTDEF_PATTERN_TEXT);

    private final ExtensionManager extMgr;

    private final Map<String, String> env;

    /**
     * Create a request header from a CGI environment. The supplied
     * environment is first scanned for extension definitions according
     * to RFC2774, which are added to the extension manager. Other
     * entries beginning with {@value #PREFIX} are retained internally.
     * The supplied environment itself is not retained within this
     * object.
     * 
     * @param extMgr a record of extension definitions in the supplied
     * environment
     * 
     * @param env the CGI environment
     */
    public CGIRequestCap(ExtensionManager extMgr,
                         Map<? extends CharSequence,
                             ? extends CharSequence> env) {
        this.extMgr = extMgr;
        this.env = new HashMap<>();

        CharSequence connFields = null;
        for (var ent : env.entrySet()) {
            var key = ent.getKey();
            var val = ent.getValue();
            var mext = EXTDEF_PATTERN.matcher(key);
            if (mext.matches()) {
                boolean conn = mext.group(1) != null;
                boolean mand = mext.group(2).equals("MAN");
                var tokens = new Tokenizer(val);
                do {
                    tokens.whitespace(0);
                    var nsuri = tokens.quotedString();
                    Map<String, String> params = new HashMap<>();
                    tokens.parameters(params);
                    String pfxTxt = params.remove("ns");
                    var pfx = ExtensionPrefix.of(pfxTxt);
                    var ext = FieldExtension.of(nsuri).hopByHop(conn)
                        .mandatory(mand).attributes(params).complete();
                    extMgr.define(ext, pfx);
                    tokens.whitespace(0);
                    if (tokens.character(',')) continue;
                    if (tokens.end()) break;
                    throw new IllegalArgumentException("bad extension definition: "
                        + key + " -> " + val);
                } while (true);
                continue;
            }
            if ((PREFIX + "CONNECTION").equals(key)) {
                connFields = val;
                continue;
            }
            if (startsWith(key, PREFIX))
                this.env.put(key.toString(), val.toString());
        }
        if (connFields != null) {
            /* Split the value on commas into atoms. Each is to be
             * treated as a header name. TODO */
        }
    }

    private static boolean startsWith(CharSequence cs, CharSequence pfx) {
        final var cslen = cs.length();
        final var pfxlen = pfx.length();
        int i;
        for (i = 0; i < cslen && i < pfxlen; i++)
            if (cs.charAt(i) != pfx.charAt(i)) return false;
        return i == pfxlen;
    }

    @Override
    public List<String> get(FieldId id) {
        String sfx = id.name().toUpperCase(Locale.ROOT).replace('-', '_');
        var ns = id.namespace();
        if (ns.isNative()) return Collections
            .singletonList(safeToString(env.get(PREFIX + sfx)));
        if (ns.isExperimental()) return Collections
            .singletonList(safeToString(env.get(EXPERIMENTAL_PREFIX + sfx)));
        var ext = ns.asExtension();
        assert ext != null;
        var pfx = extMgr.seek(ext);
        if (pfx == null) return null;
        return Collections
            .singletonList(safeToString(env.get(PREFIX + pfx + '_' + sfx)));
    }

    private static String safeToString(Object obj) {
        return obj == null ? null : obj.toString();
    }
}
