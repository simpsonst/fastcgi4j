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

package uk.ac.lancs.http.cache;

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.junit.Test;
import uk.ac.lancs.http.field.ExtensionManager;
import uk.ac.lancs.http.field.FieldId;

/**
 *
 * @author simpsons
 */
public class TestCacheControl extends TestCase {
    static final ExtensionManager exts = new ExtensionManager();

    static final InCacheContext inCtxt = new InCacheContext(exts, t -> false);

    static final OutCacheContext outCtxt = new OutCacheContext(exts);

    @Test
    public void testParsing() {
        {
            var ctrl = InCacheControl
                .ofRequest(inCtxt, CacheDirective.ALL_DIRECTIVES, "");
            assertFalse("immutable absent",
                        CacheDirective.IMMUTABLE.test(ctrl));
            assertFalse("max-stale unlimited",
                        CacheDirective.MAX_STALE_UNLIMITED.test(ctrl));
            assertFalse("must-revalidate absent",
                        CacheDirective.MUST_REVALIDATE.test(ctrl));
            assertFalse("must-understand absent",
                        CacheDirective.MUST_UNDERSTAND.test(ctrl));
            assertFalse("no-cache absent", CacheDirective.NO_CACHE.test(ctrl));
            assertFalse("no-store absent", CacheDirective.NO_STORE.test(ctrl));
            assertFalse("no-transform absent",
                        CacheDirective.NO_TRANSFORM.test(ctrl));
            assertFalse("only-if-cached absent",
                        CacheDirective.ONLY_IF_CACHED.test(ctrl));
            assertFalse("private absent", CacheDirective.PRIVATE.test(ctrl));
            assertFalse("proxy-revalidate absent",
                        CacheDirective.PROXY_REVALIDATE.test(ctrl));
            assertFalse("public absent", CacheDirective.PUBLIC.test(ctrl));
            assertFalse("max-age unset",
                        CacheDirective.MAX_AGE.get(ctrl).isPresent());
            assertFalse("max-stale unset",
                        CacheDirective.MAX_STALE.get(ctrl).isPresent());
            assertFalse("min-fresh unset",
                        CacheDirective.MIN_FRESH.get(ctrl).isPresent());
            assertFalse("stale-if-error unset",
                        CacheDirective.STALE_IF_ERROR.get(ctrl).isPresent());
            assertFalse("stale-while-revalidate unset",
                        CacheDirective.STALE_WHILE_REVALIDATE.get(ctrl)
                            .isPresent());
            assertFalse("s-maxage unset",
                        CacheDirective.S_MAXAGE.get(ctrl).isPresent());
            assertFalse("no-cache empty", CacheDirective.NO_CACHE_FIELDS
                .test(ctrl, FieldId.LOCATION));
        }

        {
            var ctrl =
                InCacheControl.ofRequest(inCtxt, CacheDirective.ALL_DIRECTIVES,
                                         "private, no-transform, no-cache");
            assertFalse("immutable absent",
                        CacheDirective.IMMUTABLE.test(ctrl));
            assertFalse("max-stale unlimited",
                        CacheDirective.MAX_STALE_UNLIMITED.test(ctrl));
            assertFalse("must-revalidate absent",
                        CacheDirective.MUST_REVALIDATE.test(ctrl));
            assertFalse("must-understand absent",
                        CacheDirective.MUST_UNDERSTAND.test(ctrl));
            assertTrue("no-cache present", CacheDirective.NO_CACHE.test(ctrl));
            assertFalse("no-store absent", CacheDirective.NO_STORE.test(ctrl));
            assertTrue("no-transform present",
                       CacheDirective.NO_TRANSFORM.test(ctrl));
            assertFalse("only-if-cached absent",
                        CacheDirective.ONLY_IF_CACHED.test(ctrl));
            assertFalse("private absent", CacheDirective.PRIVATE.test(ctrl));
            assertFalse("proxy-revalidate absent",
                        CacheDirective.PROXY_REVALIDATE.test(ctrl));
            assertFalse("public absent", CacheDirective.PUBLIC.test(ctrl));
            assertFalse("max-age unset",
                        CacheDirective.MAX_AGE.get(ctrl).isPresent());
            assertFalse("max-stale unset",
                        CacheDirective.MAX_STALE.get(ctrl).isPresent());
            assertFalse("min-fresh unset",
                        CacheDirective.MIN_FRESH.get(ctrl).isPresent());
            assertFalse("stale-if-error unset",
                        CacheDirective.STALE_IF_ERROR.get(ctrl).isPresent());
            assertFalse("stale-while-revalidate unset",
                        CacheDirective.STALE_WHILE_REVALIDATE.get(ctrl)
                            .isPresent());
            assertFalse("s-maxage unset",
                        CacheDirective.S_MAXAGE.get(ctrl).isPresent());
            assertFalse("no-cache empty", CacheDirective.NO_CACHE_FIELDS
                .test(ctrl, FieldId.LOCATION));
        }

        {
            var ctrl = InCacheControl
                .ofRequest(inCtxt, CacheDirective.ALL_DIRECTIVES,
                           "max-age=23, no-cache=\"location\", foo");
            assertFalse("immutable absent",
                        CacheDirective.IMMUTABLE.test(ctrl));
            assertFalse("max-stale unlimited",
                        CacheDirective.MAX_STALE_UNLIMITED.test(ctrl));
            assertFalse("must-revalidate absent",
                        CacheDirective.MUST_REVALIDATE.test(ctrl));
            assertFalse("must-understand absent",
                        CacheDirective.MUST_UNDERSTAND.test(ctrl));
            assertFalse("no-cache absent", CacheDirective.NO_CACHE.test(ctrl));
            assertFalse("no-store absent", CacheDirective.NO_STORE.test(ctrl));
            assertFalse("no-transform absent",
                        CacheDirective.NO_TRANSFORM.test(ctrl));
            assertFalse("only-if-cached absent",
                        CacheDirective.ONLY_IF_CACHED.test(ctrl));
            assertFalse("private absent", CacheDirective.PRIVATE.test(ctrl));
            assertFalse("proxy-revalidate absent",
                        CacheDirective.PROXY_REVALIDATE.test(ctrl));
            assertFalse("public absent", CacheDirective.PUBLIC.test(ctrl));
            assertTrue("max-age unset",
                       CacheDirective.MAX_AGE.get(ctrl).isPresent());
            assertEquals("max-age=23",
                         CacheDirective.MAX_AGE.get(ctrl).getAsInt(), 23);
            assertFalse("max-stale unset",
                        CacheDirective.MAX_STALE.get(ctrl).isPresent());
            assertFalse("min-fresh unset",
                        CacheDirective.MIN_FRESH.get(ctrl).isPresent());
            assertFalse("stale-if-error unset",
                        CacheDirective.STALE_IF_ERROR.get(ctrl).isPresent());
            assertFalse("stale-while-revalidate unset",
                        CacheDirective.STALE_WHILE_REVALIDATE.get(ctrl)
                            .isPresent());
            assertFalse("s-maxage unset",
                        CacheDirective.S_MAXAGE.get(ctrl).isPresent());
            assertFalse("no-cache empty", CacheDirective.NO_CACHE_FIELDS
                .test(ctrl, FieldId.LOCATION));
        }

        {
            var ctrl = InCacheControl
                .ofResponse(inCtxt, CacheDirective.ALL_DIRECTIVES,
                            "max-age=23, no-cache=\"location\", foo");
            assertFalse("immutable absent",
                        CacheDirective.IMMUTABLE.test(ctrl));
            assertFalse("max-stale unlimited",
                        CacheDirective.MAX_STALE_UNLIMITED.test(ctrl));
            assertFalse("must-revalidate absent",
                        CacheDirective.MUST_REVALIDATE.test(ctrl));
            assertFalse("must-understand absent",
                        CacheDirective.MUST_UNDERSTAND.test(ctrl));
            assertFalse("no-cache absent", CacheDirective.NO_CACHE.test(ctrl));
            assertFalse("no-store absent", CacheDirective.NO_STORE.test(ctrl));
            assertFalse("no-transform absent",
                        CacheDirective.NO_TRANSFORM.test(ctrl));
            assertFalse("only-if-cached absent",
                        CacheDirective.ONLY_IF_CACHED.test(ctrl));
            assertFalse("private absent", CacheDirective.PRIVATE.test(ctrl));
            assertFalse("proxy-revalidate absent",
                        CacheDirective.PROXY_REVALIDATE.test(ctrl));
            assertFalse("public absent", CacheDirective.PUBLIC.test(ctrl));
            assertTrue("max-age unset",
                       CacheDirective.MAX_AGE.get(ctrl).isPresent());
            assertEquals("max-age=23",
                         CacheDirective.MAX_AGE.get(ctrl).getAsInt(), 23);
            assertFalse("max-stale unset",
                        CacheDirective.MAX_STALE.get(ctrl).isPresent());
            assertFalse("min-fresh unset",
                        CacheDirective.MIN_FRESH.get(ctrl).isPresent());
            assertFalse("stale-if-error unset",
                        CacheDirective.STALE_IF_ERROR.get(ctrl).isPresent());
            assertFalse("stale-while-revalidate unset",
                        CacheDirective.STALE_WHILE_REVALIDATE.get(ctrl)
                            .isPresent());
            assertFalse("s-maxage unset",
                        CacheDirective.S_MAXAGE.get(ctrl).isPresent());
            assertTrue("no-cache empty", CacheDirective.NO_CACHE_FIELDS
                .test(ctrl, FieldId.LOCATION));
        }
    }

    @Test
    public void testGenerating() {
        var ouCtrl = OutCacheControl.forResponse();
        CacheDirective.MUST_UNDERSTAND.set(ouCtrl);
        CacheDirective.MAX_AGE.set(ouCtrl, 23);
        CacheDirective.NO_CACHE_FIELDS.include(ouCtrl, FieldId.LOCATION);
        CacheDirective.NO_CACHE_FIELDS.include(ouCtrl, FieldId.CONTENT_DIGEST);

        {
            List<String> elems = new ArrayList<>();
            ouCtrl.write(outCtxt, elems::add);
            System.err.println(elems);
            var inCtrl = InCacheControl
                .ofResponse(inCtxt, CacheDirective.ALL_DIRECTIVES, elems);
            assertTrue("must-understand present",
                       CacheDirective.MUST_UNDERSTAND.test(inCtrl));
            assertTrue("max-age present",
                       CacheDirective.MAX_AGE.get(inCtrl).isPresent());
            assertEquals("max-age=23",
                         CacheDirective.MAX_AGE.get(inCtrl).getAsInt(), 23);
            assertTrue("no-cache includes Location",
                       CacheDirective.NO_CACHE_FIELDS.test(inCtrl,
                                                           FieldId.LOCATION));
            assertTrue("no-cache includes Content-Digest",
                       CacheDirective.NO_CACHE_FIELDS
                           .test(inCtrl, FieldId.CONTENT_DIGEST));
            assertFalse("no-cache excludes Content-Type",
                        CacheDirective.NO_CACHE_FIELDS
                            .test(inCtrl, FieldId.CONTENT_TYPE));
        }
    }
}
