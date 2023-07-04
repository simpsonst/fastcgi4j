/*
 * Copyright (c) 2023, Lancaster University
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
 *  Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.fastcgi.path;

/**
 * Locates internal resources.
 *
 * @author simpsons
 */
public interface Locator {
    /**
     * Determine whether the service instance's root path can be
     * referenced. This is not the case if the instance's root is the
     * server root.
     *
     * @return {@code true} if the root path can be referenced;
     * {@code false} otherwise
     */
    boolean allowRoot();

    /**
     * Refer to an internal resource within the service. An empty string
     * refers to the service root. A leading slash is otherwise assumed.
     * The result can be used to generate a relative URI with a relative
     * or absolute path (useful for generating links to one resource
     * from the content of another), or an absolute URI (essential for
     * redirection).
     *
     * @param path the path to the resource relative to the service root
     *
     * @return the requested resource reference
     *
     * @throws ExternalPathException if an attempt is made to reference
     * an external resource
     */
    PathReference locate(String path);
}
