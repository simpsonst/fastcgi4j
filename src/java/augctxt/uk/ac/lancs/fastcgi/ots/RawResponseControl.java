// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2026, Lancaster University
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

package uk.ac.lancs.fastcgi.ots;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import uk.ac.lancs.fastcgi.ResponderSession;
import uk.ac.lancs.fastcgi.ots.OTSControl;
import uk.ac.lancs.http.field.FieldNames;
import uk.ac.lancs.mime.MediaType;

/**
 * Adapts a raw responder session to off-the-shelf responses.
 * 
 * @author simpsons
 */
public class RawResponseControl implements OTSControl {
    final ResponderSession base;

    /**
     * Adapt a raw session to off-the-shelf responses.
     * 
     * @param base the base session
     */
    public RawResponseControl(ResponderSession base) {
        this.base = base;
    }

    @Override
    public void setStatus(int code) {
        base.setStatus(code);
    }

    @Override
    public void clearContentType() {
        base.clearField(FieldNames.CONTENT_TYPE);
    }

    @Override
    public void clearContentLength() {
        base.clearField(FieldNames.CONTENT_LENGTH);
    }

    @Override
    public void clearContentEncoding() {
        base.clearField(FieldNames.CONTENT_ENCODING);
    }

    @Override
    public OutputStream out() throws IOException {
        return base.out();
    }

    @Override
    public void setLocation(URI location) {
        base.setField(FieldNames.LOCATION, location.toASCIIString());
    }

    @Override
    public void setContentType(MediaType type) {
        base.setField(FieldNames.CONTENT_TYPE, type.toString());
    }
}
