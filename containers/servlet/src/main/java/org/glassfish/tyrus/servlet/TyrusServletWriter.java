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

package org.glassfish.tyrus.servlet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.glassfish.tyrus.spi.CompletionHandler;
import org.glassfish.tyrus.spi.Writer;

/**
 * {@link org.glassfish.tyrus.spi.Writer} implementation used in Servlet integration.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
class TyrusServletWriter extends Writer implements WriteListener {

    private final TyrusHttpUpgradeHandler tyrusHttpUpgradeHandler;
    private final Deque<QueuedFrame> queue = new LinkedList<QueuedFrame>();

    private static final Logger LOGGER = Logger.getLogger(TyrusServletWriter.class.getName());

    /**
     * ServletOutputStream is not thread safe, must be synchronized.
     * <p/>
     * Access synchronized via "this" - Tyrus creates one instance of TyrusServletWriter per WebSocket connection, so
     * that should be ok.
     */
    private ServletOutputStream servletOutputStream = null;

    private boolean isListenerSet;

    private static class QueuedFrame {
        public final CompletionHandler<ByteBuffer> completionHandler;
        public final ByteBuffer dataFrame;

        QueuedFrame(CompletionHandler<ByteBuffer> completionHandler, ByteBuffer dataFrame) {
            this.completionHandler = completionHandler;
            this.dataFrame = dataFrame;
        }
    }

    /**
     * Constructor.
     *
     * @param tyrusHttpUpgradeHandler encapsulated {@link TyrusHttpUpgradeHandler} instance.
     */
    public TyrusServletWriter(TyrusHttpUpgradeHandler tyrusHttpUpgradeHandler) {
        this.tyrusHttpUpgradeHandler = tyrusHttpUpgradeHandler;
    }

    @Override
    public synchronized void onWritePossible() throws IOException {
        LOGGER.log(Level.FINEST, "OnWritePossible called");

        while (!queue.isEmpty() && servletOutputStream.isReady()) {
            final QueuedFrame queuedFrame = queue.poll();
            assert queuedFrame != null;

            _write(queuedFrame.dataFrame, queuedFrame.completionHandler);
        }
    }

    @Override
    public synchronized void onError(Throwable t) {
        LOGGER.log(Level.WARNING, "TyrusServletWriter.onError", t);

        QueuedFrame queuedFrame;
        while ((queuedFrame = queue.poll()) != null) {
            queuedFrame.completionHandler.failed(t);
        }
    }

    @Override
    public synchronized void write(final ByteBuffer buffer, CompletionHandler<ByteBuffer> completionHandler) {

        // first write
        if (servletOutputStream == null) {
            try {
                servletOutputStream = tyrusHttpUpgradeHandler.getWebConnection().getOutputStream();
            } catch (IOException e) {
                LOGGER.log(Level.CONFIG, "ServletOutputStream cannot be obtained", e);
                completionHandler.failed(e);
                return;
            }
        }

        if (queue.isEmpty() && servletOutputStream.isReady()) {
            _write(buffer, completionHandler);
        } else {
            final QueuedFrame queuedFrame = new QueuedFrame(completionHandler, buffer);
            queue.offer(queuedFrame);

            if (!isListenerSet) {
                isListenerSet = true;
                servletOutputStream.setWriteListener(this);
            }
        }
    }

    private void _write(ByteBuffer buffer, CompletionHandler<ByteBuffer> completionHandler) {

        try {
            if (buffer.hasArray()) {
                byte[] array = buffer.array();
                servletOutputStream.write(array, buffer.arrayOffset() + buffer.position(), buffer.remaining());
            } else {
                final int remaining = buffer.remaining();
                final byte[] array = new byte[remaining];
                buffer.get(array);
                servletOutputStream.write(array);
            }

            servletOutputStream.flush();

            if (completionHandler != null) {
                completionHandler.completed(buffer);
            }
        } catch (Exception e) {
            if (completionHandler != null) {
                completionHandler.failed(e);
            }
        }
    }

    @Override
    public void close() {
        try {
            tyrusHttpUpgradeHandler.getWebConnection().close();
        } catch (Exception e) {
            // do nothing.
        }
    }
}
