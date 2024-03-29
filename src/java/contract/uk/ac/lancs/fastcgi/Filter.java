/*
 * Copyright (C) 2022 Regents of the University of Lancaster
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ac.lancs.fastcgi;

import java.io.IOException;
import uk.ac.lancs.fastcgi.context.SessionException;
import uk.ac.lancs.fastcgi.context.FilterSession;

/**
 * Augments a session by post-processing a partial response.
 * 
 * @author simpsons
 * 
 * @see <a href=
 * "https://fastcgi-archives.github.io/FastCGI_Specification.html#S6.4">FastCGI
 * Specification &mdash; Filter</a>
 */
public interface Filter {
    /**
     * Filter a session.
     * 
     * @param session the FastCGI session context
     * 
     * @throws InterruptedException if the application was interrupted
     * (usually by the server or the remote client aborting the session)
     * 
     * @throws SessionException if the application is temporarily unable
     * to respond to the request
     * 
     * @throws IOException if an I/O error occurs in processing any of
     * the streams of the session context
     * 
     * @throws Exception if something else goes wrong, to be logged by
     * the library
     */
    void filter(FilterSession session) throws Exception;
}
