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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.websocket.Session;

import org.glassfish.tyrus.tests.qa.lifecycle.SessionConversation;
import org.glassfish.tyrus.tests.qa.lifecycle.SessionLifeCycle;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class ByteBufferSessionImpl extends SessionLifeCycle<ByteBuffer> implements SessionConversation {

    int messageSize;
    ByteBuffer messageToSend;
    ByteBuffer gotPartial;
    ByteBuffer wholeMessage;
    //String textMessageToSend;

    public ByteBufferSessionImpl(int messageSize, boolean directIO, boolean partial) {
        super(partial);
        this.messageSize = messageSize;
        if (directIO) {
            this.messageToSend = ByteBuffer.allocate(messageSize);
            gotPartial = ByteBuffer.allocate(messageSize * 4);
            wholeMessage = ByteBuffer.allocate(messageSize * 4);
        } else {
            this.messageToSend = ByteBuffer.allocateDirect(messageSize);
            gotPartial = ByteBuffer.allocateDirect(messageSize * 4);
            wholeMessage = ByteBuffer.allocateDirect(messageSize * 4);
        }

        initSendBuffer();
    }

    private void initSendBuffer() {
        for (int idx = 0; idx < messageSize; idx++) {
            messageToSend.put((byte) idx);
//            textMessageToSend+=(char)idx;
        }
        wholeMessage.put(messageToSend.array());
        wholeMessage.put(messageToSend.array());
        wholeMessage.put(messageToSend.array());
        wholeMessage.put(messageToSend.array());
    }

    private boolean bb_equals(ByteBuffer b1, ByteBuffer b2) {
        logger.log(Level.INFO, "compare:{0}", b1.compareTo(b2));
        //return 0==b1.compareTo(b2);
        //return b1.array().equals(b2.array());
        return bb_equal(b1.array(), b2.array());
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
    public void onClientMessageHandler(ByteBuffer message, Session session) throws IOException {
        if (bb_equals(message, messageToSend)) {
            closeTheSessionFromClient(session);
        }
    }

    @Override
    public void startTalk(final Session s) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(messageSize);
        s.getBasicRemote().sendBinary(messageToSend);

    }

    @Override
    public void onServerMessageHandler(ByteBuffer message, Session session) throws IOException {
        session.getBasicRemote().sendBinary(message);
    }

    @Override
    public void onServerMessageHandler(ByteBuffer message, Session session, boolean last) throws IOException {
        session.getBasicRemote().sendBinary(message, last);
    }

    @Override
    public SessionLifeCycle getSessionConversation(boolean partial) {
        return new ByteBufferSessionImpl(1024, true, partial);
    }

    @Override
    public void onClientMessageHandler(ByteBuffer message, Session session, boolean last) throws IOException {
        logger.log(Level.INFO, "message:{0}", message);
        logger.log(Level.INFO, "last:{0}", last);
        gotPartial.put(message);
        if (last) {
            if (bb_equals(gotPartial, wholeMessage)) {
                closeTheSessionFromClient(session);
            }
        }
    }

    @Override
    public void startTalkPartial(final Session s) throws IOException {
        List<Thread> partialMsgWorkers = new ArrayList<Thread>();
        final CountDownLatch done = new CountDownLatch(3);
        partialMsgWorkers.add(new Thread() {
            @Override
            public void run() {
                try {
                    //s.getBasicRemote().sendText(textMessageToSend, false);
                    s.getBasicRemote().sendBinary(messageToSend, false);
                    done.countDown();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        });
        partialMsgWorkers.add(new Thread() {
            @Override
            public void run() {
                try {
                    //s.getBasicRemote().sendText(textMessageToSend, false);
                    s.getBasicRemote().sendBinary(messageToSend, false);
                    done.countDown();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        });
        partialMsgWorkers.add(new Thread() {
            @Override
            public void run() {
                try {
                    //s.getBasicRemote().sendText(textMessageToSend, false);
                    s.getBasicRemote().sendBinary(messageToSend, false);
                    done.countDown();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        });

        for (Thread t : partialMsgWorkers) {
            t.start();
        }

        try {
            done.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        s.getBasicRemote().sendBinary(messageToSend, true);
    }
}
