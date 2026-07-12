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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Holds request header or trailer fields by parsing them from an HTTP
 * request header or trailer.
 * 
 * @author simpsons
 */
public final class InputStreamCap implements Cap {
    private final Map<FieldId, List<String>> fields;

    private final Cap backup;

    /**
     * Create an immutable request cap from a stream. The stream is
     * closed after use. Only fields from the expected set are retained.
     * The keys of the expected set are case-insensitive raw names to be
     * expected, and they map to the field ids that the values should be
     * stored under.
     * 
     * @param in the source stream
     * 
     * @param expected the set of field names and their ids to expect
     * 
     * @param backup back-up fields to read from
     * 
     * @throws IOException if an I/O error occurs in reading the stream
     */
    public InputStreamCap(ExtensionManager extMgr, InputStream in,
                          Map<String, FieldId> expected, Cap backup)
        throws IOException {
        this.backup = backup;
        Map<FieldId, List<String>> fields = new HashMap<>();

        /* TODO */

        this.fields = fields.entrySet().stream().collect(Collectors
            .toMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
    }

    @Override
    public List<String> get(FieldId id) {
        List<String> val = fields.get(id);
        if (val == null) {
            if (backup == null) return Collections.emptyList();
            return backup.get(id);
        }
        return val;
    }
}
