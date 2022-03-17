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

/**
 * Defines the top-level classes used by applications and met by engine
 * implementations of FastCGI.
 * 
 * <p>
 * An application should first obtain a
 * {@link uk.ac.lancs.fastcgi.transport.Transport}. If this fails with
 * {@link UnsupportedOperationException}, then the application is not
 * being invoked using FastCGI, or not using a supported transport.
 * 
 * <p>
 * The application should then instantiate a role implementation, such
 * as {@link uk.ac.lancs.fastcgi.role.Responder},
 * {@link uk.ac.lancs.fastcgi.role.Authorizer} or
 * {@link uk.ac.lancs.fastcgi.role.Filter}. It may provide one of each.
 * 
 * <p>
 * An engine can be built by specifying various required and optional
 * attributes, using {@link Engine.Builder#with(Attribute, Object)} and
 * {@link Engine.Builder#trying(Attribute, Object)}, and then submitting
 * the connection supply to it.
 * 
 * <p>
 * Finally, the engine should be invoked repeatedly until it indicates
 * otherwise. The application should then shutdown gracefully.
 * 
 * <pre>
 * Transport transport = Transport.get();
 * Responder myResponder = <var>...</var>;
 * Engine engine = Engine.start()
 *     .with(Attribute.MAX_CONN, 10)
 *     .with(Attribute.RESPONDER, myResponder)
 *     .build()
 *     .apply(transport);
 * while (engine.process())
 *   ;
 * </pre>
 */
package uk.ac.lancs.fastcgi.engine;

