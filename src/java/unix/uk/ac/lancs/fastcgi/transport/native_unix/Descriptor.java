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

package uk.ac.lancs.fastcgi.transport.native_unix;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author simpsons
 */
class Descriptor {
    class State implements Runnable {
        int fd;

        public State(int fd) {
            this.fd = fd;
        }

        @Override
        public void run() {
            try {
                closeSocket(fd);
                fd = -1;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    int fd() {
        return state.fd;
    }

    private final State state;

    private final Cleaner.Cleanable cleanable;

    public Descriptor(int descriptor) {
        if (descriptor < 0)
            throw new IllegalArgumentException("-ve fd " + descriptor);
        this.state = new State(descriptor);
        this.cleanable = cleaner.register(this, state);
    }

    public boolean isValid() {
        return fd() >= 0;
    }

    public void close() throws IOException {
        closeSocket(fd());
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return readSocket(fd(), b, off, len);
    }

    public int read() throws IOException {
        return readSocket(fd());
    }

    public void write(int b) throws IOException {
        writeSocket(fd(), b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        writeSocket(fd(), b, off, len);
    }

    /**
     * Close a descriptor.
     * 
     * @param descriptor the descriptor to close
     * 
     * @throws IOException if closing the descriptor returns a negative
     * result
     */
    static native void closeSocket(int descriptor) throws IOException;

    /**
     * Check to see if file descriptor 0 is a server socket.
     * 
     * @param addrLen an array whose first element will contain the size
     * of the socket's address
     * 
     * @param addr an array to write the socket's address into
     * 
     * @return the descriptor if it is a server socket; or a negative
     * value otherwise
     */
    static native int checkDescriptor(int[] addrLen, byte[] addr);

    /**
     * Decode an Internet-domain address from a buffer.
     * 
     * @param addrLen the number of bytes at the start of the array
     * holding the address
     * 
     * @param addr the array holding the address
     * 
     * @return the address reformulated as a Java socket address if of
     * the right domain; or {@code null} otherwise
     */
    static native InetSocketAddress getInternetAddress(int addrLen,
                                                       byte[] addr);

    /**
     * Attempt to accept a connection
     * 
     * @param descriptor the descriptor on which to accept the
     * connection
     * 
     * @param addrLen an array whose first element will contain the size
     * of the peer's address
     * 
     * @param addr an array to write the peer's address into. Its length
     * must be at least that returned by {@link #getAddressSize()}.
     * 
     * @return the descriptor of the connection
     * 
     * @throws IOException if the internal call returns a negative
     * result
     */
    static native int acceptConnection(int descriptor, int[] addrLen,
                                       byte[] addr)
        throws IOException;

    /**
     * Get the minimum buffer size to be passed to
     * <code class="c">accept</code>. In a single process, this is a
     * constant.
     * 
     * @return the minimum buffer size
     */
    public static native int getAddressSize();

    /**
     * Write a single byte to a descriptor.
     * 
     * @param descriptor the descriptor to write to
     * 
     * @param b the byte to write
     * 
     * @throws IOException if the internal call returns a negative
     * result
     */
    static native void writeSocket(int descriptor, int b) throws IOException;

    /**
     * Write a complete array of bytes.
     * 
     * @param descriptor the descriptor to write to
     * 
     * @param b the array of bytes to write
     * 
     * @param off the index into the array of the first byte to write
     * 
     * @param len the number of bytes to write
     * 
     * @throws IOException if the internal call returns a negative
     * result
     */
    static native void writeSocket(int descriptor, byte[] b, int off, int len)
        throws IOException;

    /**
     * Read bytes from a descriptor.
     * 
     * @param descriptor the descriptor to read from
     * 
     * @param b the array to read bytes into
     * 
     * @param off the index into the array of the first byte
     * 
     * @param len the maximum number of bytes to read
     * 
     * @return the number of bytes read; or negative if the end-of-file
     * is reached
     * 
     * @throws IOException if the internal call returns a negative
     * result
     */
    static native int readSocket(int descriptor, byte[] b, int off, int len)
        throws IOException;

    /**
     * Read a single byte from a descriptor.
     * 
     * @param descriptor the descriptor to read from
     * 
     * @return a single byte as an unsigned integer; or negative if
     * end-of-file is reached
     * 
     * @throws IOException if the internal call returns a negative
     * result
     */
    static native int readSocket(int descriptor) throws IOException;

    static final Cleaner cleaner = Cleaner.create();

    static final Logger logger =
        Logger.getLogger(Descriptor.class.getPackageName());

    private static final String LIBRARY_PROP =
        "uk.ac.lancs.fastcgi.transport.native_unix.library";

    static {
        System.load(System.getProperty(LIBRARY_PROP));
    }
}
