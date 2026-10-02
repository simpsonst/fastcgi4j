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
 *  Author: Steven Simpson <s.simpson@lancaster.ac.uk>
 */

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.ac.lancs.fastcgi.Responder;
import uk.ac.lancs.fastcgi.engine.Attribute;
import uk.ac.lancs.fastcgi.engine.Engine;
import uk.ac.lancs.fastcgi.transport.Transport;
import uk.ac.lancs.scc.jardeps.Application;

/**
 * Provides a responder that yields MD5 sums of the request content.
 * 
 * @author simpsons
 */
@Application
public class MD5SumApp {
    @SuppressWarnings("empty-statement")
    public static void main(String[] args) throws Exception {
        Collection<? extends Transport> transports = Transport.get();
        Responder rsper = new MD5SumResponder();
        var maker = Engine.start().with(Attribute.MAX_CONN, 10)
            .with(Attribute.MAX_SESS_PER_CONN, 10)
            .with(Attribute.RESPONDER, rsper).build();

        final List<Future<Void>> results;
        try (ExecutorService exec =
            Executors.newVirtualThreadPerTaskExecutor()) {
            results = exec.invokeAll(transports.stream()
                .map(mapToEngineExhaust(maker)).collect(Collectors.toList()));
        }
        for (var r : results)
            r.get();
    }

    private static Function<Transport, Callable<Void>>
        mapToEngineExhaust(Function<? super Transport,
                                    ? extends Engine> maker) {
        return t -> exhaustEngine(maker, t);
    }

    private static Callable<Void>
        exhaustEngine(Function<? super Transport, ? extends Engine> maker,
                      Transport t) {
        Engine engine = maker.apply(t);
        return () -> {
            while (engine.process())
                ;
            return null;
        };
    }
}
