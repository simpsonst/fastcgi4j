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

package uk.ac.lancs.fastcgi.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

/**
 * Creates proxies for auto-closeable objects, and intercepts the close
 * method, delivering the receiver to a configured consumer.
 * 
 * @author simpsons
 */
class AutoCloseableProxifier<T extends AutoCloseable,
                             C extends Consumer<? super T>> {
    private final Class<T> type;

    private final ClassLoader loader;

    private final Method closeMethod, toStringMethod, equalsMethod,
        hashCodeMethod;

    private final Class<?>[] types;

    private final C consumer;

    /**
     * Create a proxifier.
     * 
     * @param type the proxied type
     * 
     * @param consumer an action to perform on the receiver when
     * {@link AutoCloseable#close()} is invoked
     */
    public AutoCloseableProxifier(Class<T> type, C consumer) {
        this.type = type;
        this.types = new Class<?>[] { type };
        this.consumer = consumer;
        this.loader = type.getClassLoader();
        try {
            closeMethod = type.getMethod("close");
            toStringMethod = type.getMethod("toString");
            hashCodeMethod = type.getMethod("hashCode");
            equalsMethod =
                type.getMethod("equals", new Class<?>[]
                { Object.class });
        } catch (NoSuchMethodException ex) {
            throw new AssertionError("unreachable", ex);
        }
    }

    /**
     * Create a proxy. All methods are passed through to the receiver,
     * except the following:
     * 
     * <ul>
     * 
     * <li>{@link Object#equals(Object)} &mdash; The argument is tested
     * for identity with the proxy.
     * 
     * <li>{@link Object#hashCode()} &mdash;
     * {@link System#identityHashCode(Object)} is invoked on the proxy
     * instead.
     * 
     * <li>{@link AutoCloseable#close()} &mdash; The receiver is
     * delivered to the consumer.
     * 
     * </ul>
     * 
     * @param receiver the receiver object
     * 
     * @return a proxy for the receiver
     */
    public T proxy(T receiver) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method == closeMethod) {
                consumer.accept(receiver);
                return null;
            } else if (method == toStringMethod) {
                return receiver.toString();
            } else if (method == equalsMethod) {
                return args[0] == proxy;
            } else if (method == hashCodeMethod) {
                return System.identityHashCode(proxy);
            } else {
                return method.invoke(receiver, args);
            }
        };
        Object proxy = Proxy.newProxyInstance(loader, types, handler);
        return type.cast(proxy);
    }
}
