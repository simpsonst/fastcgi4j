/*
 * Copyright (c) 2022, Lancaster University
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
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.lancs.fastcgi.Authorizer;
import uk.ac.lancs.fastcgi.Filter;
import uk.ac.lancs.fastcgi.Responder;

/**
 * Identifies a typed attribute that an engine is required or preferred
 * to have. An attribute has a value type, an optional default value or
 * supplier of such, and an optional parser.
 * 
 * <p>
 * A number of common attributes are defined in this class. Specific
 * engine implementations may define more.
 * 
 * @param <V> the value type
 * 
 * @see Engine.Builder#with(Attribute, Object)
 * 
 * @see Engine.Builder#trying(Attribute, Object)
 * 
 * @author simpsons
 */
public final class Attribute<V> {
    private final Class<V> type;

    private final Supplier<? extends V> defaultValueSupplier;

    private final Function<? super String, ? extends V> parser;

    private Attribute(Class<V> type, Supplier<? extends V> defaultValueSupplier,
                      Function<? super String, ? extends V> parser) {
        assert type != null;
        this.type = type;
        this.defaultValueSupplier = defaultValueSupplier;
        this.parser = parser;
    }

    /**
     * Prepare to define an attribute.
     * 
     * @param <V> the attribute value type
     * 
     * @param type the attribute value type
     * 
     * @return a builder which can take on other properties
     * 
     * @constructor
     */
    public static <V> Builder<V> of(Class<V> type) {
        return new Builder<>(type);
    }

    /**
     * Prepare to define an integer attribute. The value type is
     * {@link Integer}, and {@link Integer#parseInt(String)} is set as
     * the parser.
     * 
     * @return a builder which can take on other properties
     * 
     * @constructor
     */
    public static Builder<Integer> ofInt() {
        return of(Integer.class).withParser(Integer::parseInt);
    }

    /**
     * Prepare to define a string attribute. The value type is
     * {@link String}, and an identity function is set as the parser.
     * 
     * @return a builder which can take on other properties
     * 
     * @constructor
     */
    public static Builder<String> ofString() {
        return of(String.class).withParser(s -> s);
    }

    /**
     * Allows an attribute to be defined in stages.
     * 
     * @param <V> the attribute value type
     */
    public static class Builder<V> {
        final Class<V> type;

        Supplier<? extends V> defaultValueSupplier = null;

        Function<? super String, ? extends V> parser = null;

        Builder(Class<V> type) {
            this.type = type;
        }

        /**
         * Specify the parser for the attribute.
         * 
         * @param parser a function parsing a string as the value type
         * 
         * @return this object
         */
        public Builder<V>
            withParser(Function<? super String, ? extends V> parser) {
            this.parser = parser;
            return this;
        }

        /**
         * Set the supplier of the default value of the new attribute.
         * 
         * @param defaultValueSupplier a supplier for the default
         * attribute value
         * 
         * @return this object
         */
        public Builder<V>
            withDefault(Supplier<? extends V> defaultValueSupplier) {
            this.defaultValueSupplier = defaultValueSupplier;
            return this;
        }

        /**
         * Set the default value.
         * 
         * @param defaultValue the default value
         * 
         * @return this object
         */
        public Builder<V> withDefault(V defaultValue) {
            return withDefault(() -> defaultValue);
        }

        /**
         * Define an attribute with the current properties.
         * 
         * @return the new attribute
         * 
         * @constructor
         */
        public Attribute<V> define() {
            return new Attribute<>(type, defaultValueSupplier, parser);
        }
    }

    V get(Map<? super Attribute<?>, Object> map) {
        Object value = map.get(this);
        if (value != null) return type.cast(value);
        if (defaultValueSupplier == null) return null;
        return defaultValueSupplier.get();
    }

    V parse(String text) {
        if (text != null) return parser.apply(text);
        if (defaultValueSupplier == null) return null;
        return defaultValueSupplier.get();
    }

    V parse(Properties props, String name) {
        return parse(props.getProperty(name));
    }

    /**
     * Indicates how many concurrent connections the application will
     * accept. Unset to handle unlimited connections. This value is sent
     * to the server as <code>FCGI_MAX_CONNS</code> if set, so the
     * server should not open more connections than this.
     */
    public static final Attribute<Integer> MAX_CONN = ofInt().define();

    /**
     * Indicates how many concurrent sessions will be handled per
     * connection. Set to 1 to turn off multiplexing; the library will
     * send <code>FCGI_MPXS_CONNS=0</code>. Unset to handle unlimited
     * sessions. Otherwise, the library will abort further requests with
     * <code>FCGI_OVERLOADED</code> if it already has the specified
     * number open.
     */
    public static final Attribute<Integer> MAX_SESS_PER_CONN = ofInt().define();

    /**
     * Indicates how many concurrent sessions across all connections an
     * application will accept. This value is sent to the server as
     * <code>FCGI_MAX_REQS</code> if set, so the server should queue
     * other sessions when the maximum is reached.
     */
    public static final Attribute<Integer> MAX_SESS = ofInt().define();

    /**
     * Specifies the implementation that handles full requests.
     */
    public static final Attribute<Responder> RESPONDER =
        of(Responder.class).define();

    /**
     * Specifies the implementation that authorizes requests, or
     * provides responses indicating why not.
     */
    public static final Attribute<Authorizer> AUTHORIZER =
        of(Authorizer.class).define();

    /**
     * Specifies the implementation that post-processes responses.
     */
    public static final Attribute<Filter> FILTER = of(Filter.class).define();

    /**
     * Specifies the initial buffer size for standard output.
     */
    public static final Attribute<Integer> BUFFER_SIZE = of(Integer.class)
        .withParser(Attribute::parseMemCap).withDefault(1024).define();

    private static final Pattern MEMCAP_PATTERN =
        Pattern.compile("^([0-9]+)([kKmMgG])?");

    private static int parseMemCap(String text) {
        Matcher m = MEMCAP_PATTERN.matcher(text);
        if (!m.matches()) throw new NumberFormatException(text);
        int base = Integer.parseInt(m.group(1));
        String ptxt = m.group(2);
        int power =
            ptxt == null ? 0 : (1 + "kKmKgG".indexOf(ptxt.charAt(0)) / 2);
        while (power > 0) {
            base *= 1024;
            power--;
        }
        return base;
    }

    /**
     * Specifies whether soft threads should be used for all
     * connections, regardless of the maximum number of connections. The
     * default is {@code true}. However, hard threads will be used if
     * soft threads are unavailable (pre-JDK19).
     */
    public static final Attribute<Boolean> SOFT_CONN_THREAD =
        Attribute.of(Boolean.class).withDefault(Boolean.TRUE)
            .withParser(Boolean::valueOf).define();

    /**
     * Specifies whether soft threads should be used for all sessions,
     * regardless of the maximum number of sessions per connection. The
     * default is {@code true}. However, hard threads will be used if
     * soft threads are unavailable (pre-JDK19).
     */
    public static final Attribute<Boolean> SOFT_SESS_THREAD =
        Attribute.of(Boolean.class).withDefault(Boolean.TRUE)
            .withParser(Boolean::valueOf).define();
}
