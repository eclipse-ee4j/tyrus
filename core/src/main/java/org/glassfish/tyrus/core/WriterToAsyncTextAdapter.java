/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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
import java.io.Writer;

/**
 * Simple Writer that writes its data to an async sink.
 *
 * @author Danny Coward (danny.coward at oracle.com)
 */
class WriterToAsyncTextAdapter extends Writer {
    private final TyrusWebSocket socket;
    private String buffer = null;

    public WriterToAsyncTextAdapter(TyrusWebSocket socket) {
        this.socket = socket;
    }

    private void sendBuffer(boolean last) {
        socket.sendText(buffer, last);
    }

    @Override
    public void write(char[] chars, int index, int len) throws IOException {
        if (buffer != null) {
            this.sendBuffer(false);
        }
        buffer = (new String(chars)).substring(index, index + len);

    }

    @Override
    public void flush() throws IOException {
        if (buffer != null) {
            this.sendBuffer(false);
        }
        buffer = null;
    }

    @Override
    public void close() throws IOException {
        this.sendBuffer(true);
    }
}
