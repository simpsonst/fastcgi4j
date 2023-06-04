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

package uk.ac.lancs.fastcgi.transport.iis;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import uk.ac.lancs.fastcgi.transport.Transport;
import uk.ac.lancs.fastcgi.transport.TransportConfigurationException;
import uk.ac.lancs.fastcgi.transport.TransportFactory;
import uk.ac.lancs.scc.jardeps.Service;

/**
 * Recognizes invocation by IIS as a FastCGI process, if the environment
 * variable {@value #ENV_NAME} is set.
 * 
 * @author simpsons
 */
@Service(TransportFactory.class)
public class ForkedIISTransportFactory implements TransportFactory {
    @Override
    public Transport getTransport() {
        String pipeName = System.getenv(ENV_NAME);
        if (pipeName == null) return null;
        try {
            RandomAccessFile file = new RandomAccessFile(pipeName, "rw");
            return new ForkedIISTransport(file, pipeName);
        } catch (FileNotFoundException ex) {
            throw new TransportConfigurationException(pipeName, ex);
        }
    }

    /**
     * Identifies the environment variable naming the pipe over which
     * requests and responses are carried.
     */
    public static final String ENV_NAME = "_FCGI_X_PIPE_";
}
