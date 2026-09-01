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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.ac.lancs.mime.TokenException;
import uk.ac.lancs.mime.Tokenizer;

/**
 * Holds the fields of an incoming request/response header/trailer.
 * 
 * @author simpsons
 */
public interface Cap {
    /**
     * Get the raw values of a field. Some entries may contain multiple
     * comma-separated values.
     * 
     * @param id the field to extract
     * 
     * @return the field's raw values in transmission order; possibly
     * immutable for an inward message
     */
    List<String> get(FieldId id);

    /**
     * Get the parsed values of a comma-separated field. The raw field
     * may appear as multiple strings, and each may contain one or more
     * comma-separated elements, as defined by the extractor.
     * 
     * @implNote The default implementation calls {@link #get(FieldId)}
     * to get the raw field values. Each value is then submitted to a
     * tokenizer, and parsed repeatedly using the extractor followed by
     * white space and a comma.
     * 
     * @param <T> the element type
     * 
     * @param id the field to extract
     * 
     * @param extractor the converter from tokens to element, returning
     * {@code null} if it fails to parse
     * 
     * @return a list of parsed elements
     * 
     * @throws FieldSyntaxException if any of the field values does not
     * parse as a comma-separated list of elements
     */
    default <T> List<T> get(FieldId id,
                            Function<? super Tokenizer, T> extractor) {
        try {
            return get(id).stream()
                .flatMap(e -> new Tokenizer(e)
                    .extractCommaSequence(id.toString(), extractor).stream())
                .collect(Collectors.toList());
        } catch (TokenException ex) {
            throw new FieldSyntaxException(ex.getMessage(), ex);
        }
    }

    /**
     * Get the attributes of a namespace.
     * 
     * @param ns the namespace whose attributes are requested
     * 
     * @return an immutable set of name-value attributes of the given
     * namespace; an empty map if the namespace was not defined with any
     * attributes
     */
    Map<String, String> attributes(FieldNamespace ns);
}
