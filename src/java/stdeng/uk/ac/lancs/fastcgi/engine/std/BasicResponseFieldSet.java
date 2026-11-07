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

package uk.ac.lancs.fastcgi.engine.std;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import uk.ac.lancs.fastcgi.ResponseFieldSet;

/**
 * Presents a map of field names to sequences of values as a mutable
 * field set.
 * 
 * <p>
 * Before each modification, a user-defined state test is performed. If
 * this fails, {@link IllegalStateException} is thrown.
 * 
 * <p>
 * Field names have leading and trailing spaces trimmed. For setting and
 * appending fields, the name is submitted to a user-defined guard for
 * validation, and {@link IllegalArgumentException} is thrown if
 * validation fails.
 * 
 * @author simpsons
 */
class BasicResponseFieldSet implements ResponseFieldSet {
    private final Map<String, List<String>> base;

    private final Supplier<? extends List<String>> listSupplier;

    private final Supplier<? extends CharSequence> stateGuard;

    private final Function<? super String, ? extends CharSequence> nameGuard;

    private final Runnable onSet;

    /**
     * Create a repeatable-name field set view of a map with list
     * values. The returned implementation trims leading and trailing
     * spaces from the supplied field name, and rejects names not
     * accepted by a guard function.
     * 
     * @param base the base map
     * 
     * @param listSupplier a provider of mutable lists for keys not
     * already in the map
     * 
     * @param stateGuard a function indicating whether it's too late to
     * change the field set, returning a component to be used in the
     * error message if it is, or {@code null} if not
     * 
     * @param nameGuard a function indicating whether a proposed field
     * name (after trimming) is forbidden, returning a component to be
     * used in the error message if it is (e.g., <samp>reserved
     * name</samp>), or {@code null} if the name is acceptable
     * 
     * @param onSet an action to perform on successful modification
     * other than clearing a field
     * 
     * @return the requested view of the map
     */
    public BasicResponseFieldSet(Map<String, List<String>> base,
                                 Supplier<? extends List<String>> listSupplier,
                                 Supplier<? extends CharSequence> stateGuard,
                                 Function<? super String,
                                          ? extends CharSequence> nameGuard,
                                 Runnable onSet) {
        this.base = base;
        this.listSupplier = listSupplier;
        this.stateGuard = stateGuard;
        this.nameGuard = nameGuard;
        this.onSet = onSet;
    }

    private String normalize(String name, boolean checkName) {
        var em = stateGuard.get();
        if (em != null) throw new IllegalStateException(em.toString());
        name = name.trim();
        if (checkName) {
            var nem = nameGuard.apply(name);
            if (nem != null) throw new IllegalArgumentException(nem.toString()
                + ": " + name);
        }
        return name;
    }

    @Override
    public void set(String name, String value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        name = normalize(name, true);
        var content = base.computeIfAbsent(name, k -> listSupplier.get());
        content.clear();
        content.add(value);
        onSet.run();
    }

    @Override
    public void add(String name, String value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        name = normalize(name, true);
        base.computeIfAbsent(name, k -> listSupplier.get()).add(value);
        onSet.run();
    }

    @Override
    public void clear(String name) {
        Objects.requireNonNull(name, "name");
        name = normalize(name, false);
        base.remove(name);
    }
}
