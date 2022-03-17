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

package uk.ac.lancs.fastcgi.transport.native_unix;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds a Unix file descriptor. The descriptor is closed when the
 * object is garbage-collected. Several native methods implement base
 * operations on a file descriptor.
 * 
 * @author simpsons
 */
class Descriptor {
    /**
     * Holds a file descriptor, and closes it when run.
     */
    static class State implements Runnable {
        int fd;

        /**
         * Retain a file descriptor to be closed later.
         * 
         * @param fd the file descriptor to retain
         */
        public State(int fd) {
            this.fd = fd;
        }

        /**
         * Close the descriptor, and make it invalid for good measure.
         */
        @Override
        public void run() {
            try {
                closeSocket(fd);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            } finally {
                fd = -1;
            }
        }
    }

    /**
     * Get the internal file descriptor.
     * 
     * @return the descriptor
     */
    int fd() {
        return state.fd;
    }

    /**
     * Holds the state that must persist after we have been
     * garbage-collected so that internal resources can be properly
     * released.
     */
    private final State state;

    /**
     * Ensures that the descriptor is closed when we get
     * garbage-collected.
     */
    private final Cleaner.Cleanable cleanable;

    /**
     * Retain a file descriptor.
     * 
     * @param descriptor the internal descriptor value
     * 
     * @throws IllegalArgumentException if a negative file descriptor is
     * supplied
     */
    public Descriptor(int descriptor) {
        if (descriptor < 0)
            throw new IllegalArgumentException("-ve fd " + descriptor);
        this.state = new State(descriptor);
        this.cleanable = cleaner.register(this, state);
    }

    /**
     * Determine whether the descriptor is valid. It should normally
     * only become invalid after being closed.
     * 
     * @return {@code true} if the descriptor is valid; {@code false}
     * otherwise
     */
    public boolean isValid() {
        return fd() >= 0;
    }

    /**
     * Close the descriptor.
     * 
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        cleanable.clean();
    }

    /**
     * Read bytes from the descriptor into an array. This simply calls
     * {@link #readSocket(int, byte[], int, int)}, passing the result of
     * {@link #fd()} as the first argument.
     * 
     * @param b the destination array
     * 
     * @param off the index of the first array element to store a byte
     * from the descriptor
     * 
     * @param len the maximum number of bytes to read
     * 
     * @return the number of bytes read; or {@code -1} on end-of-file
     * 
     * @throws IOException if an I/O error occurs
     */
    public int read(byte[] b, int off, int len) throws IOException {
        return readSocket(fd(), b, off, len);
    }

    /**
     * Read at most one byte from the descriptor. This simply calls
     * {@link #readSocket(int)}, passing the result of {@link #fd()} as
     * the argument.
     * 
     * @return the byte read, as an unsigned value; or {@code -1} on
     * end-of-file
     * 
     * @throws IOException if an I/O error occurs
     */
    public int read() throws IOException {
        return readSocket(fd());
    }

    /**
     * Write a single byte to the descriptor. This simply calls
     * {@link #writeSocket(int, int)}, passing the result of
     * {@link #fd()} as the first argument.
     * 
     * @param b the byte to be written, as an unsigned value
     * 
     * @throws IOException if an I/O error occurs
     */
    public void write(int b) throws IOException {
        writeSocket(fd(), b);
    }

    /**
     * Write an array of bytes to the descriptor. This simply calls
     * {@link #writeSocket(int, byte[], int, int)}, passing the result
     * of {@link #fd()} as the first argument.
     * 
     * @param b the array containing the bytes
     * 
     * @param off the index of the first element to write
     * 
     * @param len the number of bytes to write
     * 
     * @throws IOException if an I/O error occurs
     */
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

    /**
     * Ensures descriptors are closed when the containing object is
     * garbage-collected.
     */
    static final Cleaner cleaner = Cleaner.create();

    static final Logger logger =
        Logger.getLogger(Descriptor.class.getPackageName());

    /**
     * Identifies the {@linkplain System#getProperties() system
     * property} whose value specifies the location of the native
     * implementation.
     */
    private static final String LIBRARY_PROP =
        "uk.ac.lancs.fastcgi.transport.native_unix.library";

    static {
        System.load(System.getProperty(LIBRARY_PROP));
    }
}
