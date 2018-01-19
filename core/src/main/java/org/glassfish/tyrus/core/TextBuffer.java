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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.tyrus.core.l10n.LocalizationMessages;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
class TextBuffer {
    private StringBuffer buffer;
    private int bufferSize;
    private static final Logger LOGGER = Logger.getLogger(BinaryBuffer.class.getName());

    void appendMessagePart(String message) {
        if (message == null || message.length() == 0) {
            return;
        }

        if (buffer.length() + message.length() <= bufferSize) {
            buffer.append(message);
        } else {
            final MessageTooBigException messageTooBigException =
                    new MessageTooBigException(LocalizationMessages.PARTIAL_MESSAGE_BUFFER_OVERFLOW());
            LOGGER.log(Level.FINE, LocalizationMessages.PARTIAL_MESSAGE_BUFFER_OVERFLOW(), messageTooBigException);
            throw messageTooBigException;
        }
    }

    String getBufferedContent() {
        return buffer.toString();
    }

    void resetBuffer(int bufferSize) {
        this.bufferSize = bufferSize;
        this.buffer = new StringBuffer();
    }
}
