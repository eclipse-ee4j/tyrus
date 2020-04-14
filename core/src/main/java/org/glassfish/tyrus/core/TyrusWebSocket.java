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

package org.glassfish.tyrus.core;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.websocket.CloseReason;
import javax.websocket.Extension;
import javax.websocket.SendHandler;

import org.glassfish.tyrus.core.frame.BinaryFrame;
import org.glassfish.tyrus.core.frame.CloseFrame;
import org.glassfish.tyrus.core.frame.Frame;
import org.glassfish.tyrus.core.frame.PingFrame;
import org.glassfish.tyrus.core.frame.PongFrame;
import org.glassfish.tyrus.core.frame.TextFrame;
import org.glassfish.tyrus.core.frame.TyrusFrame;
import org.glassfish.tyrus.core.l10n.LocalizationMessages;
import org.glassfish.tyrus.core.monitoring.MessageEventListener;
import org.glassfish.tyrus.spi.UpgradeRequest;
import org.glassfish.tyrus.spi.WriterInfo;

/**
 * Tyrus representation of web socket connection.
 * <p>
 * Instance of this class represents one bi-directional websocket connection.
 */
public class TyrusWebSocket {

    private final TyrusEndpointWrapper endpointWrapper;
    private final ProtocolHandler protocolHandler;
    private final CountDownLatch onConnectLatch = new CountDownLatch(1);
    private final EnumSet<State> connected = EnumSet.range(State.CONNECTED, State.CLOSING);
    //TODO refactor to make this class immutable.
    private final AtomicReference<State> state = new AtomicReference<State>(State.NEW);
    private final Lock lock = new ReentrantLock();

    private volatile MessageEventListener messageEventListener = MessageEventListener.NO_OP;

    private static final WriterInfo PING_INFO = new WriterInfo(WriterInfo.MessageType.PING, WriterInfo.RemoteEndpointType.SUPER);
    private static final WriterInfo PONG_INFO = new WriterInfo(WriterInfo.MessageType.PONG, WriterInfo.RemoteEndpointType.SUPER);

    /**
     * Create new instance, set {@link ProtocolHandler} and register {@link TyrusEndpointWrapper}.
     *
     * @param protocolHandler used for writing data (sending).
     * @param endpointWrapper notifies registered endpoints about incoming events.
     */
    public TyrusWebSocket(final ProtocolHandler protocolHandler,
                          final TyrusEndpointWrapper endpointWrapper) {
        this.protocolHandler = protocolHandler;
        this.endpointWrapper = endpointWrapper;
        protocolHandler.setWebSocket(this);
    }

    /**
     * Sets the timeout for the writing operation.
     *
     * @param timeoutMs timeout in milliseconds.
     */
    public void setWriteTimeout(long timeoutMs) {
        // do nothing.
    }

    /**
     * Convenience method to determine if this {@link TyrusWebSocket} instance is connected.
     *
     * @return {@code true} if the {@link TyrusWebSocket} is connected, {@code false} otherwise.
     */
    public boolean isConnected() {
        return connected.contains(state.get());
    }

    /**
     * This callback will be invoked when the remote endpoint sent a closing frame.
     * <p>
     * The execution of this method is synchronized using {@link ProtocolHandler} instance; see TYRUS-385. Prevents
     * multiple invocations, especially from container/user code.
     *
     * @param frame the close frame from the remote endpoint.
     */
    public void onClose(CloseFrame frame) {
        boolean locked = lock.tryLock();
        if (locked) {
            try {
                final CloseReason closeReason = frame.getCloseReason();

                if (endpointWrapper != null) {
                    endpointWrapper.onClose(this, closeReason);
                }
                if (state.compareAndSet(State.CONNECTED, State.CLOSING)) {
                    protocolHandler.close(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
                } else {
                    state.set(State.CLOSED);
                    protocolHandler.doClose();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * This callback will be invoked when the opening handshake between both
     * endpoints has been completed.
     *
     * @param upgradeRequest request associated with this socket.
     * @param subProtocol    negotiated subprotocol.
     * @param extensions     negotiated extensions.
     * @param connectionId   connection id.
     * @param debugContext   debug context.
     */
    public void onConnect(UpgradeRequest upgradeRequest, String subProtocol, List<Extension> extensions,
                          String connectionId, DebugContext debugContext) {
        state.set(State.CONNECTED);

        if (endpointWrapper != null) {
            endpointWrapper.onConnect(this, upgradeRequest, subProtocol, extensions, connectionId, debugContext);
        }

        onConnectLatch.countDown();
    }

    /**
     * This callback will be invoked when a fragmented binary message has
     * been received.
     *
     * @param frame the binary data received from the remote endpoint.
     * @param last  flag indicating whether or not the payload received is the final fragment of a message.
     */
    public void onFragment(BinaryFrame frame, boolean last) {
        awaitOnConnect();
        if (endpointWrapper != null) {
            endpointWrapper.onPartialMessage(this, ByteBuffer.wrap(frame.getPayloadData()), last);
            messageEventListener.onFrameReceived(frame.getFrameType(), frame.getPayloadLength());
        }
    }

    /**
     * This callback will be invoked when a fragmented textual message has
     * been received.
     *
     * @param frame the text received from the remote endpoint.
     * @param last  flag indicating whether or not the payload received is the final fragment of a message.
     */
    public void onFragment(TextFrame frame, boolean last) {
        awaitOnConnect();
        if (endpointWrapper != null) {
            endpointWrapper.onPartialMessage(this, frame.getTextPayload(), last);
            messageEventListener.onFrameReceived(frame.getFrameType(), frame.getPayloadLength());
        }
    }

    /**
     * This callback will be invoked when a binary message has been received.
     *
     * @param frame the binary data received from the remote endpoint.
     */
    public void onMessage(BinaryFrame frame) {
        awaitOnConnect();
        if (endpointWrapper != null) {
            endpointWrapper.onMessage(this, ByteBuffer.wrap(frame.getPayloadData()));
            messageEventListener.onFrameReceived(frame.getFrameType(), frame.getPayloadLength());
        }
    }

    /**
     * This callback will be invoked when a text message has been received.
     *
     * @param frame the text received from the remote endpoint.
     */
    public void onMessage(TextFrame frame) {
        awaitOnConnect();
        if (endpointWrapper != null) {
            endpointWrapper.onMessage(this, frame.getTextPayload());
            messageEventListener.onFrameReceived(frame.getFrameType(), frame.getPayloadLength());
        }
    }

    /**
     * This callback will be invoked when the remote endpoint has sent a ping frame.
     *
     * @param frame the ping frame from the remote endpoint.
     */
    public void onPing(PingFrame frame) {
        awaitOnConnect();
        if (endpointWrapper != null) {
            endpointWrapper.onPing(this, ByteBuffer.wrap(frame.getPayloadData()));
            messageEventListener.onFrameReceived(frame.getFrameType(), frame.getPayloadLength());
        }
    }

    /**
     * This callback will be invoked when the remote endpoint has sent a pong
     * frame.
     *
     * @param frame the pong frame from the remote endpoint.
     */
    public void onPong(PongFrame frame) {
        awaitOnConnect();
        if (endpointWrapper != null) {
            endpointWrapper.onPong(this, ByteBuffer.wrap(frame.getPayloadData()));
            messageEventListener.onFrameReceived(frame.getFrameType(), frame.getPayloadLength());
        }
    }

    /**
     * Closes this {@link TyrusWebSocket}.
     */
    public void close() {
        close(CloseReason.CloseCodes.NORMAL_CLOSURE.getCode(), null);
    }

    /**
     * Closes this {@link TyrusWebSocket} using the specified status code and
     * reason.
     *
     * @param code   the closing status code.
     * @param reason the reason, if any.
     */
    public void close(int code, String reason) {
        if (state.compareAndSet(State.CONNECTED, State.CLOSING)) {
            protocolHandler.close(code, reason);
        }
    }

    /**
     * Closes this {@link TyrusWebSocket} using the {@link javax.websocket.CloseReason}.
     *
     * @param closeReason the close reason.
     */
    public void close(CloseReason closeReason) {
        close(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
    }

    /**
     * Send a binary frame to the remote endpoint.
     *
     * @param data data to be sent.
     * @return {@link Future} which could be used to control/check the sending completion state.
     */
    @Deprecated
    public Future<Frame> sendBinary(byte[] data) {
        checkConnectedState();
        return protocolHandler.send(data);
    }

    /**
     * Send a binary frame to the remote endpoint.
     *
     * @param data data to be sent.
     * @param writerInfo  information about the outbound message.
     * @return {@link Future} which could be used to control/check the sending completion state.
     */
    public Future<Frame> sendBinary(byte[] data, WriterInfo writerInfo) {
        checkConnectedState();
        return protocolHandler.send(data, writerInfo);
    }

    /**
     * Send a binary frame to the remote endpoint.
     *
     * @param data    data to be sent.
     * @param handler {@link SendHandler#onResult(javax.websocket.SendResult)} will be called when sending is complete.
     */
    @Deprecated
    public void sendBinary(byte[] data, SendHandler handler) {
        checkConnectedState();
        protocolHandler.send(data, handler);
    }

    /**
     * Send a binary frame to the remote endpoint.
     *
     * @param data    data to be sent.
     * @param writerInfo  information about the outbound message.
     * @param handler {@link SendHandler#onResult(javax.websocket.SendResult)} will be called when sending is complete.
     */
    public void sendBinary(byte[] data, SendHandler handler, WriterInfo writerInfo) {
        checkConnectedState();
        protocolHandler.send(data, handler, writerInfo);
    }

    /**
     * Send a text frame to the remote endpoint.
     *
     * @param data data to be sent.
     * @return {@link Future} which could be used to control/check the sending completion state.
     */
    @Deprecated
    public Future<Frame> sendText(String data) {
        checkConnectedState();
        return protocolHandler.send(data);
    }

    /**
     * Send a text frame to the remote endpoint.
     *
     * @param data data to be sent.
     * @param writerInfo  information about the outbound message.
     * @return {@link Future} which could be used to control/check the sending completion state.
     */
    public Future<Frame> sendText(String data, WriterInfo writerInfo) {
        checkConnectedState();
        return protocolHandler.send(data, writerInfo);
    }

    /**
     * Send a text frame to the remote endpoint.
     *
     * @param data    data to be sent.
     * @param handler {@link SendHandler#onResult(javax.websocket.SendResult)} will be called when sending is complete.
     */
    @Deprecated
    public void sendText(String data, SendHandler handler) {
        checkConnectedState();
        protocolHandler.send(data, handler);
    }

    /**
     * Send a text frame to the remote endpoint.
     *
     * @param data    data to be sent.
     * @param writerInfo  information about the outbound message.
     * @param handler {@link SendHandler#onResult(javax.websocket.SendResult)} will be called when sending is complete.
     */
    public void sendText(String data, SendHandler handler, WriterInfo writerInfo) {
        checkConnectedState();
        protocolHandler.send(data, handler, writerInfo);
    }

    /**
     * Send a frame to the remote endpoint.
     *
     * @param data complete data frame.
     * @return {@link Future} which could be used to control/check the sending completion state.
     */
    public Future<Frame> sendRawFrame(ByteBuffer data) {
        checkConnectedState();
        return protocolHandler.sendRawFrame(data);
    }

    /**
     * Sends a <code>ping</code> frame with the specified payload (if any).
     *
     * @param data optional payload.  Note that payload length is restricted to 125 bytes or less.
     * @return {@link Future} which could be used to control/check the sending completion state.
     */
    public Future<Frame> sendPing(byte[] data) {
        return send(new PingFrame(data), PING_INFO);
    }

    /**
     * Sends a <code>ping</code> frame with the specified payload (if any).
     * <p>
     * It may seem odd to send a pong frame, however, RFC-6455 states:
     * "A Pong frame MAY be sent unsolicited.  This serves as a
     * unidirectional heartbeat.  A response to an unsolicited Pong frame is
     * not expected."
     *
     * @param data optional payload.  Note that payload length is restricted
     *             to 125 bytes or less.
     * @return {@link Future} which could be used to control/check the sending completion state.
     */
    public Future<Frame> sendPong(byte[] data) {
        return send(new PongFrame(data), PONG_INFO);
    }

    // return boolean, check return value
    private void awaitOnConnect() {
        try {
            onConnectLatch.await();
        } catch (InterruptedException e) {
            // do nothing.
        }
    }

    private Future<Frame> send(TyrusFrame frame, WriterInfo writerInfo) {
        checkConnectedState();
        return protocolHandler.send(frame, writerInfo);
    }

    /**
     * Sends a fragment of a complete message.
     *
     * @param fragment the textual fragment to send.
     * @param last     boolean indicating if this message fragment is the last.
     * @return {@link Future} which could be used to control/check the sending completion state.
     */
    @Deprecated
    public Future<Frame> sendText(String fragment, boolean last) {
        checkConnectedState();
        return protocolHandler.stream(last, fragment);
    }

    /**
     * Sends a fragment of a complete message.
     *
     * @param fragment   the textual fragment to send.
     * @param last       boolean indicating if this message fragment is the last.
     * @param writerInfo information about the outbound message.
     * @return {@link Future} which could be used to control/check the sending completion state.
     */
    public Future<Frame> sendText(String fragment, boolean last, WriterInfo writerInfo) {
        checkConnectedState();
        return protocolHandler.stream(last, fragment, writerInfo);
    }

    /**
     * Sends a fragment of a complete message.
     *
     * @param bytes the binary fragment to send.
     * @param last  boolean indicating if this message fragment is the last.
     * @return {@link Future} which could be used to control/check the sending completion state.
     */
    @Deprecated
    public Future<Frame> sendBinary(byte[] bytes, boolean last) {
        return sendBinary(bytes, 0, bytes.length, last);
    }

    /**
     * Sends a fragment of a complete message.
     *
     * @param bytes      the binary fragment to send.
     * @param last       boolean indicating if this message fragment is the last.
     * @param writerInfo information about the outbound message.
     * @return {@link Future} which could be used to control/check the sending completion state.
     */
    public Future<Frame> sendBinary(byte[] bytes, boolean last, WriterInfo writerInfo) {
        return sendBinary(bytes, 0, bytes.length, last, writerInfo);
    }

    /**
     * Sends a fragment of a complete message.
     *
     * @param bytes the binary fragment to send.
     * @param off   the offset within the fragment to send.
     * @param len   the number of bytes of the fragment to send.
     * @param last  boolean indicating if this message fragment is the last.
     * @return {@link Future} which could be used to control/check the sending completion state.
     */
    @Deprecated
    public Future<Frame> sendBinary(byte[] bytes, int off, int len, boolean last) {
        checkConnectedState();
        return protocolHandler.stream(last, bytes, off, len);
    }

    /**
     * Sends a fragment of a complete message.
     *
     * @param bytes       the binary fragment to send.
     * @param off         the offset within the fragment to send.
     * @param len         the number of bytes of the fragment to send.
     * @param last        boolean indicating if this message fragment is the last.
     * @param writerInfo  information about the outbound message.
     * @return {@link Future} which could be used to control/check the sending completion state.
     */
    public Future<Frame> sendBinary(byte[] bytes, int off, int len, boolean last, WriterInfo writerInfo) {
        checkConnectedState();
        return protocolHandler.stream(last, bytes, off, len, writerInfo);
    }

    ProtocolHandler getProtocolHandler() {
        return protocolHandler;
    }

    /**
     * Set message event listener.
     *
     * @param messageEventListener message event listener.
     */
    void setMessageEventListener(MessageEventListener messageEventListener) {
        this.messageEventListener = messageEventListener;
        protocolHandler.setMessageEventListener(messageEventListener);
    }

    /**
     * Get message event listener.
     *
     * @return message event listener.
     */
    MessageEventListener getMessageEventListener() {
        return messageEventListener;
    }

    private void checkConnectedState() {
        if (!isConnected()) {
            throw new RuntimeException(LocalizationMessages.SOCKET_NOT_CONNECTED());
        }
    }

    private enum State {
        NEW, CONNECTED, CLOSING, CLOSED
    }
}
