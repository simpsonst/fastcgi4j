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

package uk.ac.lancs.fastcgi.body;

import java.io.IOException;
import java.io.Reader;
import java.lang.ref.Cleaner;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Retrieves text data from a file.
 * 
 * @author simpsons
 */
final class FileTextBody extends TransientFileElement implements TextBody {
    private final Charset charset;

    private final long size;

    /**
     * Record the storage of text data in a file. The size and character
     * encoding must be known beforehand, and the data must be in the
     * file before {@link #recover()} is called.
     * 
     * @param path the path to the file
     * 
     * @param charset the character encoding
     * 
     * @param byteSize the size of the file in bytes
     * 
     * @param charSize the number of characters in the file
     */
    public FileTextBody(Cleaner cleaner, Path path, Charset charset,
                        long byteSize, long charSize, AtomicLong usage) {
        super(cleaner, path, byteSize, usage);
        this.charset = charset;
        this.size = charSize;
    }

    @Override
    public long size() {
        return size;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws AssertionError if there is an error in opening the file
     */
    @Override
    public Reader recover() throws IOException {
        return Files.newBufferedReader(super.path(), charset);
    }
}