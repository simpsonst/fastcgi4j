// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2022,2023,2026, Lancaster University
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

package uk.ac.lancs.http;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Provides utilities for converting between a millisecond timestamp and
 * a textual representation as used in HTTP headers and trailers. The
 * three formats recognized are (using an example date and time):
 * 
 * <ol>
 * 
 * <li><samp>Sun, 06 Nov 1994 08:49:37 GMT</samp>, i.e., as defined by
 * {@link DateTimeFormatter#RFC_1123_DATE_TIME};
 * 
 * <li><samp>Sunday, 06-Nov-94 08:49:37 GMT</samp>, i.e., as defined by
 * RFC1036;
 * 
 * <li><samp>Sun Nov 6 08:49:37 1994</samp>
 * 
 * </ol>
 * 
 * <p>
 * Generation of a textual representation uses the first format
 * exclusively. Parsing is attempted with each format in the order
 * presented above.
 *
 * @author simpsons
 */
public final class Timing {
    static {
        Collection<DateTimeFormatter> formats = new ArrayList<>();
        /* RFC1123 "Sun, 06 Nov 1994 08:49:37 GMT" */
        formats.add(DateTimeFormatter.RFC_1123_DATE_TIME);
        /* RFC1036 "Sunday, 06-Nov-94 08:49:37 GMT" */
        formats.add(DateTimeFormatter.ofPattern("EEEE, dd-LLL-uu kk:mm:ss z",
                                                Locale.UK));
        /* asctime() "Sun Nov 6 08:49:37 1994" */
        formats.add(DateTimeFormatter
            .ofPattern("EEE LLL d kk:mm:ss yyyy", Locale.US)
            .withZone(ZoneOffset.UTC));
        FORMATS = List.copyOf(formats);
    }

    static final Collection<DateTimeFormatter> FORMATS;

    /**
     * Parse a timestamp according to one of three formats that could be
     * encountered in an HTTP header or trailer.
     *
     * @param text the textual representation of the timestamp
     *
     * @return the corresponding time in milliseconds since the epoch;
     * or {@link Long#MIN_VALUE} if it failed to parse
     */
    public static long parseTimestamp(CharSequence text) {
        for (DateTimeFormatter f : FORMATS) {
            try {
                ZonedDateTime z = ZonedDateTime.parse(text, f);
                return Instant.from(z).toEpochMilli();
            } catch (DateTimeParseException ex) {
                /* Just try the next one. */
            }
        }
        return Long.MIN_VALUE;
    }

    /**
     * Generate a textual representation of a timestamp. The format is
     * the subset of RFC1123. UTC is always used.
     *
     * @param millis the timestamp in milliseconds since the epoch
     *
     * @return the textual representation of the timestamp
     */
    public static String generateTimestamp(long millis) {
        return ZonedDateTime
            .ofInstant(Instant.ofEpochMilli(millis), ZoneId.of("Z"))
            .format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    private Timing() {}
}
