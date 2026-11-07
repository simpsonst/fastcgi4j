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

package uk.ac.lancs.fastcgi;

/**
 * Records a mutable set of fields with repeatable names.
 *
 * @author simpsons
 */
public interface ResponseFieldSet {
    /**
     * Set a response field, replacing any existing values.
     * 
     * @param name the field name
     * 
     * @param value the new value
     * 
     * @throws IllegalArgumentException if the field name is forbidden
     * 
     * @throws IllegalStateException if it's too late to modify the
     * field set
     */
    void set(String name, String value);

    /**
     * Add a response field, retaining earlier values as distinct
     * fields.
     * 
     * @param name the field name
     * 
     * @param value the additional value
     * 
     * @throws IllegalArgumentException if the field name is forbidden
     * 
     * @throws IllegalStateException if it's too late to modify the
     * field set
     */
    void add(String name, String value);

    /**
     * Remove all instances of a response field.
     * 
     * @param name the field name
     * 
     * @throws IllegalArgumentException if the field name is forbidden
     * 
     * @throws IllegalStateException if it's too late to modify the
     * field set
     */
    void clear(String name);
}
