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

/**
 * Provides transports for server-forked FastCGI applications.
 * Server-forked applications receive a server socket as file descriptor
 * 0. Although Java provides {@link java.io.FileDescriptor#in} to model
 * this, there's no non-native way in Java to build a
 * {@link java.net.ServerSocket} from it, so native calls are used to
 * build the transport.
 * 
 * @see <a href= "https://httpd.apache.org/mod_fcgid/"
 * title="mod_fcgid - FastCGI interface module for Apache 2 - The Apache HTTP Server Project">mod_fcgi</a>
 * Apache module
 * 
 * @see <a href=
 * "https://redmine.lighttpd.net/projects/1/wiki/docs_modfastcgi"
 * title="Docs ModFastCGI - Lighttpd - lighty labs">ModFastCGI</a>
 * Lighttpd module
 * 
 * @author simpsons
 * 
 * @deprecated This relies on some native code, and so might not work
 * well with lightweight threads. Inherited-channel transports require
 * no native code, and so should work better.
 */
package uk.ac.lancs.fastcgi.transport.fork;
