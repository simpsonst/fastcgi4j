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

package uk.ac.lancs.fastcgi.engine.std;

import java.nio.charset.Charset;
import java.util.function.Function;
import uk.ac.lancs.fastcgi.Authorizer;
import uk.ac.lancs.fastcgi.Filter;
import uk.ac.lancs.fastcgi.Responder;
import uk.ac.lancs.fastcgi.engine.Attribute;
import uk.ac.lancs.fastcgi.conn.ConnectionSupply;
import uk.ac.lancs.fastcgi.engine.Engine;
import uk.ac.lancs.fastcgi.engine.EngineConfiguration;
import uk.ac.lancs.fastcgi.engine.EngineFactory;
import uk.ac.lancs.scc.jardeps.Service;

/**
 * Provides engines which can handle responders, authorizers and
 * filters, supporting multiplexed sessions on each connection. This
 * implementation reads the attributes {@link Attribute#RESPONDER},
 * {@link Attribute#AUTHORIZER}, {@link Attribute#FILTER},
 * {@link Attribute#MAX_CONN} (must be non-positive if set),
 * {@link Attribute#MAX_SESS} (non-positive if set) and
 * {@link Attribute#MAX_SESS_PER_CONN} (non-positive if set). All are
 * optional, except that at least one role must be specified. The
 * attribute {@link Attribute#BUFFER_SIZE} is also read, and is not
 * optional.
 * 
 * @author simpsons
 */
@Service(EngineFactory.class)
public class MultiplexGenericEngineFactory implements EngineFactory {
    @Override
    public Function<? super ConnectionSupply, ? extends Engine>
        test(EngineConfiguration config) {
        /* Identify the roles. */
        Responder responder = config.get(Attribute.RESPONDER);
        Authorizer authorizer = config.get(Attribute.AUTHORIZER);
        Filter filter = config.get(Attribute.FILTER);
        if (responder == null && authorizer == null && filter == null)
            return null;

        /* Get and validate performance parameters. */
        Integer maxConn = config.get(Attribute.MAX_CONN);
        Integer maxSess = config.get(Attribute.MAX_SESS);
        Integer maxSessPerConn = config.get(Attribute.MAX_SESS_PER_CONN);
        if (maxConn != null && maxConn < 1) return null;
        if (maxSess != null && maxSess < 1) return null;
        if (maxSessPerConn != null && maxSessPerConn < 1) return null;
        int outBufSize = config.get(Attribute.BUFFER_SIZE);
        if (outBufSize < 0) return null;

        /* Create the factory for creating the engine from a connection
         * supply. */
        return cs -> new MultiplexGenericEngine(cs, Charset.defaultCharset(),
                                                responder, authorizer, filter,
                                                maxConn != null ? maxConn : 0,
                                                maxSessPerConn != null ?
                                                    maxSessPerConn : 0,
                                                maxSess != null ? maxSess : 0,
                                                outBufSize, 1024 * 1);
    }
}
