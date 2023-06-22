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
 *  Author: Steven Simpson <s.simpson@lancaster.ac.uk>
 */

package uk.ac.lancs.fastcgi.engine.std;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import uk.ac.lancs.fastcgi.Authorizer;
import uk.ac.lancs.fastcgi.Filter;
import uk.ac.lancs.fastcgi.Responder;
import uk.ac.lancs.fastcgi.engine.Engine;
import uk.ac.lancs.fastcgi.engine.std.threading.ThreadingManager;
import uk.ac.lancs.fastcgi.engine.util.CachePipePool;
import uk.ac.lancs.fastcgi.engine.util.Pipe;
import uk.ac.lancs.fastcgi.proto.ApplicationVariables;
import uk.ac.lancs.fastcgi.proto.ProtocolStatuses;
import uk.ac.lancs.fastcgi.proto.RequestFlags;
import uk.ac.lancs.fastcgi.proto.RoleTypes;
import uk.ac.lancs.fastcgi.proto.serial.RecordHandler;
import uk.ac.lancs.fastcgi.proto.serial.RecordReader;
import uk.ac.lancs.fastcgi.proto.serial.RecordWriter;
import uk.ac.lancs.fastcgi.transport.Connection;
import uk.ac.lancs.fastcgi.transport.Transport;

/**
 * Handles FastCGI records and delivers to all role types, supporting
 * multiple connections and multiple sessions per connection.
 *
 * @author simpsons
 */
class MultiplexGenericEngine implements Engine {
    private final Charset charset;

    private final Transport connections;

    private final ThreadingManager threading;

    private final Responder responder;

    private final Authorizer authorizer;

    private final Filter filter;

    private final int maxConns;

    private final int maxReqsPerConn;

    private final int maxReqs;

    private final int stderrBufferSize;

    private final int stdoutBufferSize;

    private final Supplier<? extends Pipe> pipes =
        CachePipePool.start().create()::newPipe;

    private final ThreadGroup conntg = new ThreadGroup("connections");

    /**
     * Create an engine.
     * 
     * @param connections the supply of connections
     * 
     * @param threading a means to create thread-related resources
     * 
     * @param charset the character encoding for handling parameters
     * from the server, application variable names and values, and
     * response headers
     * 
     * @param responder the object to handle responder requests; or
     * {@code null} if not required
     * 
     * @param authorizer the object to handle authorizer requests; or
     * {@code null} if not required
     * 
     * @param filter the object to handle filter requests; or
     * {@code null} if not required
     * 
     * @param maxConns the maximum number of connections to offer to the
     * server; or {@code null} if unlimited
     * 
     * @param maxReqsPerConn the maximum number of requests to handle
     * simultaneously per connection; or {@code null} if unlimited
     * 
     * @param maxReqs the maximum number of simultaneous requests to
     * offer to the server; or {@code null} if unlimited
     * 
     * @param stdoutBufferSize the default output buffer size
     * 
     * @param stderrBufferSize the standard error output buffer size
     */
    public MultiplexGenericEngine(Transport connections,
                                  ThreadingManager threading, Charset charset,
                                  Responder responder, Authorizer authorizer,
                                  Filter filter, int maxConns,
                                  int maxReqsPerConn, int maxReqs,
                                  int stdoutBufferSize, int stderrBufferSize) {
        this.connections = connections;
        this.threading = threading;
        this.charset = charset;
        this.responder = responder;
        this.authorizer = authorizer;
        this.filter = filter;
        this.maxConns = maxConns;
        this.maxReqs = maxReqs;
        this.maxReqsPerConn = maxReqsPerConn;
        this.stdoutBufferSize = stdoutBufferSize;
        this.stderrBufferSize = stderrBufferSize;
        this.connExecutor = threading.newConnectionExecutor(conntg);
    }

    private final Executor connExecutor;

    private final BufferPool paramBufs = new BufferPool();

    @Override
    public boolean process() throws IOException {
        Connection conn = connections.nextConnection();
        if (conn == null) return false;
        ConnHandler ch = new ConnHandler(conn);
        connExecutor.execute(ch);
        return true;
    }

    private final AtomicInteger connIds = new AtomicInteger(0);

    private static int optimizeBufferSize(int requested, int recommended,
                                          int alignment) {
        final int chosenAlignment;
        if (requested < recommended * 2) {
            /* Just round it up to the alignment. */
            chosenAlignment = alignment;
        } else {
            chosenAlignment = recommended;
        }

        /* Round up to a multiple of the recommended. */

        /* Round it up to the alignment. */
        final int tmp = requested + (chosenAlignment - 1);
        return tmp - tmp % chosenAlignment;
    }

    private class ConnHandler implements Runnable, RecordHandler {
        private final int id = connIds.getAndIncrement();

        private final ThreadGroup sesstg =
            new ThreadGroup(conntg, "sessions-" + id);

        private final Connection conn;

        private final RecordReader recordsIn;

        private final RecordWriter recordsOut;

        private final Executor executor;

        private final int optimizedBufferSize;

        public ConnHandler(Connection conn) throws IOException {
            this.conn = conn;
            this.recordsIn =
                new RecordReader(conn.input(), charset, this, "conn-" + id);
            this.recordsOut =
                new RecordWriter(conn.output(), charset, "conn-" + id);
            this.optimizedBufferSize =
                optimizeBufferSize(stdoutBufferSize,
                                   this.recordsOut.optimumPayloadLength(),
                                   this.recordsOut.alignment());
            this.executor = threading.newSessionExceutor(sesstg);
            logger.info(() -> msg("on %s/%s", conn.description(),
                                  conn.internalDescription()));
        }

        private final Map<Integer, SessionHandler> sessions =
            new ConcurrentHashMap<>();

        private volatile boolean keepGoing = true;

        @Override
        public void run() {
            Thread.currentThread().setName("fastcgi4j-ct-" + id);
            try {
                while ((keepGoing || !sessions.isEmpty()) &&
                    recordsIn.processRecord()) {
                    /* All work is done in processRecords(). */
                }
                conn.close();
            } catch (IOException ex) {
                if (keepGoing || !sessions.isEmpty()) {
                    /* There was an error reading to or writing from the
                     * connection. */
                    logger.severe(() -> msg("I/O error: %s", ex.getMessage()));
                }
            } finally {
                logger.fine(() -> msg("closed"));
                Thread.currentThread().setName("fastcgi4j-ct-unused");
            }
        }

        @Override
        public void getValues(Collection<? extends String> names)
            throws IOException {
            /* Pick out values we recognize and have settings for, build
             * a table, and write it out. */
            Map<String, String> outMap = new HashMap<>();

            if (names.contains(ApplicationVariables.MAX_CONNS)) {
                if (maxConns >= 1) outMap.put(ApplicationVariables.MAX_CONNS,
                                              Integer.toString(maxConns));
            }

            if (names.contains(ApplicationVariables.MAX_REQS)) {
                if (maxReqs >= 1) outMap.put(ApplicationVariables.MAX_REQS,
                                             Integer.toString(maxReqs));
            }

            if (names.contains(ApplicationVariables.MPXS_CONNS)) {
                if (maxReqsPerConn == 1)
                    outMap.put(ApplicationVariables.MPXS_CONNS, "1");
                else
                    outMap.put(ApplicationVariables.MPXS_CONNS, "0");
            }

            recordsOut.writeValues(outMap);
        }

        @Override
        public void beginRequest(int id, int role, int flags)
            throws IOException {
            if (!keepGoing) {
                /* We're not accepting new requests on this connection.
                 * TODO: There really needs to be more status codes. */
                logger.warning(() -> msg(
                                         "rejecting request %d"
                                             + " as we weren't expecting more",
                                         id));
                recordsOut.writeEndRequest(id, -3, ProtocolStatuses.OVERLOADED);
                return;
            }

            if ((flags & RequestFlags.KEEP_CONN) == 0) {
                /* The server tells us we're not going to use this
                 * connection any more for new requests. */
                logger.info(() -> msg("ack last request %d", id));
                keepGoing = false;
                /* Proceed with this one, though! */
            }

            /* Detect temporary overload based on the configured maximum
             * requests per connection. */
            if (maxReqsPerConn > 0) {
                final int sz = sessions.size();
                if (sz >= maxReqsPerConn) {
                    logger.warning(() -> msg("rejecting request %d;"
                        + " %d/%d sessions as overloaded", id, sz,
                                             maxReqsPerConn));
                    recordsOut
                        .writeEndRequest(id, -3,
                                         maxReqsPerConn == 1 ?
                                             ProtocolStatuses.CANT_MPX_CONN :
                                             ProtocolStatuses.OVERLOADED);
                    return;
                }
            }

            /* Package components required by all roles. */
            Supplier<HandlerContext> ctxt =
                () -> new HandlerContext(this.id, id, conn.implementation(),
                                         conn.description(),
                                         conn.internalDescription(),
                                         this::abortConnection,
                                         h -> sessions.remove(id, h),
                                         this::checkLastCall, recordsOut,
                                         executor, charset, paramBufs,
                                         optimizedBufferSize, stderrBufferSize);

            /* Create the session if there isn't one with the specified
             * id, and the role type is recognized. */
            Function<Integer, SessionHandler> handlerMaker = k -> {
                switch (role) {
                case RoleTypes.RESPONDER:
                    if (responder == null) return null;
                    return new ResponderHandler(ctxt.get(), responder,
                                                pipes.get());

                case RoleTypes.FILTER:
                    if (filter == null) return null;
                    return new FilterHandler(ctxt.get(), filter, pipes.get(),
                                             pipes.get());

                case RoleTypes.AUTHORIZER:
                    if (authorizer == null) return null;
                    return new AuthorizerHandler(ctxt.get(), authorizer);

                default:
                    /* No mapping is to be recorded. */
                    return null;
                }
            };
            SessionHandler sess = sessions.computeIfAbsent(id, handlerMaker);

            if (sess == null) {
                /* No mapping was recorded, meaning that we don't
                 * recognize the role. */
                logger.warning(() -> msg("bad role %s for %d",
                                         ProtocolStatuses.toString(role), id));
                assert !sessions.containsKey(id);
                recordsOut.writeEndRequest(id, -3,
                                           ProtocolStatuses.UNKNOWN_ROLE);
                return;
            }

            if (sess.start()) {
                /* There must be no existing session. */
                keepGoing = false;
                logger
                    .severe(() -> msg("server began existing request %d", id));
            } else {
                logger.fine(() -> msg("begun request %d", id));
            }
        }

        private void checkLastCall() {
            if (!keepGoing && sessions.isEmpty()) {
                try {
                    conn.close();
                } catch (IOException ex) {
                    logger.warning(() -> msg("I/O on close: %s",
                                             ex.getMessage()));
                }
            }
        }

        @Override
        public void abortRequest(int id) throws IOException {
            SessionHandler sess = sessions.get(id);
            if (sess != null) sess.abortRequest();
        }

        @Override
        public void params(int id, int len, InputStream in) throws IOException {
            SessionHandler sess = sessions.get(id);
            if (sess != null) sess.params(len, in);
        }

        @Override
        public void paramsEnd(int id) throws IOException {
            SessionHandler sess = sessions.get(id);
            if (sess != null) sess.paramsEnd();
        }

        @Override
        public void stdin(int id, int len, InputStream in) throws IOException {
            SessionHandler sess = sessions.get(id);
            if (sess != null) sess.stdin(len, in);
        }

        @Override
        public void stdinEnd(int id) throws IOException {
            SessionHandler sess = sessions.get(id);
            if (sess != null) sess.stdinEnd();
        }

        @Override
        public void data(int id, int len, InputStream in) throws IOException {
            SessionHandler sess = sessions.get(id);
            if (sess != null) sess.data(len, in);
        }

        @Override
        public void dataEnd(int id) throws IOException {
            SessionHandler sess = sessions.get(id);
            if (sess != null) sess.dataEnd();
        }

        @Override
        public void bad(int reasons, int version, int type, int length, int id)
            throws IOException {
            if ((reasons & RecordHandler.UNKNOWN_TYPE) != 0) {
                recordsOut.writeUnknownType(type);
                return;
            }
            if (id == 0) return;
            SessionHandler sess = sessions.get(id);
            if (sess != null) sess.abortRequest();
        }

        private void abortConnection() {
            keepGoing = false;
            logger.warning(() -> msg("aborting"));
            sessions.forEach((k, v) -> v
                .transportFailure(new IOException("transport failure")));
        }

        private String msg(String fmt, Object... args) {
            return "conn-" + id + ":"
                + MultiplexGenericEngine.this.msg(fmt, args);
        }
    }

    private String msg(String fmt, Object... args) {
        return String.format(fmt, args);
    }

    private static final Logger logger =
        Logger.getLogger(MultiplexGenericEngine.class.getPackageName());
}
