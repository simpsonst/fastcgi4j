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

package uk.ac.lancs.http.encoding;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import uk.ac.lancs.http.field.FieldNames;

/**
 * Records how a stream should be encoded, and how this encoding should
 * be declared. The declaration is usually needed before applying the
 * plan, because the declaration needs to go in an HTTP header before
 * the raw output stream can be obtained for wrapping.
 * 
 * @author simpsons
 */
public final class EncodingPlan {
    private final List<Map.Entry<String, OutputEncoding>> plan;

    /**
     * Create an encoding plan from a collection. The iteration order of
     * the collection is preserved within the plan.
     * 
     * @param plan a collection of elements of the plan
     */
    EncodingPlan(Collection<? extends Map.Entry<? extends CharSequence,
                                                ? extends OutputEncoding>> plan) {
        this.plan = plan.stream()
            .map(e -> Map.entry(e.getKey().toString(), e.getValue()))
            .collect(Collectors.toList());
    }

    /**
     * Encode a stream using this plan. The supplied stream is wrapped
     * by the {@link OutputEncoding#encode(OutputStream)} of each
     * encoding in the plan, in reverse order, so that the first one in
     * the plan will be applied first.
     * 
     * @param base the base stream
     * 
     * @return the encoded stream
     * 
     * @throws IOException if an I/O error occurs in applying an
     * encoding
     */
    public OutputStream apply(OutputStream base) throws IOException {
        for (var ent : plan.reversed())
            base = ent.getValue().encode(base);
        return base;
    }

    /**
     * Get a declaration of the encoding order. Encodings whose
     * {@link OutputEncoding#listed()} method returns {@code false} are
     * not included. The order is the same as that which should go in a
     * <samp>{@value "%s" FieldNames#CONTENT_ENCODING}</samp> or
     * <samp>{@value "%s" FieldNames#TRANSFER_ENCODING}</samp> header
     * field.
     * 
     * @return the names of encodings that would be applied to a stream
     * submitted to {@link #apply(OutputStream)}
     */
    public List<String> declare() {
        return plan.stream().filter(e -> e.getValue().listed())
            .map(Map.Entry::getKey).collect(Collectors.toList());
    }
}
