/*
 * Copyright (c) 2022, Regents of the University of Lancaster
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
 *  Author: Steven Simpson <s.simpson@lancaster.ac.uk>
 */

package uk.ac.lancs.fastcgi.engine;

import java.util.Map;
import java.util.function.Supplier;
import uk.ac.lancs.fastcgi.Authorizer;
import uk.ac.lancs.fastcgi.Filter;
import uk.ac.lancs.fastcgi.Responder;

/**
 * Identifies a typed attribute that an engine is required to have.
 * 
 * @param <V> the value type
 * 
 * @author simpsons
 */
public final class Attribute<V> {
    private final Class<V> type;

    private final Supplier<? extends V> defaultValue;

    private Attribute(Class<V> type, Supplier<? extends V> defaultValue) {
        assert type != null;
        assert defaultValue != null;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    /**
     * Create an attribute key with a default value.
     * 
     * @param <V> the attribute value type
     * 
     * @param type the attribute value type
     * 
     * @param defaultValue the default attribute value
     * 
     * @return the required key
     * 
     * @constructor
     */
    public static <V> Attribute<V> of(Class<V> type, V defaultValue) {
        return new Attribute<>(type, () -> defaultValue);
    }

    /**
     * Create an attribute key with a means to obtain the default value.
     * 
     * @param <V> the attribute value type
     * 
     * @param type the attribute value type
     * 
     * @param defaultValueSupplier a supplier for the default attribute
     * value
     * 
     * @return the required key
     * 
     * @constructor
     */
    public static <V> Attribute<V>
        of(Class<V> type, Supplier<? extends V> defaultValueSupplier) {
        return new Attribute<>(type, defaultValueSupplier);
    }

    /**
     * Create an attribute key with no default value.
     * 
     * @param <V> the attribute value type
     * 
     * @param type the attribute value type
     * 
     * @return the required key
     * 
     * @constructor
     */
    public static <V> Attribute<V> of(Class<V> type) {
        return of(type, (Supplier<? extends V>) () -> null);
    }

    V get(Map<? super Attribute<?>, Object> map) {
        Object value = map.get(this);
        if (value == null) return defaultValue.get();
        return type.cast(value);
    }

    /**
     * Indicates how many concurrent connections the application will
     * accept. Unset to handle unlimited connections. This value is sent
     * to the server as <code>FCGI_MAX_CONNS</code> if set, so the
     * server should not open more connections than this.
     */
    public static Attribute<Integer> MAX_CONN = of(Integer.class);

    /**
     * Indicates how many concurrent sessions will be handled per
     * connection. Set to 1 to turn off multiplexing; the library will
     * send <code>FCGI_MPXS_CONNS=0</code>. Unset to handle unlimited
     * sessions. Otherwise, the library will abort further requests with
     * <code>FCGI_OVERLOADED</code> if it already has the specified
     * number open.
     */
    public static Attribute<Integer> MAX_SESS_PER_CONN = of(Integer.class);

    /**
     * Indicates how many concurrent sessions across all connections an
     * application will accept. This value is sent to the server as
     * <code>FCGI_MAX_REQS</code> if set, so the server should queue
     * other sessions when the maximum is reached.
     */
    public static Attribute<Integer> MAX_SESS = of(Integer.class);

    /**
     * Specifies the implementation that handles full requests.
     */
    public static Attribute<Responder> RESPONDER = of(Responder.class);

    /**
     * Specifies the implementation that authorizes requests, or
     * provides responses indicating why not.
     */
    public static Attribute<Authorizer> AUTHORIZER = of(Authorizer.class);

    /**
     * Specifies the implementation that post-processes responses.
     */
    public static Attribute<Filter> FILTER = of(Filter.class);

    /**
     * Specifies the initial buffer size for standard output.
     */
    public static final Attribute<Integer> BUFFER_SIZE =
        of(Integer.class, 1024);
}
