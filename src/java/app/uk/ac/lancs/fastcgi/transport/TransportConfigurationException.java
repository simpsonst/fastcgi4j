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

package uk.ac.lancs.fastcgi.transport;

/**
 * Indicates that a transport was exclusively identified, but has not
 * been configured correctly.
 * 
 * @author simpsons
 */
public class TransportConfigurationException extends RuntimeException {
    /**
     * Create an exception.
     */
    public TransportConfigurationException() {}

    /**
     * Create an exception with a detail message.
     * 
     * @param message the detail message
     */
    public TransportConfigurationException(String message) {
        super(message);
    }

    /**
     * Create an exception with a detail message and a cause.
     * 
     * @param message the detail message
     * 
     * @param cause the cause of the exception
     */
    public TransportConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create an exception with a cause.
     * 
     * @param cause the cause of the exception
     */
    public TransportConfigurationException(Throwable cause) {
        super(cause);
    }

    /**
     * Create an exception with a detail message, a cause, suppression
     * enabled or disabled, and writable stack trace enabled or
     * disabled.
     * 
     * @param message the detail message
     * 
     * @param cause the cause; or {@code null} if there was no cause
     * 
     * @param enableSuppression whether or not suppression is enabled or
     * disabled
     * 
     * @param writableStackTrace whether or not the stack trace should
     * be writable
     */
    protected TransportConfigurationException(String message, Throwable cause,
                                              boolean enableSuppression,
                                              boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
