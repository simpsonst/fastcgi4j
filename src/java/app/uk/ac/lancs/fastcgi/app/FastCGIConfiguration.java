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

package uk.ac.lancs.fastcgi.app;

import uk.ac.lancs.fastcgi.role.Authorizer;
import uk.ac.lancs.fastcgi.role.Filter;
import uk.ac.lancs.fastcgi.role.Responder;

/**
 * Allows a FastCGI application to declare its capabilities.
 * 
 * @author simpsons
 */
public interface FastCGIConfiguration {
    /**
     * Set the Responder behaviour. If not set, the application may
     * implement {@link Responder} itself. Calling this method after
     * returning from
     * {@link FastCGIApplication#init(FastCGIConfiguration, String[])}
     * has no effect.
     * 
     * @param app the Responder behaviour
     */
    void setResponder(Responder app);

    /**
     * Set the Authorizer behaviour. If not set, the application may
     * implement {@link Authorizer} itself. Calling this method after
     * returning from
     * {@link FastCGIApplication#init(FastCGIConfiguration, String[])}
     * has no effect.
     * 
     * @param app the Authorizer behaviour
     */
    void setAuthorizer(Authorizer app);

    /**
     * Set the Filter behaviour. If not set, the application may
     * implement {@link Filter} itself. Calling this method after
     * returning from
     * {@link FastCGIApplication#init(FastCGIConfiguration, String[])}
     * has no effect.
     * 
     * @param app the Filter behaviour
     */
    void setFilter(Filter app);
}
