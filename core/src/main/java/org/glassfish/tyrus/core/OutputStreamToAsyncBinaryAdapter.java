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
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Simple Writer that writes its data to an async sink.
 *
 * @author Danny Coward (danny.coward at oracle.com)
 */
class OutputStreamToAsyncBinaryAdapter extends OutputStream {
    private final TyrusWebSocket socket;

    public OutputStreamToAsyncBinaryAdapter(TyrusWebSocket socket) {
        this.socket = socket;
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        final Future<?> future = socket.sendBinary(b, off, len, false);
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                throw new IOException(e.getCause());
            }
        }
    }

    @Override
    public void write(int i) throws IOException {
        byte[] byteArray = new byte[]{(byte) i};

        write(byteArray, 0, byteArray.length);
    }

    @Override
    public void flush() throws IOException {
        // do nothing.
    }

    @Override
    public void close() throws IOException {
        socket.sendBinary(new byte[]{}, true);
    }
}
