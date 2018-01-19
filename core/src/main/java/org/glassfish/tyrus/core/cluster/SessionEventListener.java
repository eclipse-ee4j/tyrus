/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.core.cluster;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.websocket.CloseReason;
import javax.websocket.Session;

/**
 * Session event listener.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class SessionEventListener {

    private final Session session;

    /**
     * Constructor.
     *
     * @param session session to be used for sending messages passed from other nodes.
     */
    public SessionEventListener(Session session) {
        this.session = session;
    }

    /**
     * Invoked on send text message event.
     *
     * @param message message to be sent.
     * @throws IOException if there is a problem delivering the message.
     */
    public void onSendText(String message) throws IOException {
        session.getBasicRemote().sendText(message);
    }

    /**
     * Invoked on send partial text message event.
     *
     * @param message partial message to be sent.
     * @param isLast  {@code true} when the partial message being sent is the last part of the message.
     * @throws IOException if there is a problem delivering the message.
     */
    public void onSendText(String message, boolean isLast) throws IOException {
        session.getBasicRemote().sendText(message, isLast);
    }

    /**
     * Invoked on send binary message event.
     *
     * @param message data to be sent.
     * @throws IOException if there is a problem delivering the message.
     */
    public void onSendBinary(byte[] message) throws IOException {
        session.getBasicRemote().sendBinary(ByteBuffer.wrap(message));
    }

    /**
     * Invoked on send partial binary message event.
     *
     * @param message data to be sent.
     * @param isLast  {@code true} when the partial message being sent is the last part of the message.
     * @throws IOException if there is a problem delivering the message.
     */
    public void onSendBinary(byte[] message, boolean isLast) throws IOException {
        session.getBasicRemote().sendBinary(ByteBuffer.wrap(message), isLast);
    }

    /**
     * Invoked on send ping frame event.
     *
     * @param payload ping frame payload.
     * @throws IOException if there is a problem delivering the message.
     */
    public void onSendPing(byte[] payload) throws IOException {
        session.getBasicRemote().sendPing(ByteBuffer.wrap(payload));
    }

    /**
     * Invoked on send pong frame event.
     *
     * @param payload pong frame payload.
     * @throws IOException if there is a problem delivering the message.
     */
    public void onSendPong(byte[] payload) throws IOException {
        session.getBasicRemote().sendPong(ByteBuffer.wrap(payload));
    }

    /**
     * Invoked on session close event.
     *
     * @throws IOException if there is a problem closing the session.
     */
    public void onClose() throws IOException {
        session.close();
    }

    /**
     * Invoked on session close event.
     *
     * @param closeReason close reason of the session close event.
     * @throws IOException if there is a problem closing the session.
     */
    public void onClose(CloseReason closeReason) throws IOException {
        session.close(closeReason);
    }
}
