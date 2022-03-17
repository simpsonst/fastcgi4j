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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Function;
import uk.ac.lancs.fastcgi.transport.Transport;

/**
 * Processes FastCGI requests from the server and responses from the
 * application.
 * 
 * @author simpsons
 */
public interface Engine {
    /**
     * Create a builder with no attributes.
     * 
     * @return the new builder
     * 
     * @constructor
     */
    public static Builder start() {
        return new Builder();
    }

    /**
     * Builds an engine from a set of required attributes.
     */
    public final static class Builder {
        private Builder() {}

        private final Map<Attribute<?>, Object> attrs = new HashMap<>();

        private final Map<Attribute<?>, Object> optAttrs = new HashMap<>();

        private Properties props;

        /**
         * Specify the properties from which attributes shall be
         * extracted.
         * 
         * @see #withProperty(Attribute, String)
         * 
         * @see #tryingProperty(Attribute, String)
         * 
         * @param <V> the attribute value type
         * 
         * @param props the properties from which attributes will be
         * extracted
         * 
         * @return this builder
         */
        public <V> Builder using(Properties props) {
            this.props = props;
            return this;
        }

        /**
         * Include a required attribute of the engine from properties.
         * The property value is extracted from the set provided to the
         * most call to {@link #using(Properties)}. If the property is
         * not defined, no value is set.
         * 
         * @param <V> the attribute value type
         * 
         * @param key the attribute key
         * 
         * @param propName the property name
         * 
         * @return this builder
         */
        public <V> Builder withProperty(Attribute<V> key, String propName) {
            V value = key.parse(props, propName);
            if (value == null) return this;
            return with(key, value);
        }

        /**
         * Include a preferred attribute of the engine from properties.
         * The property value is extracted from the set provided to the
         * most call to {@link #using(Properties)}. If the property is
         * not defined, no value is set.
         * 
         * @param <V> the attribute value type
         * 
         * @param key the attribute key
         * 
         * @param propName the property name
         * 
         * @return this builder
         */
        public <V> Builder tryingProperty(Attribute<V> key, String propName) {
            V value = key.parse(props, propName);
            if (value == null) return this;
            return trying(key, value);
        }

        /**
         * Include a required attribute of the engine.
         * 
         * @param <V> the attribute value type
         * 
         * @param key the attribute key
         * 
         * @param value the attribute value
         * 
         * @return this builder
         */
        public <V> Builder with(Attribute<V> key, V value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            attrs.put(key, value);
            return this;
        }

        /**
         * Include a required attribute of the engine.
         * 
         * @param <V> the attribute value type
         * 
         * @param key the attribute key
         * 
         * @param text the text from which a value shall be parsed
         * 
         * @return this builder
         */
        public <V> Builder withText(Attribute<V> key, String text) {
            V value = key.parse(text);
            return with(key, value);
        }

        /**
         * Include a preferred attribute of the engine.
         * 
         * @param <V> the attribute value type
         * 
         * @param key the attribute key
         * 
         * @param value the attribute value
         * 
         * @return this builder
         */
        public <V> Builder trying(Attribute<V> key, V value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            optAttrs.put(key, value);
            return this;
        }

        /**
         * Include a preferred attribute of the engine.
         * 
         * @param <V> the attribute value type
         * 
         * @param key the attribute key
         * 
         * @param text the text from which a value shall be parsed
         * 
         * @return this builder
         */
        public <V> Builder tryingText(Attribute<V> key, String text) {
            V value = key.parse(text);
            return trying(key, value);
        }

        /**
         * Exclude an attribute neither required nor preferred of the
         * engine.
         * 
         * @param key the attribute key
         * 
         * @return this builder
         */
        public Builder without(Attribute<?> key) {
            optAttrs.remove(key);
            attrs.remove(key);
            return this;
        }

        /**
         * Create an engine with the current configuration from the
         * context class loader of the calling thread.
         * 
         * @return the new engine
         * 
         * @throws EngineConfigurationException if no engine could be
         * found with the specified attributes
         * 
         * @throws UnsupportedOperationException if no connection
         * supplier could be found
         */
        public Function<? super Transport, ? extends Engine> build() {
            ClassLoader ctxtLoader =
                Thread.currentThread().getContextClassLoader();
            return build(ctxtLoader);
        }

        /**
         * Create an engine with the current configuration from a given
         * class loader.
         * 
         * @param loader the class loader to be used to load
         * provider-configuration files and provider classes for
         * {@link EngineFactory}, or {@code null} if the system class
         * loader (or, failing that, the bootstrap class loader) is to
         * be used
         * 
         * @return the new engine
         * 
         * @throws EngineConfigurationException if no engine could be
         * found with the specified attributes
         * 
         * @throws UnsupportedOperationException if no connection
         * supplier could be found
         */
        public Function<? super Transport, ? extends Engine>
            build(ClassLoader loader) {
            class MyConfig implements EngineConfiguration {
                @Override
                public <V> V get(Attribute<V> key) {
                    unchecked.remove(key);
                    return key.get(attrs);
                }

                @Override
                public <V> V getPreferred(Attribute<V> key) {
                    V value = key.get(optAttrs);
                    if (value != null) return value;
                    return key.get(attrs);
                }

                Collection<Attribute<?>> unchecked = new HashSet<>();

                void reset() {
                    unchecked.addAll(attrs.keySet());
                }
            }
            MyConfig config = new MyConfig();

            for (EngineFactory factory : ServiceLoader.load(EngineFactory.class,
                                                            loader)) {
                /* See if this factory can meet the required
                 * configuration. */
                config.reset();
                var sup = factory.test(config);
                if (sup == null) continue;
                if (!config.unchecked.isEmpty()) continue;

                return sup;
            }

            throw new EngineConfigurationException(config);
        }
    }

    /**
     * Perform some unit of processing.
     * 
     * @return {@code true} if more processing is expected;
     * {@code false} if the application should terminate
     * 
     * @throws IOException if an I/O error occurs
     */
    boolean process() throws IOException;
}
