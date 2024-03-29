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

package uk.ac.lancs.fastcgi.engine;

/**
 * Indicates that no engine could be provided to meet a requested
 * configuration.
 * 
 * @author simpsons
 */
public class EngineConfigurationException extends RuntimeException {
    /**
     * The configuration that could not be met
     */
    public final EngineConfiguration config;

    /**
     * Create an exception.
     * 
     * @param config the configuration that could not be met
     */
    public EngineConfigurationException(EngineConfiguration config) {
        this.config = config;
    }

    /**
     * Create an exception with a detail message.
     * 
     * @param config the configuration that could not be met
     * 
     * @param message the detail message
     */
    public EngineConfigurationException(EngineConfiguration config,
                                        String message) {
        super(message);
        this.config = config;
    }

    /**
     * Create an exception with a detail message and a cause.
     * 
     * @param config the configuration that could not be met
     * 
     * @param message the detail message
     * 
     * @param cause the cause of the exception
     */
    public EngineConfigurationException(EngineConfiguration config,
                                        String message, Throwable cause) {
        super(message, cause);
        this.config = config;
    }

    /**
     * Create an exception with a cause.
     * 
     * @param config the configuration that could not be met
     * 
     * @param cause the cause of the exception
     */
    public EngineConfigurationException(EngineConfiguration config,
                                        Throwable cause) {
        super(cause);
        this.config = config;
    }

    /**
     * Create an exception with a detail message, a cause, suppression
     * enabled or disabled, and writable stack trace enabled or
     * disabled.
     * 
     * @param config the configuration that could not be met
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
    protected EngineConfigurationException(EngineConfiguration config,
                                           String message, Throwable cause,
                                           boolean enableSuppression,
                                           boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.config = config;
    }
}
