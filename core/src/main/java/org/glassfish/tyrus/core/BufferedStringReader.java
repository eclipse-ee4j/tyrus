/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.tyrus.core;

import java.io.IOException;
import java.io.Reader;

/**
 * Passed to the (@link MessageHandler.Whole} in case that partial messages are being received.
 *
 * @author Danny Coward (danny.coward at oracle.com)
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
class BufferedStringReader extends Reader {
    private final ReaderBuffer readerBuffer;

    /**
     * Constructor.
     *
     * @param readerBuffer underlying buffer.
     */
    public BufferedStringReader(ReaderBuffer readerBuffer) {
        this.readerBuffer = readerBuffer;
    }

    @Override
    public int read(char[] destination, int offsetToStart, int numberOfChars) throws IOException {
        char[] got = readerBuffer.getNextChars(numberOfChars);
        if (got != null) {
            System.arraycopy(got, 0, destination, offsetToStart, got.length);
            return got.length;
        } else {
            return -1;
        }
    }

    @Override
    public void close() {
        this.readerBuffer.finishReading();
    }
}
