// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides static methods and regular expressions for handling paths.
 *
 * @author simpsons
 */
class Utils {
    /**
     * Matches sequences of at least one forward slash.
     */
    static final Pattern PATH_SEPS = Pattern.compile("/+");

    private static final char[] hexes = "0123456789ABCDEF".toCharArray();

    static final Pattern SERVER_PROTOCOL_FORMAT =
        Pattern.compile("^(?<name>[^/]+)/(?<major>[0-9]+)\\.(?<minor>[0-9]+)$");

    /**
     * Convert a path prefix to a list of elements. Empty elements are
     * removed. Providing an empty string or {@code null} yields an
     * empty list. Relative components are not permitted.
     * 
     * @param prefix the prefix to decompose
     * 
     * @return an immutable list of path elements
     * 
     * @throws PathElementException if an element is a dot or double dot
     */
    static List<String> decomposePathPrefix(String prefix) {
        if (prefix == null) return Collections.emptyList();
        List<String> result = Arrays.stream(Utils.PATH_SEPS.split(prefix, -1))
            .filter(s -> !s.isEmpty()).toList();
        if (result.contains("."))
            throw new PathElementException(".", "not permitted in prefix");
        if (result.contains(".."))
            throw new PathElementException("..", "not permitted in prefix");
        return result;
    }

    static String composePathPrefix(List<? extends String> elems) {
        return composePathPrefix(elems.stream());
    }

    static String composePathPrefix(Stream<? extends String> elems) {
        return Stream.concat(elems, Stream.of("")).map(s -> "/" + s)
            .collect(Collectors.joining());
    }

    /**
     * Normalize an internal path reference. The path is split into
     * elements by forward slashes. Multiple adjacent slashes are
     * treated as single slashes. A leading slash is assumed, so the
     * result always begins with an empty element. If the last element
     * is a dot or double dot, an empty element is appended. Dot
     * elements are then removed, and each double-dot element and its
     * preceding element will be removed.
     * 
     * @param path the input path reference
     * 
     * @return a modifiable list of path elements
     * 
     * @throws ExternalPathException if a double-dot element does not
     * have a preceding element
     */
    static List<String> decomposeInternalPath(String path) {
        /* Split the path on forward slashes. Treat multiple adjacent
         * slashes as a single slash. Acknowledge both leading and
         * trailing slashes by allowing them to result in empty
         * elements. */
        List<String> elems =
            new ArrayList<>(List.of(PATH_SEPS.split(path, -1)));

        /* Append an empty element when the last element is a dot or
         * double-dot, as these implicitly refer to a directory-like
         * resource. */
        assert elems.isEmpty();
        switch (elems.get(elems.size() - 1)) {
        case ".", ".." -> elems.add("");
        }

        /* The first element must be an empty string. If the input began
         * with a slash, it's already there; otherwise, insert one. */
        if (!elems.get(0).isEmpty()) elems.add(0, "");

        /* Normalize "." and ".." elements. */
        for (ListIterator<String> iter = elems.listIterator();
             iter.hasNext();) {
            String value = iter.next();
            if (value.equals(".")) {
                iter.remove();
            } else if (value.equals("..")) {
                iter.remove();
                if (!iter.hasPrevious()) throw new ExternalPathException(path);
                iter.previous();
                iter.remove();
            }
        }

        return elems;
    }

    static StringBuilder escapePathElement(StringBuilder output,
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

    static StringBuilder escapeFragment(StringBuilder output,
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

    static StringBuilder escapeParam(StringBuilder output, CharSequence input) {
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

    static StringBuilder appendPercentCodepoint(StringBuilder output, int cp) {
        if (cp < 128) {
            /* 0xxx:xxxx */
            output.append('%').append(hexes[(cp >>> 4) & 15])
                .append(hexes[cp & 15]);
        } else if (cp < 2048) {
            /* 110x:xxxx 10xx:xxxx */
            output.append('%').append(hexes[(cp >>> 10) | 12])
                .append(hexes[(cp >>> 6) & 15]).append('%')
                .append(hexes[(cp >>> 4) & 3 | 8]).append(hexes[cp & 15]);
        } else if (cp < 65536) {
            /* 1110:xxxx 10xx:xxxx 10xx:xxxx */
            output.append("%E").append(hexes[cp >>> 12]).append('%')
                .append(hexes[(cp >>> 10) & 15 | 8])
                .append(hexes[(cp >>> 6) & 15]).append('%')
                .append(hexes[(cp >>> 4) & 15 | 8]).append(hexes[cp & 15]);
        } else if (cp < 2097152) {
            /* 1111:0xxx 10xx:xxxx 10xx:xxxx 10xx:xxxx */
            output.append("%F").append(hexes[cp >>> 18]).append('%')
                .append(hexes[(cp >>> 16) & 15 | 8])
                .append(hexes[(cp >>> 12) & 15]).append('%')
                .append(hexes[(cp >>> 10) & 15 | 8])
                .append(hexes[(cp >>> 6) & 15]).append('%')
                .append(hexes[(cp >>> 4) & 15 | 8]).append(hexes[cp & 15]);
        } else if (cp < 67108864) {
            /* 1111:10xx 10xx:xxxx 10xx:xxxx 10xx:xxxx 10xx:xxxx */
            output.append("%F").append(hexes[cp >>> 24] | 8).append('%')
                .append(hexes[(cp >>> 22) & 15 | 8])
                .append(hexes[(cp >>> 18) & 15]).append('%')
                .append(hexes[(cp >>> 16) & 15 | 8])
                .append(hexes[(cp >>> 12) & 15]).append('%')
                .append(hexes[(cp >>> 10) & 15 | 8])
                .append(hexes[(cp >>> 6) & 15]).append('%')
                .append(hexes[(cp >>> 4) & 15 | 8]).append(hexes[cp & 15]);
        } else {
            assert cp < -2147483648;
            /* 1111:110x 10xx:xxxx 10xx:xxxx 10xx:xxxx 10xx:xxxx
             * 10xx:xxxx */
            output.append("%F").append(hexes[cp >>> 30] | 12).append('%')
                .append(hexes[(cp >>> 28) & 15 | 8])
                .append(hexes[(cp >>> 24) & 15]).append('%')
                .append(hexes[(cp >>> 22) & 15 | 8])
                .append(hexes[(cp >>> 18) & 15]).append('%')
                .append(hexes[(cp >>> 16) & 15 | 8])
                .append(hexes[(cp >>> 12) & 15]).append('%')
                .append(hexes[(cp >>> 10) & 15 | 8])
                .append(hexes[(cp >>> 6) & 15]).append('%')
                .append(hexes[(cp >>> 4) & 15 | 8]).append(hexes[cp & 15]);
        }
        return output;
    }

    /**
     * Append the host and port based on protocol.
     *
     * <p>
     * The protocol name <samp>HTTP</samp> is recognized, and the value
     * of <samp>HTTP_HOST</samp> is appended if present.
     *
     * @param result the string to append to
     *
     * @param protocol the protocol
     *
     * @param params other CGI parameters
     *
     * @return {@code true} if the protocol was recognized and an
     * address was appended; {@code false} otherwise
     */
    static boolean
        appendProtocolAddress(StringBuilder result, String protocol,
                              Map<? super String, ? extends String> params) {
        switch (protocol) {
        case "HTTP" -> {
            String httpHost = params.get("HTTP_HOST");
            if (httpHost != null) {
                result.append(httpHost);
                return true;
            }
            return false;
        }
        default -> {
            return false;
        }
        }
    }

    /**
     * Get the internal server address from CGI parameters. The
     * parameters <samp>REQUEST_SCHEME</samp>, <samp>SERVER_NAME</samp>
     * and <samp>SERVER_PORT</samp> are used. However, if the protocol
     * is recognized, and supports virtual hosting, the virtual host is
     * used instead of <samp>SERVER_NAME</samp> and
     * <samp>SERVER_PORT</samp>.
     *
     * <p>
     * Only HTTP is recognized, and the value of <samp>HTTP_HOST</samp>
     * specifies both the host and port.
     *
     * @param params CGI parameters
     *
     * @return a URI consisting of scheme, host and optional port, based
     * on the supplied CGI parameters
     */
    static URI getInternalServer(Map<? super String, ? extends String> params) {
        /* Identify the protocol and version. */
        String protocolText = Objects
            .requireNonNull(params.get("SERVER_PROTOCOL"), "SERVER_PROTOCOL");
        Matcher protocol = SERVER_PROTOCOL_FORMAT.matcher(protocolText);
        if (!protocol.matches())
            throw new IllegalArgumentException("bad SERVER_PROTOCOL: "
                + protocolText);
        StringBuilder result = new StringBuilder();
        final String scheme = params.get("REQUEST_SCHEME");
        result.append(scheme).append("://");
        if (!appendProtocolAddress(result, protocol.group("name"), params)) {
            /* We don't understand the protocol, so just use the generic
             * name and port. Omit the port if we know it's the default
             * for the scheme. */
            result.append(params.get("SERVER_NAME"));
            int port = Integer.parseInt(params.get("SERVER_PORT"));
            if (port != defaultPort(scheme)) result.append(':').append(port);
        }
        return URI.create(result.toString());
    }

    /**
     * Get the default port for a given scheme.
     *
     * @param scheme the scheme
     *
     * @return the default port; or <code>-1</code> if unknown
     */
    static int defaultPort(String scheme) {
        return switch (scheme) {
        case "http" -> 80;
        case "https" -> 443;
        default -> -1;
        };
    }

    private Utils() {}
}
