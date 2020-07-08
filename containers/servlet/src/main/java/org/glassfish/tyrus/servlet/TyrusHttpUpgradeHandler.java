/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.websocket.CloseReason;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.WebConnection;

import org.glassfish.tyrus.core.CloseReasons;
import org.glassfish.tyrus.spi.Connection;
import org.glassfish.tyrus.spi.WebSocketEngine;
import org.glassfish.tyrus.spi.Writer;

/**
 * {@link HttpUpgradeHandler} and {@link ReadListener} implementation.
 * <p>
 * Reads data from {@link ServletInputStream} and passes it further to the Tyrus runtime.
 *
 * @author Jitendra Kotamraju
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class TyrusHttpUpgradeHandler implements HttpUpgradeHandler, ReadListener {

    public static final String FRAME_BUFFER_SIZE = "org.glassfish.tyrus.servlet.incoming-buffer-size";

    private final CountDownLatch connectionLatch = new CountDownLatch(1);

    private ServletInputStream is;
    private ServletOutputStream os;
    private WebConnection wc;
    private ByteBuffer buf;

    private volatile boolean closed = false;
    private int incomingBufferSize = 4194315; // 4M (payload) + 11 (frame overhead)

    private static final Logger LOGGER = Logger.getLogger(TyrusHttpUpgradeHandler.class.getName());

    private Connection connection;
    private WebSocketEngine.UpgradeInfo upgradeInfo;
    private Writer writer;


    private boolean authenticated = false;

    @Override
    public void init(WebConnection wc) {
        LOGGER.config("Servlet 3.1 Upgrade");
        try {
            is = wc.getInputStream();
            os = wc.getOutputStream();
            this.wc = wc;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        try {
            is.setReadListener(this);
        } catch (IllegalStateException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        connection = upgradeInfo.createConnection(writer, new Connection.CloseListener() {
            @Override
            public void close(CloseReason reason) {
                try {
                    TyrusHttpUpgradeHandler.this.getWebConnection().close();
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, e.getMessage(), e);
                }
            }
        });

        connectionLatch.countDown();
    }

    public void preInit(WebSocketEngine.UpgradeInfo upgradeInfo, Writer writer, boolean authenticated) {
        this.upgradeInfo = upgradeInfo;
        this.writer = writer;
        this.authenticated = authenticated;
    }

    @Override
    public void onDataAvailable() {
        try {
            connectionLatch.await();
        } catch (InterruptedException e) {
            // do nothing.
        }

        do {
            try {
                int available = is.available();
                if (available == 0) {
                    // tomcat impl returns always 0
                    available = 16384;
                }

                int toRead;
                if (buf == null) {
                    if (available > incomingBufferSize) {
                        toRead = incomingBufferSize;
                    } else {
                        toRead = available;
                    }
                } else {
                    if (buf.remaining() + available > incomingBufferSize) {
                        toRead = incomingBufferSize - buf.remaining();
                    } else {
                        toRead = buf.remaining() + available;
                    }
                }

                if (toRead == 0) {
                    throw new IOException(String.format("Tyrus input buffer exceeded. Current buffer size is %s bytes.",
                                                        incomingBufferSize));
                }

                available -= fillBuf(toRead);

                if (buf != null) {

                    LOGGER.finest(String.format("Remaining Data = %d", buf.remaining()));

                    if (buf.hasRemaining()) {
                        connection.getReadHandler().handle(buf);
                    }
                }
            } catch (IOException e) {
                connection.close(CloseReasons.CANNOT_ACCEPT.getCloseReason());
            }
        } while (!closed && is.isReady());
    }

    /**
     * Fill the buf with some more websocket protocol data.
     *
     * @param length length of data available to read.
     * @return legth of actually read data.
     * @throws IOException if some other I/O error occurs.
     */
    private int fillBuf(int length) throws IOException {
        byte[] data = new byte[length];
        int len = is.read(data);
        if (len == 0) {
            return 0;
        }
        if (buf == null) {
            LOGGER.finest("No Buffer. Allocating new one");
            buf = ByteBuffer.wrap(data);
            ((Buffer) buf).limit(len);
        } else {
            int limit = buf.limit();
            int capacity = buf.capacity();
            int remaining = buf.remaining();

            if (capacity - limit >= len) {
                // Remaining data need not be changed. New data is just appended
                LOGGER.finest("Remaining data need not be moved. New data is just appended");
                ((Buffer) buf).mark();
                ((Buffer) buf).position(limit);
                ((Buffer) buf).limit(capacity);
                buf.put(data, 0, len);
                ((Buffer) buf).limit(limit + len);
                ((Buffer) buf).reset();
            } else if (remaining + len < capacity) {
                // Remaining data is moved to left. Then new data is appended
                LOGGER.finest("Remaining data is moved to left. Then new data is appended");
                buf.compact();
                buf.put(data, 0, len);
                ((Buffer) buf).flip();
            } else {
                // Remaining data + new > capacity. So allocate new one
                LOGGER.finest("Remaining data + new > capacity. So allocate new one");
                byte[] array = new byte[remaining + len];
                buf.get(array, 0, remaining);
                System.arraycopy(data, 0, array, remaining, len);
                buf = ByteBuffer.wrap(array);
                ((Buffer) buf).limit(remaining + len);
            }
        }

        return len;
    }

    @Override
    public void onAllDataRead() {
        close(CloseReason.CloseCodes.NORMAL_CLOSURE.getCode(), null);
    }

    @Override
    public void onError(Throwable t) {
        close(CloseReason.CloseCodes.CLOSED_ABNORMALLY.getCode(),
              t.getMessage() == null ? "No reason given." : t.getMessage());
    }

    @Override
    public void destroy() {
        close(CloseReason.CloseCodes.CLOSED_ABNORMALLY.getCode(), "No reason given.");
    }

    /**
     * Called when related {@link jakarta.servlet.http.HttpSession} is destroyed or invalidated.
     * <p>
     * Implementation is required to call onClose() on server-side with corresponding close code (1008, see
     * WebSocket spec 7.2) - only when there is an authorized user for this session.
     */
    public void sessionDestroyed() {
        if (authenticated) {
            // websocket spec 7.2 [WSC-7.2-3]
            httpSessionForcedClose(CloseReason.CloseCodes.VIOLATED_POLICY.getCode(), "No reason given.");
        }

        // else do nothing.
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TyrusHttpUpgradeHandler{");
        sb.append("is=").append(is);
        sb.append(", os=").append(os);
        sb.append(", wc=").append(wc);
        sb.append(", closed=").append(closed);
        sb.append('}');
        return sb.toString();
    }

    public void setIncomingBufferSize(int incomingBufferSize) {
        this.incomingBufferSize = incomingBufferSize;
    }

    private void httpSessionForcedClose(int closeCode, String closeReason) {
        if (!closed) {
            try {
                // TODO
                // initiates connection close without sending close frame to the client - session is already invalidated
                // so we should not send anything.
                // ((TyrusWebSocket) ((TyrusWebSocketEngine) engine).getWebSocketHolder(writer).webSocket).setClosed();
                connection.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(closeCode), closeReason));
                closed = true;
                wc.close();
            } catch (Exception e) {
                LOGGER.log(Level.CONFIG, e.getMessage(), e);
            }
        }
    }

    private void close(int closeCode, String closeReason) {
        if (!closed) {
            try {
                connection.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(closeCode), closeReason));
                closed = true;
                wc.close();
            } catch (Exception e) {
                LOGGER.log(Level.CONFIG, e.getMessage(), e);
            }
        }
    }

    WebConnection getWebConnection() {
        if (wc == null) {
            throw new IllegalStateException();
        }
        return wc;
    }
}
