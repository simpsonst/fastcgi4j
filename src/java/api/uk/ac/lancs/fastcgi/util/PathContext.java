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
 *  Author: Steven Simpson <s.simpson@lancaster.ac.uk>
 */

package uk.ac.lancs.fastcgi.util;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import uk.ac.lancs.fastcgi.context.SessionContext;

/**
 * Specifies the virtual path context of a request. The context consists
 * of two fields, the script path and the sub-path. The script path
 * identifies why the FastCGI application got a request. The sub-path
 * then identifies a resource within the script's internal virtual path
 * space. Together they form the virtual path of the request.
 * 
 * <p>
 * The script path always begins with a forward slash <samp>/</samp>,
 * and may contain several path elements. It does not end with a slash.
 * The sub-path is either empty, or begins with a <samp>/</samp>.
 * Percent-encoded characters have been decoded.
 *
 * @author simpsons
 */
public final class PathContext {
    /**
     * The path of the script being executed
     */
    public final String script;

    /**
     * The sub-path of the request
     */
    public final String subpath;

    private PathContext(String script, String subpath) {
        this.script = script;
        this.subpath = subpath;
    }

    /**
     * Create a path context from CGI variables.
     * 
     * <p>
     * The algorithm is as follows:
     * 
     * <ol>
     * 
     * <li>If <samp>PATH_INFO</samp> is set, return its value as the
     * sub-path, and the value of <samp>SCRIPT_NAME</samp> as the script
     * path.</li>
     * 
     * <li>Otherwise, if <samp>SCRIPT_FILENAME</samp> is a
     * <samp>proxy:</samp> URI, get the raw scheme-specific part of it,
     * parse it as a URI, and extract the decoded path. If it is a
     * suffix of the value of <samp>SCRIPT_NAME</samp>, return it as the
     * the sub-path, and subtract it from <samp>SCRIPT_NAME</samp> to
     * yield the script path.</li>
     * 
     * <li>Otherwise, the value of <samp>SCRIPT_NAME</samp> is the path
     * script, and the sub-path is empty.</li>
     * 
     * </ol>
     * 
     * @param params the request context's CGI variables
     * 
     * @return the derived path context
     */
    public static PathContext
        infer(Map<? super String, ? extends String> params) {
        final URI scriptFilename = URI.create(params.get("SCRIPT_FILENAME"));
        final String pathInfo = params.get("PATH_INFO");
        final String scriptName = params.get("SCRIPT_NAME");
        if (pathInfo != null) {
            return new PathContext(scriptName, pathInfo);
        } else if ("proxy".equals(scriptFilename.getScheme())) {
            final URI ssp =
                URI.create(scriptFilename.getRawSchemeSpecificPart());
            final String virtualPath = ssp.getPath();
            if (scriptName.endsWith(virtualPath)) {
                final String correctedScriptName = scriptName
                    .substring(0, scriptName.length() - virtualPath.length());
                return new PathContext(correctedScriptName, virtualPath);
            }
        }
        return new PathContext(scriptName, "");
    }

    /**
     * Create a path context from a session context. Session parameters
     * are extracted from the context using
     * {@link SessionContext#parameters(), and then passed to
     * {@link #infer(Map)}.
     * 
     * @param ctxt the session context
     * 
     * @return the derived path context
     * 
     * @see #infer(Map)
     */
    public static PathContext infer(SessionContext ctxt) {
        return infer(ctxt.parameters());
    }

    /**
     * Get the hash code for this object.
     * 
     * @return the object's hash code
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + Objects.hashCode(this.script);
        hash = 71 * hash + Objects.hashCode(this.subpath);
        return hash;
    }

    /**
     * Test whether another object equals this path context.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is a path context with
     * identical field values; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PathContext other = (PathContext) obj;
        if (!Objects.equals(this.script, other.script)) {
            return false;
        }
        return Objects.equals(this.subpath, other.subpath);
    }

    /**
     * Get a string representation of this context. This combines the
     * fields as <samp><var>script</var>:<var>subpath</var></samp>.
     * Colons and percent signs in the fields are percent-encoded.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        return escape(script) + ':' + escape(subpath);
    }

    private static String escape(String s) {
        return s.replace("%", "%25").replace(":", "%3A");
    }
}
