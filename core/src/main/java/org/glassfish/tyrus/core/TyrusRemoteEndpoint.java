/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;

import org.glassfish.tyrus.core.l10n.LocalizationMessages;
import org.glassfish.tyrus.spi.WriterInfo;
import org.glassfish.tyrus.spi.WriterInfo.MessageType;
import org.glassfish.tyrus.spi.WriterInfo.RemoteEndpointType;

import static org.glassfish.tyrus.core.Utils.checkNotNull;

/**
 * Wraps the {@link javax.websocket.RemoteEndpoint} and represents the other side of the websocket connection.
 *
 * @author Danny Coward (danny.coward at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public abstract class TyrusRemoteEndpoint implements javax.websocket.RemoteEndpoint {

    final TyrusSession session;
    final TyrusWebSocket webSocket;

    private final TyrusEndpointWrapper endpointWrapper;

    private static final Logger LOGGER = Logger.getLogger(TyrusRemoteEndpoint.class.getName());

    private TyrusRemoteEndpoint(TyrusSession session, TyrusWebSocket socket, TyrusEndpointWrapper endpointWrapper) {
        this.webSocket = socket;
        this.endpointWrapper = endpointWrapper;
        this.session = session;
    }

    static class Basic extends TyrusRemoteEndpoint implements javax.websocket.RemoteEndpoint.Basic {

        Basic(TyrusSession session, TyrusWebSocket socket, TyrusEndpointWrapper endpointWrapper) {
            super(session, socket, endpointWrapper);
        }

        @Override
        public void sendText(String text) throws IOException {
            checkNotNull(text, "text");

            session.getDebugContext()
                   .appendLogMessage(LOGGER, Level.FINEST, DebugContext.Type.MESSAGE_OUT, "Sending text message: ",
                                     text);

            final Future<?> future = webSocket.sendText(text, new WriterInfo(MessageType.TEXT, RemoteEndpointType.BASIC));
            try {
                processFuture(future);
            } finally {
                session.restartIdleTimeoutExecutor();
            }
        }

        @Override
        public void sendBinary(ByteBuffer data) throws IOException {
            checkNotNull(data, "data");

            session.getDebugContext()
                   .appendLogMessage(LOGGER, Level.FINEST, DebugContext.Type.MESSAGE_OUT, "Sending binary message");

            final Future<?> future = webSocket.sendBinary(Utils.getRemainingArray(data),
                    new WriterInfo(WriterInfo.MessageType.BINARY, WriterInfo.RemoteEndpointType.BASIC));
            try {
                processFuture(future);
            } finally {
                session.restartIdleTimeoutExecutor();
            }
        }

        @Override
        public void sendText(String partialMessage, boolean isLast) throws IOException {
            checkNotNull(partialMessage, "partialMessage");

            session.getDebugContext().appendLogMessage(LOGGER, Level.FINEST, DebugContext.Type.MESSAGE_OUT,
                                                       "Sending partial text message: ", partialMessage);

            final Future<?> future = webSocket.sendText(partialMessage, isLast,
                    new WriterInfo(isLast ? MessageType.TEXT : MessageType.TEXT_CONTINUATION, RemoteEndpointType.BASIC));
            try {
                processFuture(future);
            } finally {
                session.restartIdleTimeoutExecutor();
            }
        }

        @Override
        public void sendBinary(ByteBuffer partialByte, boolean isLast) throws IOException {
            checkNotNull(partialByte, "partialByte");

            session.getDebugContext().appendLogMessage(LOGGER, Level.FINEST, DebugContext.Type.MESSAGE_OUT,
                                                       "Sending partial binary message");

            final Future<?> future = webSocket.sendBinary(Utils.getRemainingArray(partialByte), isLast,
                    new WriterInfo(isLast ? MessageType.BINARY : MessageType.BINARY_CONTINUATION, RemoteEndpointType.BASIC));
            try {
                processFuture(future);
            } finally {
                session.restartIdleTimeoutExecutor();
            }
        }

        /**
         * Wait for the future to be completed.
         * <p>
         * {@link java.util.concurrent.Future#get()} will be invoked and exception processed (if thrown).
         *
         * @param future to be processed.
         * @throws IOException when {@link java.io.IOException} is the cause of thrown {@link
         *                     java.util.concurrent.ExecutionException} it will be extracted and rethrown. Otherwise
         *                     whole ExecutionException will be rethrown wrapped in {@link java.io.IOException}.
         */
        private void processFuture(Future<?> future) throws IOException {
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
        public void sendObject(Object data) throws IOException, EncodeException {
            checkNotNull(data, "data");
            final Future<?> future = sendSyncObject(data, new WriterInfo(MessageType.OBJECT, RemoteEndpointType.BASIC));
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                } else if (e.getCause() instanceof EncodeException) {
                    throw (EncodeException) e.getCause();
                } else {
                    throw new IOException(e.getCause());
                }
            }
            session.restartIdleTimeoutExecutor();
        }

        @Override
        public OutputStream getSendStream() throws IOException {
            return new OutputStreamToAsyncBinaryAdapter(webSocket);
        }

        @Override
        public Writer getSendWriter() throws IOException {
            return new WriterToAsyncTextAdapter(webSocket);
        }
    }

    static class Async extends TyrusRemoteEndpoint implements javax.websocket.RemoteEndpoint.Async {
        private long sendTimeout;

        Async(TyrusSession session, TyrusWebSocket socket, TyrusEndpointWrapper endpointWrapper) {
            super(session, socket, endpointWrapper);

            if (session.getContainer() != null) {
                setSendTimeout(session.getContainer().getDefaultAsyncSendTimeout());
            }
        }

        @Override
        public void sendText(String text, SendHandler handler) {
            checkNotNull(text, "text");
            checkNotNull(handler, "handler");
            session.restartIdleTimeoutExecutor();
            sendAsync(text, handler, AsyncMessageType.TEXT);
        }

        @Override
        public Future<Void> sendText(String text) {
            checkNotNull(text, "text");
            session.restartIdleTimeoutExecutor();
            return sendAsync(text, AsyncMessageType.TEXT);
        }

        @Override
        public Future<Void> sendBinary(ByteBuffer data) {
            checkNotNull(data, "data");
            session.restartIdleTimeoutExecutor();
            return sendAsync(data, AsyncMessageType.BINARY);
        }

        @Override
        public void sendBinary(ByteBuffer data, SendHandler handler) {
            checkNotNull(data, "data");
            checkNotNull(handler, "handler");
            session.restartIdleTimeoutExecutor();
            sendAsync(data, handler, AsyncMessageType.BINARY);
        }

        @Override
        public void sendObject(Object data, SendHandler handler) {
            checkNotNull(data, "data");
            checkNotNull(handler, "handler");
            session.restartIdleTimeoutExecutor();
            sendAsync(data, handler, AsyncMessageType.OBJECT);
        }

        @Override
        public Future<Void> sendObject(Object data) {
            checkNotNull(data, "data");
            session.restartIdleTimeoutExecutor();
            return sendAsync(data, AsyncMessageType.OBJECT);
        }

        @Override
        public long getSendTimeout() {
            return sendTimeout;
        }

        @Override
        public void setSendTimeout(long timeoutmillis) {
            sendTimeout = timeoutmillis;
            webSocket.setWriteTimeout(timeoutmillis);
        }

        /**
         * Sends the message asynchronously.
         * <p>
         * IMPORTANT NOTE: There is no need to start new thread here. All writer operations are by default
         * asynchronous,
         * the only difference between sync and async writer are that sync send should wait for future.get().
         *
         * @param message message to be sent
         * @param type    message type
         * @return message sending callback {@link Future}
         */
        private Future<Void> sendAsync(final Object message, final AsyncMessageType type) {
            Future<?> result = null;

            switch (type) {
                case TEXT:
                    session.getDebugContext().appendLogMessage(
                            LOGGER, Level.FINEST, DebugContext.Type.MESSAGE_OUT, "Sending text message: ", message);
                    result = webSocket.sendText((String) message, new WriterInfo(MessageType.TEXT, RemoteEndpointType.ASYNC));
                    break;

                case BINARY:
                    session.getDebugContext().appendLogMessage(
                            LOGGER, Level.FINEST, DebugContext.Type.MESSAGE_OUT, "Sending binary message");
                    result = webSocket.sendBinary(Utils.getRemainingArray((ByteBuffer) message),
                                new WriterInfo(MessageType.BINARY, RemoteEndpointType.ASYNC));
                    break;

                case OBJECT:
                    result = sendSyncObject(message, new WriterInfo(MessageType.OBJECT, RemoteEndpointType.ASYNC));
                    break;
            }

            final Future<?> finalResult = result;

            return new Future<Void>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return finalResult.cancel(mayInterruptIfRunning);
                }

                @Override
                public boolean isCancelled() {
                    return finalResult.isCancelled();
                }

                @Override
                public boolean isDone() {
                    return finalResult.isDone();
                }

                @Override
                public Void get() throws InterruptedException, ExecutionException {
                    finalResult.get();
                    return null;
                }

                @Override
                public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                        TimeoutException {
                    finalResult.get(timeout, unit);
                    return null;
                }
            };
        }

        /**
         * Sends the message asynchronously.
         * <p>
         * IMPORTANT NOTE: There is no need to start new thread here. All writer are by default asynchronous, only
         * difference between sync and async writer are that sync send should wait for future.get().
         *
         * @param message message to be sent
         * @param handler message sending callback handler
         * @param type    message type
         */
        private void sendAsync(final Object message, final SendHandler handler, final AsyncMessageType type) {
            switch (type) {
                case TEXT:
                    webSocket.sendText((String) message, handler, new WriterInfo(MessageType.TEXT, RemoteEndpointType.ASYNC));
                    break;

                case BINARY:
                    webSocket.sendBinary(Utils.getRemainingArray((ByteBuffer) message), handler,
                            new WriterInfo(MessageType.BINARY, RemoteEndpointType.ASYNC));
                    break;

                case OBJECT:
                    sendSyncObject(message, handler, new WriterInfo(MessageType.OBJECT, RemoteEndpointType.ASYNC));
                    break;
            }
        }

        private static enum AsyncMessageType {
            TEXT, // String
            BINARY,  // ByteBuffer
            OBJECT // OBJECT
        }
    }

    @SuppressWarnings("unchecked")
    Future<?> sendSyncObject(Object o, WriterInfo writerInfo) {
        Object toSend;
        try {
            session.getDebugContext()
                   .appendLogMessage(LOGGER, Level.FINEST, DebugContext.Type.MESSAGE_OUT, "Sending object: ", o);
            toSend = endpointWrapper.doEncode(session, o);
        } catch (final Exception e) {
            return new Future<Object>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public Object get() throws InterruptedException, ExecutionException {
                    throw new ExecutionException(e);
                }

                @Override
                public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                        TimeoutException {
                    throw new ExecutionException(e);
                }
            };
        }

        if (toSend instanceof String) {
            return webSocket.sendText((String) toSend, writerInfo);
        } else if (toSend instanceof ByteBuffer) {
            return webSocket.sendBinary(Utils.getRemainingArray((ByteBuffer) toSend), writerInfo);
        } else if (toSend instanceof StringWriter) {
            StringWriter writer = (StringWriter) toSend;
            StringBuffer sb = writer.getBuffer();
            return webSocket.sendText(sb.toString(), writerInfo);
        } else if (toSend instanceof ByteArrayOutputStream) {
            ByteArrayOutputStream baos = (ByteArrayOutputStream) toSend;
            return webSocket.sendBinary(baos.toByteArray(), writerInfo);
        }

        return null;
    }

    // TODO: naming
    @SuppressWarnings("unchecked")
    void sendSyncObject(Object o, SendHandler handler, WriterInfo writerInfo) {
        if (o instanceof String) {
            webSocket.sendText((String) o, handler, writerInfo);
        } else {
            Object toSend = null;
            try {
                toSend = endpointWrapper.doEncode(session, o);
            } catch (final Exception e) {
                handler.onResult(new SendResult(e));
            }

            if (toSend instanceof String) {
                webSocket.sendText((String) toSend, handler, writerInfo);
            } else if (toSend instanceof ByteBuffer) {
                webSocket.sendBinary(Utils.getRemainingArray((ByteBuffer) toSend), handler, writerInfo);
            } else if (toSend instanceof StringWriter) {
                StringWriter writer = (StringWriter) toSend;
                StringBuffer sb = writer.getBuffer();
                webSocket.sendText(sb.toString(), handler, writerInfo);
            } else if (toSend instanceof ByteArrayOutputStream) {
                ByteArrayOutputStream baos = (ByteArrayOutputStream) toSend;
                webSocket.sendBinary(baos.toByteArray(), handler, writerInfo);
            }
        }
    }

    @Override
    public void sendPing(ByteBuffer applicationData) throws IOException {
        if (applicationData != null && applicationData.remaining() > 125) {
            throw new IllegalArgumentException(LocalizationMessages.APPLICATION_DATA_TOO_LONG("Ping"));
        }
        session.restartIdleTimeoutExecutor();
        webSocket.sendPing(Utils.getRemainingArray(applicationData));
    }

    @Override
    public void sendPong(ByteBuffer applicationData) throws IOException {
        if (applicationData != null && applicationData.remaining() > 125) {
            throw new IllegalArgumentException(LocalizationMessages.APPLICATION_DATA_TOO_LONG("Pong"));
        }
        session.restartIdleTimeoutExecutor();
        webSocket.sendPong(Utils.getRemainingArray(applicationData));
    }

    @Override
    public String toString() {
        return "Wrapped: " + getClass().getSimpleName();
    }

    @Override
    public void setBatchingAllowed(boolean allowed) {
        // TODO: Implement.
    }

    @Override
    public boolean getBatchingAllowed() {
        return false;  // TODO: Implement.
    }

    @Override
    public void flushBatch() {
        // TODO: Implement.
    }

    public void close(CloseReason cr) {
        LOGGER.fine("Close public void close(CloseReason cr): " + cr);
        webSocket.close(cr);
    }
}
