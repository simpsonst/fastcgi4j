// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2022,2023, Lancaster University
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

package uk.ac.lancs.fastcgi.engine.std.threading.soft;

import uk.ac.lancs.fastcgi.engine.Attribute;
import uk.ac.lancs.fastcgi.engine.EngineConfiguration;
import uk.ac.lancs.fastcgi.engine.std.threading.ThreadingManager;
import uk.ac.lancs.fastcgi.engine.std.threading.ThreadingManagerFactory;
import uk.ac.lancs.scc.jardeps.Service;

/**
 * Allows the use of soft threads. Soft threads are also known as
 * virtual or lightweight threads, as introduced in JDK19. If
 * {@link Attribute#MAX_CONN} is not set, or
 * {@link Attribute#SOFT_CONN_THREAD} is {@code true} (the default),
 * connections use virtual threads. If
 * {@link Attribute#MAX_SESS_PER_CONN} is not set, or
 * {@link Attribute#SOFT_SESS_THREAD} is {@code true} (the default),
 * sessions use virtual threads.
 * 
 * @author simpsons
 */
@Service(ThreadingManagerFactory.class)
public class SoftThreadingManagerFactory implements ThreadingManagerFactory {
    @Override
    public ThreadingManager getManager(EngineConfiguration config) {
        Integer maxConnObj = config.get(Attribute.MAX_CONN);
        Integer maxSessPerConnObj = config.get(Attribute.MAX_SESS_PER_CONN);
        Boolean allSess = config.get(Attribute.SOFT_SESS_THREAD);
        Boolean allConn = config.get(Attribute.SOFT_CONN_THREAD);
        return new SoftThreadingManager(maxConnObj == null ? 0 : maxConnObj,
                                        maxSessPerConnObj == null ? 0 :
                                            maxSessPerConnObj,
                                        allConn, allSess);
    }
}
