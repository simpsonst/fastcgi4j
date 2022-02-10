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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.lancs.fastcgi.Authorizer;
import uk.ac.lancs.fastcgi.AuthorizerContext;
import uk.ac.lancs.fastcgi.Filter;
import uk.ac.lancs.fastcgi.FilterContext;
import uk.ac.lancs.fastcgi.OverloadException;
import uk.ac.lancs.fastcgi.Responder;
import uk.ac.lancs.fastcgi.ResponderContext;
import uk.ac.lancs.fastcgi.SessionContext;
import uk.ac.lancs.fastcgi.conn.Connection;
import uk.ac.lancs.fastcgi.conn.ConnectionSupply;
import uk.ac.lancs.fastcgi.engine.Engine;
import uk.ac.lancs.fastcgi.engine.util.CachePipePool;
import uk.ac.lancs.fastcgi.engine.util.Pipe;
import uk.ac.lancs.fastcgi.proto.ApplicationVariables;
import uk.ac.lancs.fastcgi.proto.ProtocolStatuses;
import uk.ac.lancs.fastcgi.proto.RequestFlags;
import uk.ac.lancs.fastcgi.proto.RoleTypes;
import uk.ac.lancs.fastcgi.proto.app.RecordHandler;
import uk.ac.lancs.fastcgi.proto.app.RecordReader;
import uk.ac.lancs.fastcgi.proto.app.RecordWriter;

/**
 * Handles FastCGI records and delivers to all role types, supporting
 * multiple connections and multiple sessions per connection.
 *
 * @author simpsons
 */
class MultiplexGenericEngine implements Engine {
    private final Charset charset;

    private final ConnectionSupply connections;

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
    public MultiplexGenericEngine(ConnectionSupply connections, Charset charset,
                                  Responder responder, Authorizer authorizer,
                                  Filter filter, int maxConns,
                                  int maxReqsPerConn, int maxReqs,
                                  int stdoutBufferSize, int stderrBufferSize) {
        this.connections = connections;
        this.charset = charset;
        this.responder = responder;
        this.authorizer = authorizer;
        this.filter = filter;
        this.maxConns = maxConns;
        this.maxReqs = maxReqs;
        this.maxReqsPerConn = maxReqsPerConn;
        this.stdoutBufferSize = stdoutBufferSize;
        this.stderrBufferSize = stderrBufferSize;
        AtomicInteger ctid = new AtomicInteger(0);
        ThreadFactory conntf =
            (r) -> new Thread(conntg, r, "ct-" + ctid.getAndIncrement());
        this.connExecutor =
            maxConns == 0 ? Executors.newCachedThreadPool(conntf) :
                Executors.newFixedThreadPool(maxConns, conntf);
    }

    private final Executor connExecutor;

    private final List<byte[]> paramBufs = new ArrayList<>();

    private byte[] getBuffer() {
        synchronized (paramBufs) {
            if (paramBufs.isEmpty()) return new byte[128];
            return paramBufs.remove(paramBufs.size() - 1);
        }
    }

    private void returnParamBuf(byte[] buf) {
        synchronized (paramBufs) {
            paramBufs.add(buf);
        }
    }

    @Override
    public boolean process() throws IOException {
        Connection conn = connections.nextConnection();
        if (conn == null) return false;
        ConnHandler ch = new ConnHandler(conn);
        connExecutor.execute(ch);
        return true;
    }

    private final AtomicInteger connIds = new AtomicInteger(0);

    private class ConnHandler implements Runnable, RecordHandler {
        private final int id = connIds.getAndIncrement();

        private final ThreadGroup sesstg =
            new ThreadGroup(conntg, "sessions-" + id);

        private final Connection conn;

        private final RecordReader recordsIn;

        private final RecordWriter recordsOut;

        private final Executor executor;

        public ConnHandler(Connection conn) throws IOException {
            this.conn = conn;
            this.recordsIn = new RecordReader(conn.getInput(), charset, this);
            this.recordsOut = new RecordWriter(conn.getOutput(), charset);
            AtomicInteger intSessIds = new AtomicInteger(0);
            ThreadFactory tf = r -> new Thread(sesstg, r, "session-" + id + "-"
                + intSessIds.getAndIncrement());
            this.executor = maxReqsPerConn >= 1 ?
                Executors.newFixedThreadPool(maxReqsPerConn, tf) :
                Executors.newCachedThreadPool(tf);
        }

        private final Map<Integer, SessionHandler> sessions =
            new ConcurrentHashMap<>();

        private boolean keepGoing = true;

        @Override
        public void run() {
            try {
                while ((keepGoing || !sessions.isEmpty()) &&
                    recordsIn.processRecord()) {
                    /* All work is done in processRecords(). */
                }
                conn.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "connection " + id, ex);
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
            if ((flags & RequestFlags.KEEP_CONN) != 0) keepGoing = false;

            if (maxReqsPerConn > 0 && sessions.size() >= maxReqsPerConn) {
                recordsOut.writeEndRequest(id, -3,
                                           maxReqsPerConn == 1 ?
                                               ProtocolStatuses.CANT_MPX_CONN :
                                               ProtocolStatuses.OVERLOADED);
                return;
            }

            SessionHandler sess = sessions.computeIfAbsent(id, k -> {
                switch (role) {
                case RoleTypes.RESPONDER:
                    if (responder == null) return null;
                    return new ResponderHandler(id);

                case RoleTypes.FILTER:
                    if (filter == null) return null;
                    return new FilterHandler(id);

                case RoleTypes.AUTHORIZER:
                    if (authorizer == null) return null;
                    return new AuthorizerHandler(id);

                default:
                    /* No mapping is to be recorded. */
                    return null;
                }
            });
            if (sess == null) {
                /* No mapping was recorded, meaning that we don't
                 * recognize the role. */
                recordsOut.writeEndRequest(id, -3,
                                           ProtocolStatuses.UNKNOWN_ROLE);
                return;
            }
            if (sess.start()) {
                /* There must be no existing session. */
                keepGoing = false;
                logger.log(Level.SEVERE,
                           () -> "server began existing request" + id);
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

        private abstract class AbstractHandler
            implements SessionHandler, SessionContext {
            final int id;

            Map<String, String> params = new HashMap<>();

            int appStatus = 0;

            public AbstractHandler(int id) {
                this.id = id;
            }

            abstract void innerRun() throws Exception;

            Thread thread;

            void run() {
                synchronized (this) {
                    this.thread = Thread.currentThread();
                }
                try {
                    boolean completed = false;
                    try {
                        try {
                            innerRun();
                            Thread.interrupted();
                        } finally {
                            synchronized (this) {
                                this.thread = null;
                            }
                        }
                    } catch (InterruptedException ex) {
                        recordsOut
                            .writeEndRequest(id, -1,
                                             ProtocolStatuses.REQUEST_COMPLETE);
                        completed = true;
                    } catch (OverloadException ex) {
                        recordsOut.writeEndRequest(id, -2,
                                                   ProtocolStatuses.OVERLOADED);
                        completed = true;
                    } catch (Exception | Error ex) {
                        setStatus(501);
                        setHeader("Content-Type", "text/plain; charset=UTF-8");
                        try (PrintWriter out =
                            new PrintWriter(new OutputStreamWriter(out(),
                                                                   StandardCharsets.UTF_8))) {
                            out.printf("Internal Server Error\n");
                        }
                        ex.printStackTrace(err());
                        recordsOut
                            .writeEndRequest(id, -2,
                                             ProtocolStatuses.REQUEST_COMPLETE);
                        completed = true;
                    } finally {
                        if (!completed) {
                            ensureResponseHeader();
                            recordsOut
                                .writeEndRequest(id, appStatus,
                                                 ProtocolStatuses.REQUEST_COMPLETE);
                        }
                    }
                } catch (IOException ex) {
                    /* There is some problem with the I/O on this whole
                     * connection, so abort it. */
                    keepGoing = false;
                } finally {
                    sessions.remove(id);
                }
            }

            @Override
            public synchronized void abortRequest() throws IOException {
                if (thread == null)
                    sessions.remove(id);
                else
                    thread.interrupt();
            }

            byte[] paramCache = getBuffer();

            int paramLen = 0;

            /**
             * Attempt to fill the parameter buffer.
             * 
             * @param in the byte source
             * 
             * @return {@code false} if the stream has reported EOF;
             * {@code true} otherwise
             * 
             * @throws IOException if an I/O error occurs reading from
             * the stream
             */
            boolean recordParam(InputStream in) throws IOException {
                assert paramLen <= paramCache.length;
                if (paramLen == paramCache.length) {
                    paramLen += 128;
                    paramLen *= 2;
                    paramCache = Arrays.copyOf(paramCache, paramLen);
                }

                int got =
                    in.read(paramCache, paramLen, paramCache.length - paramLen);
                if (got >= 0) {
                    paramLen += got;
                    return true;
                }
                return false;
            }

            /**
             * Attempt to decode one parameter at the start of the
             * buffer. If a parameter is decoded, its bytes are removed
             * from the buffer, and the trailing bytes are moved to the
             * head of the buffer.
             * 
             * @return {@code true} if the method should be called
             * again; {@code false} otherwise, e.g., if there are
             * insufficient bytes to determine whether a complete
             * parameter has loaded
             */
            boolean decodeParam() {
                if (paramLen < 2) return false;
                final int nameLen, valueLen, nameStart;
                if (paramCache[0] > 127) {
                    if (paramLen < 5) return false;
                    if (paramCache[4] > 127) {
                        if (paramLen < 8) return false;
                        valueLen = getInt(paramCache, 4);
                        nameStart = 8;
                    } else {
                        valueLen = paramCache[4] & 0xff;
                        nameStart = 5;
                    }
                    nameLen = getInt(paramCache, 0);
                } else {
                    if (paramCache[1] > 127) {
                        if (paramLen < 5) return false;
                        valueLen = getInt(paramCache, 1);
                        nameStart = 5;
                    } else {
                        valueLen = paramCache[1] & 0xff;
                        nameStart = 1;
                    }
                    nameLen = paramCache[0] & 0xff;
                }
                final int end = nameStart + nameLen + valueLen;
                if (paramLen < end) return false;
                final String name =
                    new String(paramCache, nameStart, nameLen, charset);
                final int valueStart = nameStart + nameLen;
                final String value =
                    new String(paramCache, valueStart, valueLen, charset);
                params.put(name, value);
                System.arraycopy(paramCache, end, paramCache, 0,
                                 paramLen - end);
                paramLen -= end;
                return paramLen >= 2;
            }

            @Override
            @SuppressWarnings("empty-statement")
            public void params(int len, InputStream in) throws IOException {
                while (recordParam(in))
                    while (decodeParam())
                        ;
            }

            @Override
            public void paramsEnd() throws IOException {
                /* Detect a duplicate call, and save away the parameter
                 * buffer for later use. */
                if (paramCache == null)
                    throw new IOException("parameters ended twice on request "
                        + id);
                returnParamBuf(paramCache);
                paramCache = null;

                /* Check that we have no excess parameter data. */
                if (paramLen > 0)
                    throw new IOException("trailing parameter bytes on request "
                        + id + ": " + paramLen);

                /* Freeze the parameters, */
                params = Map.copyOf(params);

                /* Let the application run. */
                executor.execute(this::run);
            }

            @Override
            public Map<String, String> parameters() {
                /* We don't need to protect this. By the time the
                 * application is called, this has already been made an
                 * immutable copy. */
                return params;
            }

            @Override
            public void exit(int exitCode) {
                if (exitCode < 0)
                    throw new IllegalArgumentException("-ve exit code");
                appStatus = exitCode;
            }

            /**
             * Converts output-stream operations into FCGI_STDOUT
             * records.
             */
            private final OutputStream out = new OutputStream() {
                private boolean closed = false;

                private final byte[] buf1 = new byte[1];

                @Override
                public void write(int b) throws IOException {
                    if (closed) throw new IOException("closed");
                    buf1[0] = (byte) b;
                    recordsOut.writeStdout(id, buf1, 0, 1);
                }

                @Override
                public void close() throws IOException {
                    if (closed) return;
                    closed = true;
                    recordsOut.writeStdoutEnd(id);
                }

                @Override
                public void write(byte[] b, int off, int len)
                    throws IOException {
                    if (closed) throw new IOException("closed");

                    /* TODO: For large values of len, break into
                     * multiple calls. */
                    recordsOut.writeStdout(id, b, off, len);
                }
            };

            /**
             * Reduces calls on {@link #out} by buffering.
             */
            private OutputStream bufferedOut;

            private int bufferSize = stdoutBufferSize;

            @Override
            public boolean setBufferSize(int amount) {
                if (amount < 0)
                    throw new IllegalArgumentException("-ve buffer size "
                        + amount);
                if (bufferedOut != null) return false;
                bufferSize = amount;
                return true;
            }

            /**
             * Ensures that the response header has been transmitted.
             * This object is presented to the application as its
             * standard output.
             */
            private final OutputStream headeredOut = new OutputStream() {
                @Override
                public void write(byte[] b, int off, int len)
                    throws IOException {
                    ensureResponseHeader();
                    bufferedOut.write(b, off, len);
                }

                @Override
                public void write(int b) throws IOException {
                    ensureResponseHeader();
                    bufferedOut.write(b);
                }

                @Override
                public void close() throws IOException {
                    ensureResponseHeader();
                    bufferedOut.close();
                }

                @Override
                public void flush() throws IOException {
                    ensureResponseHeader();
                    bufferedOut.flush();
                }
            };

            @Override
            public OutputStream out() {
                return headeredOut;
            }

            private final PrintStream err =
                new PrintStream(new BufferedOutputStream(new OutputStream() {
                    private boolean closed = false;

                    private final byte[] buf1 = new byte[1];

                    @Override
                    public void write(int b) throws IOException {
                        if (closed) throw new IOException("closed");
                        buf1[0] = (byte) b;
                        recordsOut.writeStderr(id, buf1, 0, 1);
                    }

                    @Override
                    public void close() throws IOException {
                        if (closed) return;
                        closed = true;
                        recordsOut.writeStderrEnd(id);
                    }

                    @Override
                    public void write(byte[] b, int off, int len)
                        throws IOException {
                        if (closed) throw new IOException("closed");
                        recordsOut.writeStderr(id, b, off, len);
                    }
                }, stderrBufferSize), true, charset);

            @Override
            public PrintStream err() {
                return err;
            }

            int statusCode = 200;

            private final Map<String, List<String>> outHeaders =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            @Override
            public void setHeader(String name, String value) {
                Objects.requireNonNull(name, "name");
                Objects.requireNonNull(value, "value");
                name = name.trim();
                setHeaderInternal(name, value);
            }

            void setHeaderInternal(String name, String value) {
                if (name.equalsIgnoreCase(SessionContext.STATUS_FIELD))
                    throw new IllegalArgumentException("reserved name " + name);
                if (statusCode < 0)
                    throw new IllegalStateException("header sent");
                List<String> aval =
                    outHeaders.computeIfAbsent(name, k -> new ArrayList<>());
                aval.clear();
                aval.add(value);
            }

            @Override
            public void addHeader(String name, String value) {
                Objects.requireNonNull(name, "name");
                Objects.requireNonNull(value, "value");
                name = name.trim();
                addHeaderInternal(name, value);
            }

            void addHeaderInternal(String name, String value) {
                if (name.equalsIgnoreCase(SessionContext.STATUS_FIELD))
                    throw new IllegalArgumentException("reserved name " + name);
                if (statusCode < 0)
                    throw new IllegalStateException("header sent");
                outHeaders.computeIfAbsent(name, k -> new ArrayList<>())
                    .add(value);
            }

            @Override
            public void setStatus(int code) {
                if (statusCode < 0)
                    throw new IllegalStateException("header sent");
                if (code < 100 || code >= 600)
                    throw new IllegalArgumentException("bad status " + code);
                statusCode = code;
            }

            private void ensureResponseHeader() throws IOException {
                if (statusCode < 0) return;

                assert bufferedOut == null;
                bufferedOut = bufferSize == 0 ? out :
                    new BufferedOutputStream(out, bufferSize);

                /* Don't autoclose this stream; we need the base to
                 * remain open. */
                PrintStream pout = new PrintStream(bufferedOut, false, charset);
                try {
                    pout.printf("Status: %d %s%n", statusCode,
                                getStatusMessage(statusCode));
                    for (var entry : outHeaders.entrySet()) {
                        List<String> values = entry.getValue();
                        if (values.isEmpty()) continue;
                        String name = entry.getKey();
                        for (String value : values)
                            pout.printf("%s: %s%n", name, value);
                    }
                    pout.println();
                } finally {
                    pout.flush();
                    statusCode = -1;
                }
            }

            private boolean started = false;

            @Override
            public boolean start() {
                if (started) return true;
                started = true;
                return false;
            }

            @Override
            public void stdin(int len, InputStream in) throws IOException {
                /* Ignored by default, as we don't expect this record
                 * type. */
            }

            @Override
            public void stdinEnd() throws IOException {
                /* Ignored by default, as we don't expect this record
                 * type. */
            }

            @Override
            public void data(int len, InputStream in) throws IOException {
                /* Ignored by default, as we don't expect this record
                 * type. */
            }

            @Override
            public void dataEnd() throws IOException {
                /* Ignored by default, as we don't expect this record
                 * type. */
            }
        }

        private class ResponderHandler extends AbstractHandler
            implements ResponderContext {

            public ResponderHandler(int id) {
                super(id);
            }

            @Override
            void innerRun() throws Exception {
                responder.respond(this);
            }

            private final Pipe stdinPipe = pipes.get();

            @Override
            public void abortRequest() throws IOException {
                super.abortRequest();
                stdinPipe.getOutputStream().close();
            }

            @Override
            public void stdin(int len, InputStream in) throws IOException {
                in.transferTo(stdinPipe.getOutputStream());
            }

            @Override
            public void stdinEnd() throws IOException {
                stdinPipe.getOutputStream().close();
            }

            @Override
            public InputStream in() {
                return stdinPipe.getInputStream();
            }
        }

        private class FilterHandler extends AbstractHandler
            implements FilterContext {
            public FilterHandler(int id) {
                super(id);
            }

            @Override
            void innerRun() throws Exception {
                filter.filter(this);
            }

            private final Pipe stdinPipe = pipes.get();

            @Override
            public void abortRequest() throws IOException {
                super.abortRequest();
                stdinPipe.getOutputStream().close();
                dataPipe.getOutputStream().close();
            }

            @Override
            public void stdin(int len, InputStream in) throws IOException {
                in.transferTo(stdinPipe.getOutputStream());
            }

            @Override
            public void stdinEnd() throws IOException {
                stdinPipe.getOutputStream().close();
            }

            @Override
            public InputStream in() {
                return stdinPipe.getInputStream();
            }

            private final Pipe dataPipe = pipes.get();

            @Override
            public void data(int len, InputStream in) throws IOException {
                in.transferTo(dataPipe.getOutputStream());
            }

            @Override
            public void dataEnd() throws IOException {
                dataPipe.getOutputStream().close();
            }

            @Override
            public InputStream data() {
                return dataPipe.getInputStream();
            }
        }

        private class AuthorizerHandler extends AbstractHandler
            implements AuthorizerContext {
            public AuthorizerHandler(int id) {
                super(id);
            }

            @Override
            void innerRun() throws Exception {
                authorizer.authorize(this);
            }

            @Override
            public void addHeader(String name, String value) {
                Objects.requireNonNull(name, "name");
                Objects.requireNonNull(value, "value");
                name = name.trim();
                if (isVariable(name))
                    throw new IllegalArgumentException("reserved name " + name);
                addHeaderInternal(name, value);
                if (statusCode == 200) statusCode = 401;
            }

            @Override
            public void setHeader(String name, String value) {
                Objects.requireNonNull(name, "name");
                Objects.requireNonNull(value, "value");
                name = name.trim();
                if (isVariable(name))
                    throw new IllegalArgumentException("reserved name " + name);
                setHeaderInternal(name, value);
                if (statusCode == 200) statusCode = 401;
            }

            @Override
            public void setVariable(String name, String value) {
                Objects.requireNonNull(name, "name");
                Objects.requireNonNull(value, "value");
                name = name.trim();
                setHeaderInternal(AuthorizerContext.VARIABLE_PREFIX + name,
                                  value);
            }
        }
    }

    /**
     * Receives application record types, with the request id stripped
     * and implied by context.
     */
    private interface SessionHandler {
        /**
         * Start the session, if not already.
         * 
         * @return {@code true} if the session was already started
         */
        boolean start();

        void abortRequest() throws IOException;

        void params(int len, InputStream in) throws IOException;

        void paramsEnd() throws IOException;

        void stdin(int len, InputStream in) throws IOException;

        void stdinEnd() throws IOException;

        void data(int len, InputStream in) throws IOException;

        void dataEnd() throws IOException;
    }

    private static final Logger logger =
        Logger.getLogger(MultiplexGenericEngine.class.getPackageName());

    private int getInt(byte[] buf, int off) {
        int r = 0;
        for (int i = 0; i < 4; i++) {
            r <<= 8;
            r |= buf[off + i] & 0xff;
        }
        return r & 0x7fffffff;
    }

    private static String getStatusMessage(int code) {
        switch (code) {
        default:
            return "UNKNOWN";

        case 100:
            return "Continue";

        case 101:
            return "Switching Protocols";

        case 102:
            return "Processing";

        case 200:
            return "OK";

        case 201:
            return "Created";

        case 202:
            return "Accepted";

        case 203:
            return "Non-authoritative Information";

        case 204:
            return "No Content";

        case 205:
            return "Reset Content";

        case 206:
            return "Partial Content";

        case 207:
            return "Multi-Status";

        case 208:
            return "Already Reported";

        case 226:
            return "IM Used";

        case 300:
            return "Multiple Choices";

        case 301:
            return "Moved Permanently";

        case 302:
            return "Found";

        case 303:
            return "See Other";

        case 304:
            return "Not Modified";

        case 305:
            return "Use Proxy";

        case 307:
            return "Temporary Redirect";

        case 308:
            return "Permanent Redirect";

        case 400:
            return "Bad Request";

        case 401:
            return "Unauthorized";

        case 402:
            return "Payment Required";

        case 403:
            return "Forbidden";

        case 404:
            return "Not Found";

        case 405:
            return "Method Not Allowed";

        case 406:
            return "Not Acceptable";

        case 407:
            return "Proxy Authentication Required";

        case 408:
            return "Request Timeout";

        case 409:
            return "Conflict";

        case 410:
            return "Gone";

        case 411:
            return "Length Required";

        case 412:
            return "Precondition Failed";

        case 413:
            return "Payload Too Large";

        case 414:
            return "Request-URI Too Long";

        case 415:
            return "Unsupported Media Type";

        case 416:
            return "Requested Range Not Satisfiable";

        case 417:
            return "Expectation Failed";

        case 418:
            return "I'm a teapot";

        case 421:
            return "Misdirected Request";

        case 422:
            return "Unprocessable Entity";

        case 423:
            return "Locked";

        case 424:
            return "Failed Dependency";

        case 426:
            return "Upgrade Required";

        case 428:
            return "Precondition Required";

        case 429:
            return "Too Many Requests";

        case 431:
            return "Request Header Fields Too Large";

        case 444:
            return "Connection Closed Without Respons";

        case 451:
            return "Unavailable For Legal Reasons";

        case 499:
            return "Client Closed Request";

        case 500:
            return "Internal Server Error";

        case 501:
            return "Not Implemented";

        case 502:
            return "Bad Gateway";

        case 503:
            return "Service Unavailable";

        case 504:
            return "Gateway Timeout";

        case 505:
            return "HTTP Version Not Supported";

        case 506:
            return "Variant Also Negotiates";

        case 507:
            return "Insufficient Storage";

        case 508:
            return "Loop Detected";

        case 510:
            return "Not Extended";

        case 511:
            return "Network Authentication Required";

        case 599:
            return "Network Connect Timeout Error";
        }
    }

    private static final int VARIABLE_PREFIX_LENGTH =
        AuthorizerContext.VARIABLE_PREFIX.length();

    private static boolean isVariable(CharSequence in) {
        if (in.length() < VARIABLE_PREFIX_LENGTH) return false;
        CharSequence prefix =
            in.subSequence(0, VARIABLE_PREFIX_LENGTH).toString();
        return AuthorizerContext.VARIABLE_PREFIX.equals(prefix);
    }
}
