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

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the relation between extensions and their local prefixes.
 * 
 * @author simpsons
 */
public final class ExtensionManager {
    private int nextWidth = 2;

    private int nextValue = 0;

    private int nextLimit = 100;

    private final Map<FieldExtension, ExtensionPrefix> prefixes =
        new HashMap<>();

    private final Map<ExtensionPrefix, FieldExtension> extensions =
        new HashMap<>();

    /**
     * Relate an extension to a suggested prefix.
     * 
     * @param pfx the suggested prefix
     * 
     * @param ext the extension to relate
     * 
     * @return the existing prefix if the extension has already been
     * related; the new prefix if that is used
     * 
     * @throws IllegalStateException if the extension has no related
     * prefix, but the suggested prefix is already in use
     */
    public ExtensionPrefix define(FieldExtension ext, ExtensionPrefix pfx) {
        var existing = prefixes.get(ext);
        if (existing != null) return existing;
        if (extensions.containsKey(pfx))
            throw new IllegalStateException("prefix in use: " + pfx);
        prefixes.put(ext, pfx);
        extensions.put(pfx, ext);
        return pfx;
    }

    /**
     * Map a prefix to an extension.
     * 
     * @param pfx the prefix of the sought extension
     * 
     * @return the extension if related to the prefix; {@code null}
     * otherwise
     */
    public FieldExtension seek(ExtensionPrefix pfx) {
        return extensions.get(pfx);
    }

    /**
     * Map an extension to a prefix.
     * 
     * @param ext the extension whose prefix is sought
     * 
     * @return the extension's prefix if defined; {@code null} otherwise
     */
    public ExtensionPrefix seek(FieldExtension ext) {
        return prefixes.get(ext);
    }

    /**
     * Relate an extension to a new prefix.
     * 
     * @param ext the extension to relate
     * 
     * @return the prefix related to the extension
     */
    public ExtensionPrefix define(FieldExtension ext) {
        var existing = prefixes.get(ext);
        if (existing != null) return existing;
        do {
            var pfx = ExtensionPrefix.of(nextWidth++, nextValue);
            if (nextValue == nextLimit) {
                nextWidth++;
                nextLimit *= 10;
                nextValue = 0;
            }
            if (extensions.containsKey(pfx)) continue;
            prefixes.put(ext, pfx);
            extensions.put(pfx, ext);
            return pfx;
        } while (true);
    }
}
