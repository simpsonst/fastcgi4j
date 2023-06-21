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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Pattern;
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

    private final String leadElem;

    /**
     * The sub-path of the request
     */
    public final String subpath;

    private final List<String> subelems;

    private PathContext(String script, String subpath) {
        this.script = script;

        String[] scriptElems = PATH_SEPS.split(script);
        leadElem =
            scriptElems.length == 0 ? "" : scriptElems[scriptElems.length - 1];

        this.subpath = subpath;
        var spes = new ArrayList<>(List.of(PATH_SEPS.split(subpath, -1)));
        spes.set(0, leadElem);
        this.subelems = List.copyOf(spes);
    }

    private static final Pattern PATH_SEPS = Pattern.compile("/+");

    /**
     * Determine the relative path of a given reference, based on the
     * sub-path. The reference may be empty to refer to the script
     * itself. Otherwise, if it doesn't begin with a slash, it is
     * treated as if the slash is prefixed. A trailing dot element is
     * replaced with an empty string, and other dot elements are
     * removed. Each double-dot element and its non-double-dot element
     * are removed. Double slashes are collapsed into singles. It is an
     * error to attempt to break out of the script's context by using
     * leading double-dot elements.
     * 
     * <table>
     * <thead>
     * <tr>
     * <th>Script path</th>
     * <th>Sub-path</th>
     * <th>Reference</th>
     * <th>Result</th>
     * </tr>
     * </thead> <tbody>
     * <tr>
     * <td><samp>/foo/bar</samp></td>
     * <td><samp>/baz/qux</samp></td>
     * <td>(empty)</td>
     * <td><samp>bar</samp></td>
     * </tr>
     * <tr>
     * <td><samp>/foo/bar</samp></td>
     * <td><samp>/baz/qux</samp></td>
     * <td><samp>/baz/qux</samp></td>
     * <td><samp>qux</samp></td>
     * </tr>
     * <tr>
     * <td><samp>/foo/bar</samp></td>
     * <td><samp>/baz/qux</samp></td>
     * <td><samp>/baz/qux/quux</samp></td>
     * <td><samp>qux/quux</samp></td>
     * </tr>
     * <tr>
     * <td><samp>/foo/bar</samp></td>
     * <td><samp>/baz/qux</samp></td>
     * <td><samp>/baz/yan/tan/</samp></td>
     * <td><samp>yan/tan/</samp></td>
     * </tr>
     * <tr>
     * <td><samp>/foo/bar</samp></td>
     * <td><samp>/baz/qux/quux</samp></td>
     * <td><samp>/baz/yan/tan/</samp></td>
     * <td><samp>../yan/tan/</samp></td>
     * </tr>
     * <tr>
     * <td><samp>/foo/bar</samp></td>
     * <td><samp>/baz/qux/</samp></td>
     * <td><samp>/baz/qux/</samp></td>
     * <td><samp>.</samp></td>
     * </tr>
     * <tr>
     * <td><samp>/foo/bar</samp></td>
     * <td><samp>/baz/qux/</samp></td>
     * <td><samp>/baz/</samp></td>
     * <td><samp>../</samp></td>
     * </tr>
     * <tr>
     * <td><samp>/foo/bar</samp></td>
     * <td><samp>/baz/qux/</samp></td>
     * <td><samp>/baz</samp></td>
     * <td><samp>../baz</samp></td>
     * </tr>
     * </tbody>
     * </table>
     * 
     * @param ref the reference
     * 
     * @return the relative path
     * 
     * @throws NoSuchElementException if a leading double-dot element is
     * present
     */
    public URI locate(String ref) {
        List<String> elems = new ArrayList<>(List.of(PATH_SEPS.split(ref, -1)));

        /* Normalize the reference by appending an empty element when
         * the last element is a dot or double-dot. */
        if (!elems.isEmpty()) {
            switch (elems.get(elems.size() - 1)) {
            case ".", ".." -> elems.add("");
            }
        }

        /* Normalize the reference by removing "." and ".." elements. */
        for (var iter = elems.listIterator(); iter.hasNext();) {
            String value = iter.next();
            if (value.equals(".")) {
                iter.remove();
            } else if (value.equals("..")) {
                iter.remove();
                iter.previous();
                iter.remove();
            }
        }

        /* The first element should always be the lead element. If
         * blank, set it to that value; otherwise, prefix it. */
        if (!elems.isEmpty() && elems.get(0).isEmpty())
            elems.set(0, leadElem);
        else
            elems.add(0, leadElem);

        /* Always take off the trailing element of the base. */
        List<String> pfx = subelems.subList(0, subelems.size() - 1);

        /* Eliminate identical initial elements of our two lists. */
        while (!pfx.isEmpty() && elems.size() > 1 &&
            pfx.get(0).equals(elems.get(0))) {
            pfx = pfx.subList(1, pfx.size());
            elems = elems.subList(1, elems.size());
        }

        /* For each element of the base still remaining, prefix "../",
         * then append the remaining elements separated by "/". */
        StringBuilder result = new StringBuilder("../".repeat(pfx.size()));
        String sep = "";
        for (String elem : elems) {
            result.append(sep);
            sep = "/";
            escapePathElement(result, elem);
        }
        if (result.isEmpty()) result.append("./");
        return URI.create(result.toString());
    }

    private static final char[] hexes = "0123456789ABCDEF".toCharArray();

    private static void escapePathElement(StringBuilder output,
                                          CharSequence input) {
        int[] cps = input.codePoints().toArray();
        for (int cp : cps) {
            switch (cp) {
            default -> output.appendCodePoint(cp);

            case ':', '/', '?', '#' -> {
                if (cp < 0x80) {
                    /* 0xxx:xxxx */
                    output.append('%').append(hexes[(cp >>> 4) & 0xf])
                        .append(hexes[cp & 0xf]);
                } else if (cp < 0x800) {
                    /* 110x:xxxx 10xx:xxxx */
                    output.append('%').append(hexes[(cp >>> 10) | 0xc])
                        .append(hexes[(cp >>> 6) & 0xf])

                        .append('%').append(hexes[(cp >>> 4) & 0x3 | 0x8])
                        .append(hexes[cp & 0xf]);
                } else if (cp < 0x10000) {
                    /* 1110:xxxx 10xx:xxxx 10xx:xxxx */
                    output.append("%E").append(hexes[cp >>> 12])

                        .append('%').append(hexes[(cp >>> 10) & 0xf | 0x8])
                        .append(hexes[(cp >>> 6) & 0xf])

                        .append('%').append(hexes[(cp >>> 4) & 0xf | 0x8])
                        .append(hexes[cp & 0xf]);
                } else if (cp < 0x200000) {
                    /* 1111:0xxx 10xx:xxxx 10xx:xxxx 10xx:xxxx */
                    output.append("%F").append(hexes[cp >>> 18])

                        .append('%').append(hexes[(cp >>> 16) & 0xf | 0x8])
                        .append(hexes[(cp >>> 12) & 0xf])

                        .append('%').append(hexes[(cp >>> 10) & 0xf | 0x8])
                        .append(hexes[(cp >>> 6) & 0xf])

                        .append('%').append(hexes[(cp >>> 4) & 0xf | 0x8])
                        .append(hexes[cp & 0xf]);
                } else if (cp < 0x4000000) {
                    /* 1111:10xx 10xx:xxxx 10xx:xxxx 10xx:xxxx
                     * 10xx:xxxx */
                    output.append("%F").append(hexes[cp >>> 24] | 0x8)

                        .append('%').append(hexes[(cp >>> 22) & 0xf | 0x8])
                        .append(hexes[(cp >>> 18) & 0xf])

                        .append('%').append(hexes[(cp >>> 16) & 0xf | 0x8])
                        .append(hexes[(cp >>> 12) & 0xf])

                        .append('%').append(hexes[(cp >>> 10) & 0xf | 0x8])
                        .append(hexes[(cp >>> 6) & 0xf])

                        .append('%').append(hexes[(cp >>> 4) & 0xf | 0x8])
                        .append(hexes[cp & 0xf]);
                } else {
                    assert cp < 0x80000000;
                    /* 1111:110x 10xx:xxxx 10xx:xxxx 10xx:xxxx 10xx:xxxx
                     * 10xx:xxxx */
                    output.append("%F").append(hexes[cp >>> 30] | 0xc)

                        .append('%').append(hexes[(cp >>> 28) & 0xf | 0x8])
                        .append(hexes[(cp >>> 24) & 0xf])

                        .append('%').append(hexes[(cp >>> 22) & 0xf | 0x8])
                        .append(hexes[(cp >>> 18) & 0xf])

                        .append('%').append(hexes[(cp >>> 16) & 0xf | 0x8])
                        .append(hexes[(cp >>> 12) & 0xf])

                        .append('%').append(hexes[(cp >>> 10) & 0xf | 0x8])
                        .append(hexes[(cp >>> 6) & 0xf])

                        .append('%').append(hexes[(cp >>> 4) & 0xf | 0x8])
                        .append(hexes[cp & 0xf]);
                }
            }
            }
        }
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
