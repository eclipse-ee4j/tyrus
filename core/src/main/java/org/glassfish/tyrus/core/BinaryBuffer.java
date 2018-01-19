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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.tyrus.core.l10n.LocalizationMessages;

/**
 * Save received partial messages to a list and concatenate them.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
class BinaryBuffer {
    private final List<ByteBuffer> list = new ArrayList<ByteBuffer>();
    private int bufferSize;
    private int currentlyBuffered = 0;
    private static final Logger LOGGER = Logger.getLogger(BinaryBuffer.class.getName());

    /**
     * Append buffer.
     * <p>
     * Actual implementation just stores the buffer instance in list.
     *
     * @param message to be buffered.
     */
    void appendMessagePart(ByteBuffer message) {

        if ((currentlyBuffered + message.remaining()) <= bufferSize) {
            currentlyBuffered += message.remaining();
            list.add(message);
        } else {
            final MessageTooBigException messageTooBigException = new MessageTooBigException(
                    LocalizationMessages.PARTIAL_MESSAGE_BUFFER_OVERFLOW());
            LOGGER.log(Level.FINE, LocalizationMessages.PARTIAL_MESSAGE_BUFFER_OVERFLOW(), messageTooBigException);
            throw messageTooBigException;
        }
    }

    /**
     * Return concatenated list of buffers and reset internal state.
     *
     * @return concatenated buffer.
     */
    ByteBuffer getBufferedContent() {
        ByteBuffer b = ByteBuffer.allocate(currentlyBuffered);

        for (ByteBuffer buffered : list) {
            b.put(buffered);
        }

        b.flip();
        resetBuffer(0);
        return b;
    }

    /**
     * Reset buffer with setting maximal buffer size.
     *
     * @param bufferSize max buffer size.
     */
    void resetBuffer(int bufferSize) {
        this.bufferSize = bufferSize;
        this.list.clear();
        currentlyBuffered = 0;
    }
}

