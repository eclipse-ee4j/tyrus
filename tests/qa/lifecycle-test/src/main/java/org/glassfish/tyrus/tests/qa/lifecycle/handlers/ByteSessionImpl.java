/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.qa.lifecycle.handlers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;

import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import org.glassfish.tyrus.tests.qa.lifecycle.SessionConversation;
import org.glassfish.tyrus.tests.qa.lifecycle.SessionLifeCycle;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class ByteSessionImpl extends SessionLifeCycle<byte[]> implements SessionConversation {

    @Override
    public SessionLifeCycle getSessionConversation(boolean partial) {
        return new ByteSessionImpl(1024, true, partial);
    }

    int messageSize;
    byte[] messageToSend;
    ByteBuffer gotPartial, wholeMessage;

    public ByteSessionImpl(int messageSize, boolean directIO, boolean partial) {
        super(partial);
        this.messageSize = messageSize;
        messageToSend = new byte[messageSize];
        gotPartial = ByteBuffer.allocate(messageSize * 5);
        wholeMessage = ByteBuffer.allocate(messageSize * 5);
        initSendBuffer();

    }

    private void initSendBuffer() {
        for (int idx = 0; idx < messageSize; idx++) {
            messageToSend[idx] = (byte) idx;
        }

        wholeMessage.put(ByteBuffer.wrap(messageToSend));
        wholeMessage.put(ByteBuffer.wrap(messageToSend));
        wholeMessage.put(ByteBuffer.wrap(messageToSend));
        wholeMessage.put(ByteBuffer.wrap(messageToSend));
        wholeMessage.put(ByteBuffer.wrap(messageToSend));

    }

    boolean bb_equal(final byte[] b1, final byte[] b2) {
        if (b1.length != b2.length) {
            logger.log(Level.SEVERE, "arrays not equal! {0} {1}", new Object[]{b1.length, b2.length});
            return false;
        }
        for (int idx = 0; idx < b1.length; idx++) {
            if (b1[idx] != b2[idx]) {
                logger.log(Level.SEVERE, "Arrays mismatch at index: {0}", idx);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onClientMessageHandler(byte[] message, Session session) throws IOException {
        if (0 == ByteBuffer.wrap(message).compareTo(ByteBuffer.wrap(messageToSend))) {
            closeTheSessionFromClient(session);
        }
    }

    @Override
    public void onServerMessageHandler(byte[] message, Session session) throws IOException {
        session.getBasicRemote().sendBinary(ByteBuffer.wrap(message));
    }

    @Override
    public void startTalk(Session s) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(messageToSend);
        s.getBasicRemote().sendBinary(bb);
    }

    @Override
    public void onServerMessageHandler(byte[] message, Session session, boolean last) throws IOException {
        session.getBasicRemote().sendBinary(ByteBuffer.wrap(message), last);
    }

    @Override
    public void onClientMessageHandler(byte[] message, Session session, boolean last) throws IOException {
        gotPartial.put(ByteBuffer.wrap(message));
        if (last) {
            logger.log(Level.INFO, "got Last one:{0}", gotPartial);
            if (0 == gotPartial.compareTo(wholeMessage)) {
                closeTheSessionFromClient(session);
            }
        }

    }

    @Override
    public void startTalkPartial(Session s) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(messageToSend);
        Basic remote = s.getBasicRemote();
        remote.sendBinary(bb, false);
        remote.sendBinary(bb, false);
        remote.sendBinary(bb, false);
        remote.sendBinary(bb, false);
        remote.sendBinary(bb, true);
    }
}
