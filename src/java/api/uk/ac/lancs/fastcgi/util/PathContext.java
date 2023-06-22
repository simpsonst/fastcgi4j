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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
        leadElem = scriptElems[scriptElems.length - 1];

        this.subpath = subpath;
        var spes = new ArrayList<>(List.of(PATH_SEPS.split(subpath, -1)));
        spes.set(0, leadElem);
        this.subelems = List.copyOf(spes);
    }

    private static final Pattern PATH_SEPS = Pattern.compile("/+");

    /**
     * Determine the relative path of a given reference, based on the
     * sub-path.
     * 
     * <p>
     * This call is equivalent to calling
     * <code>{@linkplain #locate(String, List, String) locate}(ref, null, null)</code>.
     * 
     * @param ref the reference
     * 
     * @return the relative URI
     * 
     * @throws NoSuchElementException if a leading double-dot element is
     * present
     */
    public URI locate(String ref) {
        return locate(ref, null, null);
    }

    /**
     * Determine the relative path of a given reference, based on the
     * sub-path, and optionally include a fragment identifier.
     * 
     * <p>
     * This call is equivalent to calling
     * <code>{@linkplain #locate(String, List, String) locate}(ref, null, fragment)</code>.
     * 
     * @param ref the reference
     * 
     * @param fragment the fragment identifier; or {@code null} if no
     * fragment identifier is to be present
     * 
     * @return the relative URI
     * 
     * @throws NoSuchElementException if a leading double-dot element is
     * present
     */
    public URI locate(String ref, String fragment) {
        return locate(ref, null, fragment);
    }

    /**
     * Determine the relative path of a given reference, based on the
     * sub-path, and optionally include query parameters.
     * 
     * <p>
     * This call is equivalent to calling
     * <code>{@linkplain #locate(String, List, String) locate}(ref, params, null)</code>.
     * 
     * @param ref the reference
     * 
     * @param params name-value pairs for the query string; or
     * {@code null} if no query string is to be present
     * 
     * @return the relative URI
     * 
     * @throws NoSuchElementException if a leading double-dot element is
     * present
     */
    public URI
        locate(String ref,
               Collection<? extends Map.Entry<? extends String,
                                              ? extends String>> params) {
        return locate(ref, params, null);
    }

    /**
     * Determine whether the script's root path can be referenced. This
     * is not the case if the script's root is the server root.
     * 
     * @return {@code true} if the root path can be referenced;
     * {@code false} otherwise
     */
    public boolean allowRoot() {
        return !leadElem.isEmpty();
    }

    /**
     * Determine the relative path of a given reference, based on the
     * sub-path, and optionally include query parameters and a fragment
     * identifier.
     * 
     * <p>
     * The reference may be empty to refer to the script itself.
     * Otherwise, if it doesn't begin with a slash, it is treated as if
     * the slash is prefixed. A trailing dot element is replaced with an
     * empty string, and other dot elements are removed. Each double-dot
     * element and its non-double-dot element are removed. Double
     * slashes are collapsed into singles. It is an error to attempt to
     * break out of the script's context by using leading double-dot
     * elements.
     * 
     * <p>
     * Query parameters are added in the iteration order of the supplied
     * collection.
     * 
     * <table>
     * <thead>
     * <tr>
     * <th align="left">Script path</th>
     * <th align="left">Sub-path</th>
     * <th align="left">Reference</th>
     * <th align="left">Result</th>
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
     * <td><samp>../../baz</samp></td>
     * </tr>
     * </tbody>
     * </table>
     * 
     * @param ref the reference
     * 
     * @param params name-value pairs for the query string; or
     * {@code null} if no query string is to be present
     * 
     * @param fragment the fragment identifier; or {@code null} if no
     * fragment identifier is to be present
     * 
     * @return the relative URI
     * 
     * @throws NoSuchElementException if a leading double-dot element is
     * present
     */
    public URI locate(String ref,
                      Collection<? extends Map.Entry<? extends String,
                                                     ? extends String>> params,
                      String fragment) {
        /* The raw script cannot be referenced if at the top level. */
        if (leadElem.isEmpty() && ref.isEmpty())
            throw new IllegalArgumentException(String
                .format("bad ref [%s] base=[%s] sub=[%s]", ref, script,
                        subpath));

        List<String> elems = new ArrayList<>(List.of(PATH_SEPS.split(ref, -1)));

        /* Normalize the reference by appending an empty element when
         * the last element is a dot or double-dot. */
        if (!elems.isEmpty()) {
            switch (elems.get(elems.size() - 1)) {
            case ".", ".." -> elems.add("");
            }
        }

        /* Normalize the reference by removing "." and ".." elements. */
        try {
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
        } catch (NoSuchElementException ex) {
            throw new IllegalArgumentException(String
                .format("bad ref [%s] base=[%s] sub=[%s]", ref, script,
                        subpath));
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
        {
            String sep = "";
            for (String elem : elems) {
                result.append(sep);
                sep = "/";
                escapePathElement(result, elem);
            }
        }
        if (result.isEmpty()) result.append("./");
        logger.finer(() -> String
            .format("script=%s subpath=%s " + "ref=%s result=%s", script,
                    subpath, ref, result));

        if (params != null) {
            result.append('?');
            String sep = "";
            for (var kv : params) {
                result.append(sep);
                sep = "&";

                var k = kv.getKey();
                var v = kv.getValue();
                escapeParam(result, k);
                result.append('=');
                escapeParam(result, v);
            }
        }

        if (fragment != null) escapeFragment(result.append('#'), fragment);

        return URI.create(result.toString());
    }

    private static StringBuilder escapeParam(StringBuilder output,
                                             CharSequence input) {
        for (int cp : input.codePoints().toArray()) {
            switch (cp) {
            default -> output.appendCodePoint(cp);
            case ' ' -> output.append('+');
            /* TODO: Work out full set of characters. */
            case '=', '&', '+', '#', ':' -> appendPercentCodepoint(output, cp);
            }
        }
        return output;
    }

    private static StringBuilder escapePathElement(StringBuilder output,
                                                   CharSequence input) {
        for (int cp : input.codePoints().toArray()) {
            switch (cp) {
            default -> output.appendCodePoint(cp);
            /* TODO: Work out full set of characters. */
            case ':', '/', '?', '#', ' ' -> appendPercentCodepoint(output, cp);
            }
        }
        return output;
    }

    private static StringBuilder escapeFragment(StringBuilder output,
                                                CharSequence input) {
        for (int cp : input.codePoints().toArray()) {
            switch (cp) {
            default -> output.appendCodePoint(cp);
            /* TODO: Work out full set of characters. */
            case '#', ' ' -> appendPercentCodepoint(output, cp);
            }
        }
        return output;
    }

    private static final char[] hexes = "0123456789ABCDEF".toCharArray();

    private static StringBuilder appendPercentCodepoint(StringBuilder output,
                                                        int cp) {
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
            /* 1111:10xx 10xx:xxxx 10xx:xxxx 10xx:xxxx 10xx:xxxx */
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
        return output;
    }

    private static PathContext infer(URI scriptFilename, String pathInfo,
                                     String scriptName, String prefix) {
        prefix = normalizePrefix(prefix);
        if (pathInfo != null) {
            return new PathContext(prefix + scriptName, pathInfo);
        } else if ("proxy".equals(scriptFilename.getScheme())) {
            final URI ssp =
                URI.create(scriptFilename.getRawSchemeSpecificPart());
            final String virtualPath = ssp.getPath();
            if (scriptName.endsWith(virtualPath)) {
                final String correctedScriptName = scriptName
                    .substring(0, scriptName.length() - virtualPath.length());
                return new PathContext(prefix + correctedScriptName,
                                       virtualPath);
            }
        }
        return new PathContext(prefix + scriptName, "");
    }

    /**
     * Normalize a path prefix. The input is split by slashes, empty
     * elements are removed, then each element is prefixed with a slash,
     * and the elements are concatenated. If {@code null} is supplied,
     * an empty string is returned.
     * 
     * @param input the prefix to normalize
     * 
     * @return the normalized prefix
     */
    private static String normalizePrefix(CharSequence input) {
        if (input == null) return "";
        return Arrays.stream(PATH_SEPS.split(input, -1))
            .filter(s -> !s.isEmpty()).map(s -> "/" + s)
            .collect(Collectors.joining());
    }

    /**
     * Creates path contexts from CGI parameters.
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
     * <p>
     * In each case, the path script may be prefixed by a value possibly
     * derived from CGI parameters. This <dfn>external prefix</dfn>
     * defaults to an empty string. Any supplied prefix will be
     * normalized by ensuring that each path element begins with a
     * slash, and no element is empty. So, for example,
     * <samp>foo/bar/</samp> is normalized to <samp>/foo/bar</samp>.
     */
    public static final class Builder {
        private Function<? super Map<? super String, ? extends String>,
                         ? extends String> scriptFilename =
                             m -> m.get("SCRIPT_FILENAME");

        private Function<? super Map<? super String, ? extends String>,
                         ? extends String> pathInfo = m -> m.get("PATH_INFO");

        private Function<? super Map<? super String, ? extends String>,
                         ? extends String> scriptName =
                             m -> m.get("SCRIPT_NAME");

        private Function<? super Map<? super String, ? extends String>,
                         ? extends String> prefix = m -> "";

        /**
         * Specify how to obtain the script filename.
         * 
         * @param func a function taking CGI parameters and returning
         * the script filename
         * 
         * @return this builder
         */
        public Builder
            scriptFilename(Function<? super Map<? super String,
                                                ? extends String>,
                                    ? extends String> func) {
            this.scriptFilename = Objects.requireNonNull(func, "func");
            return this;
        }

        /**
         * Specify how to obtain the script name.
         * 
         * @param func a function taking CGI parameters and returning
         * the script name
         * 
         * @return this builder
         * 
         * @see <a href=
         * "https://datatracker.ietf.org/doc/html/rfc3875#section-4.1.13">RFC3875
         * Section 4.1.13</a>
         */
        public Builder
            scriptName(Function<? super Map<? super String, ? extends String>,
                                ? extends String> func) {
            this.scriptName = Objects.requireNonNull(func, "func");
            return this;
        }

        /**
         * Specify how to obtain the path information.
         * 
         * @param func a function taking CGI parameters and returning
         * the path information, or {@code null} if not available
         * 
         * @return this builder
         * 
         * @see <a href=
         * "https://datatracker.ietf.org/doc/html/rfc3875section-4.1.5">RFC3875
         * Section 4.1.5</a>
         */
        public Builder
            pathInfo(Function<? super Map<? super String, ? extends String>,
                              ? extends String> func) {
            this.pathInfo = Objects.requireNonNull(func, "func");
            return this;
        }

        /**
         * Specify how to obtain the external prefix.
         * 
         * @param func a function taking CGI parameters and returning
         * the external prefix
         * 
         * @return this builder
         */
        public Builder
            prefix(Function<? super Map<? super String, ? extends String>,
                            ? extends String> func) {
            this.prefix = Objects.requireNonNull(func, "func");
            return this;
        }

        /**
         * Specify a fixed external prefix.
         * 
         * @param text the external prefix
         * 
         * @return this builder
         */
        public Builder prefix(CharSequence text) {
            this.prefix = s -> text.toString();
            return this;
        }

        /**
         * Specify an external prefix derived from CGI parameters.
         * 
         * @param name the name of the CGI parameter
         * 
         * @return this builder
         */
        public Builder prefixFromCGI(String name) {
            return prefix(s -> s.get(name));
        }

        /**
         * Specify an external prefix derived from the environment. The
         * value is read immediately using
         * {@link System#getenv(String)}, and delivered as a constant.
         * 
         * @param name the name of the environment variable
         * 
         * @return this builder
         */
        public Builder prefixFromEnvironment(String name) {
            return prefix(System.getenv(name));
        }

        /**
         * Specify an external prefix derived from a system property.
         * The value is read immediately using
         * {@link System#getProperty(String)}, and delivered as a
         * constant.
         * 
         * @param name the name of the system property
         * 
         * @return this builder
         */
        public Builder prefixFromSystemProperty(String name) {
            return prefix(System.getProperty(name));
        }

        /**
         * Create a path context using the current settings and some CGI
         * parameters.
         * 
         * <p>
         * This method can be used multiple times on the same object to
         * obtain distinct path contexts.
         * 
         * @param params the CGI parameters
         * 
         * @return the path context
         */
        public PathContext build(Map<? super String, ? extends String> params) {
            URI scriptFilename = URI.create(this.scriptFilename.apply(params));
            String pathInfo = this.pathInfo.apply(params);
            String scriptName = this.scriptName.apply(params);
            String prefix = this.prefix.apply(params);
            return infer(scriptFilename, pathInfo, scriptName, prefix);
        }

        /**
         * Create a path context using the current settings and the CGI
         * parameters from a session context.
         * 
         * @param ctxt the session context
         * 
         * @return the path context
         */
        public PathContext build(SessionContext ctxt) {
            return build(ctxt.parameters());
        }

        private Builder() {}
    }

    /**
     * Prepare to create a path context. By default, the builder reads
     * <samp>PATH_INFO</samp>, <samp>SCRIPT_FILENAME</samp> and
     * <samp>SCRIPT_NAME</samp> from the supplied CGI parameters, and
     * has an empty external prefix.
     * 
     * @return the new builder
     */
    public static Builder start() {
        return new Builder();
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

    private static final Logger logger =
        Logger.getLogger(PathContext.class.getPackageName());
}
