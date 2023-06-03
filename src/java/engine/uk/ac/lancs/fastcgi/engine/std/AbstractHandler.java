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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import uk.ac.lancs.fastcgi.context.Diagnostics;
import uk.ac.lancs.fastcgi.context.OverloadException;
import uk.ac.lancs.fastcgi.context.SessionContext;
import uk.ac.lancs.fastcgi.proto.ProtocolStatuses;
import uk.ac.lancs.fastcgi.proto.serial.ParamReader;
import uk.ac.lancs.fastcgi.proto.serial.RecordIOException;
import uk.ac.lancs.fastcgi.proto.serial.RecordWriter;

/**
 * Implements session-specific behaviour functionality common to all
 * role types.
 *
 * @author simpsons
 */
abstract class AbstractHandler implements SessionHandler, SessionContext {
    /**
     * Holds the session id (or request id in FastCGI parlance).
     */
    final int id;

    /**
     * Holds the id of the owning connection.
     */
    final int connId;

    /**
     * Holds the diagnostic structure to help cross-referencing of
     * error/log messages.
     */
    final Diagnostics diags;

    /**
     * Holds the action to take if an error is detected on the transport
     * connection, thus jeopardizing all other sessions on the same
     * connection.
     */
    final Runnable connAbort;

    /**
     * Holds the action to take when the session is to receive no more
     * application records.
     */
    final Runnable cleanUp;

    /**
     * Holds the means to write records to the transport connection.
     */
    final RecordWriter recordsOut;

    /**
     * Used to invoke the application-specific behaviour.
     */
    final Executor executor;

    /**
     * Specifies the character encoding for transmitted and received
     * name-value pairs, and for CGI response header fields.
     */
    final Charset charset;

    /**
     * Holds the accumulated request parameters. Once complete, the map
     * is frozen, and only then is the application behaviour invoked.
     */
    Map<String, String> params = new HashMap<>();

    /**
     * Holds context while parsing records that provide request
     * parameters. Each decoded parameter is written to {@link #params}.
     */
    ParamReader paramReader;

    /**
     * Holds the 'exit' status of the application for this
     * session/request.
     */
    int appStatus = 0;

    /**
     * Holds the CGI/HTTP response code for this session/request. This
     * is not transmitted until the standard output is written to,
     * flushed or closed.
     */
    int statusCode = 200;

    /**
     * Holds the CGI/HTTP response headers. Names are case-insensitive.
     * This is not transmitted until the standard output is written to,
     * flushed or closed.
     */
    private final Map<String, List<String>> outHeaders =
        new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Holds the thread being used to execute the application. It is set
     * by the application task as soon as it starts, protected by
     * {@link #threadLock}.
     */
    private Thread thread;

    /**
     * Protects {@link #thread} and {@link #appCompleted}.
     */
    private final Lock threadLock = new ReentrantLock();

    /**
     * Set to {@code true} only when the application code has completed.
     * Protected by {@link #threadLock}.
     */
    private boolean appCompleted = false;

    /**
     * Records whether the handler has started. This is used to detect
     * when the server attempts to begin a request that is already in
     * progress.
     */
    private boolean started = false;

    /**
     * Provides the standard error output to the application handling
     * this request.
     */
    private final PrintStream err;

    /**
     * Reduces calls on {@link #out} by buffering.
     */
    private OutputStream bufferedOut;

    /**
     * Holds the buffer size to be used for standard output, including
     * the CGI response headers. After first use of the stream, this
     * field has no effect.
     */
    private int bufferSize;

    /**
     * Converts output-stream operations into FCGI_STDOUT records.
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
        public void write(byte[] b, int off, int len) throws IOException {
            if (closed) throw new IOException("closed");

            /* TODO: For large values of len, break into multiple
             * calls. */
            recordsOut.writeStdout(id, b, off, len);
        }
    };

    /**
     * Ensures that the response header has been transmitted. This
     * object is presented to the application as its standard output.
     */
    private final OutputStream headeredOut = new OutputStream() {
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
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

    /**
     * Create a request handler.
     * 
     * @param ctxt the set of resources needed by all request handlers
     */
    public AbstractHandler(HandlerContext ctxt) {
        this.id = ctxt.id;
        this.connId = ctxt.connId;
        this.diags =
            new Diagnostics(ctxt.impl, ctxt.connDescr, ctxt.intConnDescr,
                            Integer.toString(ctxt.connId), id);
        this.connAbort = ctxt.connAbort;
        this.cleanUp = ctxt.cleanUp;
        this.recordsOut = ctxt.recordsOut;
        this.executor = ctxt.executor;
        this.charset = ctxt.charset;

        this.paramReader =
            new ParamReader(params, ctxt.charset, ctxt.paramBufs.getBuffer(),
                            ctxt.paramBufs::returnParamBuf);
        this.bufferSize = ctxt.stdoutBufferSize;
        this.err = new PrintStream(new BufferedOutputStream(new OutputStream() {
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
            public void write(byte[] b, int off, int len) throws IOException {
                if (closed) throw new IOException("closed");
                recordsOut.writeStderr(id, b, off, len);
            }
        }, ctxt.stderrBufferSize), true, charset);
    }

    @Override
    public Diagnostics diagnostics() {
        return diags;
    }

    /**
     * Invoke the role-specific behaviour. This will normally involve
     * invoking a specific role interface as defined in
     * {@link uk.ac.lancs.fastcgi.role} to implement
     * application-specific behaviour.
     * 
     * @throws RecordIOException if the invoking thread encounters an
     * I/O error in accessing the connection. For example, an attempt to
     * write a record could fail.
     * 
     * @throws InterruptedException if the invoking thread was
     * interrupted. This can happen if another session detects failure
     * in the transport connection, for example. The request is
     * terminated with exit status {@code -2} and
     * {@link ProtocolStatuses#OVERLOADED}.
     * 
     * @throws OverloadException if the application deems itself too
     * busy to handle new (or even current) requests. The request is
     * terminated with exit status {@code -1} and
     * {@link ProtocolStatuses#REQUEST_COMPLETE}.
     * 
     * @throws Throwable if any other abnormal event occurs. An attempt
     * is made to send an HTTP 501 Server Error response with diagnostic
     * information. The request is then terminated with exit status
     * {@code -2} and {@link ProtocolStatuses#REQUEST_COMPLETE}.
     */
    abstract void innerRun() throws Exception;

    /**
     * Run the application-specific behaviour of this handler. This
     * wraps an invocation of {@link #innerRun()} such that the
     * executing thread is recorded (allowing it to be interrupted on
     * error), and that various exceptions are caught and handled
     * appropriately.
     */
    void run() {
        Thread.currentThread().setName("fastcgi-sess-" + connId + "-" + id);
        try {
            threadLock.lock();
            thread = Thread.currentThread();
        } finally {
            threadLock.unlock();
        }
        try {
            boolean completed = false;
            try {
                try {
                    innerRun();
                } finally {
                    /* Suppress further interruptions. */
                    try {
                        threadLock.lock();
                        appCompleted = true;
                    } finally {
                        threadLock.unlock();
                    }

                    /* Discard any remaining interruptions. */
                    Thread.interrupted();
                }
            } catch (RecordIOException ex) {
                ex.unpack();
            } catch (InterruptedException ex) {
                recordsOut.writeEndRequest(id, -1,
                                           ProtocolStatuses.REQUEST_COMPLETE);
                completed = true;
            } catch (OverloadException ex) {
                recordsOut.writeEndRequest(id, -2, ProtocolStatuses.OVERLOADED);
                completed = true;
            } catch (Exception | Error ex) {
                try {
                    try {
                        setStatus(501);
                        setHeader("Content-Type", "text/plain; charset=UTF-8");
                        /* TODO: Clear other headers. */
                        try (PrintWriter out =
                            new PrintWriter(new OutputStreamWriter(out(),
                                                                   StandardCharsets.UTF_8))) {
                            out.printf("Internal Server Error\n");
                            /* TODO: Provide a more detailed and
                             * run-time configurable error. Use XSLT. */
                        }
                    } catch (IllegalStateException ise) {
                        err().printf("Could not send error response; "
                            + "response body partially sent%n");
                    }
                    ex.printStackTrace(err());
                    recordsOut
                        .writeEndRequest(id, -2,
                                         ProtocolStatuses.REQUEST_COMPLETE);
                    completed = true;
                } catch (RecordIOException ex2) {
                    ex2.unpack();
                }
            } finally {
                if (!completed) {
                    try {
                        ensureResponseHeader();
                        recordsOut
                            .writeEndRequest(id, appStatus,
                                             ProtocolStatuses.REQUEST_COMPLETE);
                    } catch (RecordIOException ex2) {
                        ex2.unpack();
                    }
                }
            }
        } catch (IOException ex) {
            /* The application was unable to write a record, so we have
             * to terminate all handlers on this connection. */
            connAbort.run();
        } finally {
            cleanUp.run();
            Thread.currentThread().setName("fastcgi-sess-unused");
        }
    }

    protected void terminate() {
        try {
            threadLock.lock();
            if (thread == null)
                cleanUp.run();
            else if (!appCompleted) thread.interrupt();
        } finally {
            threadLock.unlock();
        }
    }

    @Override
    public void abortRequest() throws IOException {
        terminate();
    }

    @Override
    public void transportFailure(IOException ex) {
        terminate();
    }

    @Override
    @SuppressWarnings("empty-statement")
    public void params(int len, InputStream in) throws IOException {
        assert len > 0;
        paramReader.consume(in);
    }

    @Override
    public void paramsEnd() throws IOException {
        try {
            paramReader.complete();
        } catch (IllegalStateException ex) {
            ex.printStackTrace(err());
        }
        paramReader = null;

        /* Freeze the parameters, */
        params = Map.copyOf(params);

        /* Let the application run. */
        executor.execute(this::run);
    }

    @Override
    public Map<String, String> parameters() {
        /* We don't need to protect this. By the time the application is
         * called, this has already been made an immutable copy. */
        return params;
    }

    @Override
    public void exit(int exitCode) {
        if (exitCode < 0) throw new IllegalArgumentException("-ve exit code");
        appStatus = exitCode;
    }

    @Override
    public boolean setBufferSize(int amount) {
        if (amount < 0)
            throw new IllegalArgumentException("-ve buffer size " + amount);
        if (bufferedOut != null) return false;
        bufferSize = amount;
        return true;
    }

    @Override
    public OutputStream out() {
        return headeredOut;
    }

    @Override
    public PrintStream err() {
        return err;
    }

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
        if (statusCode < 0) throw new IllegalStateException("header sent");
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
        if (statusCode < 0) throw new IllegalStateException("header sent");
        outHeaders.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }

    @Override
    public void setStatus(int code) {
        if (statusCode < 0) throw new IllegalStateException("header sent");
        if (code < 100 || code >= 600)
            throw new IllegalArgumentException("bad status " + code);
        statusCode = code;
    }

    private void ensureResponseHeader() throws IOException {
        if (statusCode < 0) return;

        assert bufferedOut == null;
        bufferedOut =
            bufferSize == 0 ? out : new BufferedOutputStream(out, bufferSize);

        /* Don't autoclose this stream; we need the base to remain
         * open. */
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

    @Override
    public boolean start() {
        if (started) return true;
        started = true;
        return false;
    }

    @Override
    public void stdin(int len, InputStream in) throws IOException {
        /* Ignored by default, as we don't expect this record type. */
    }

    @Override
    public void stdinEnd() throws IOException {
        /* Ignored by default, as we don't expect this record type. */
    }

    @Override
    public void data(int len, InputStream in) throws IOException {
        /* Ignored by default, as we don't expect this record type. */
    }

    @Override
    public void dataEnd() throws IOException {
        /* Ignored by default, as we don't expect this record type. */
    }

    /**
     * Convert an HTTP status code into its human-readable equivalent.
     * 
     * @param code the code to convert
     * 
     * @return the equivalent message; or {@code "UNKNOWN"}.
     */
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
}
