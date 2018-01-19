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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Level;

import javax.websocket.Session;

import org.glassfish.tyrus.tests.qa.lifecycle.SessionConversation;
import org.glassfish.tyrus.tests.qa.lifecycle.SessionLifeCycle;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class BufferedReaderSessionImpl extends SessionLifeCycle<Reader> implements SessionConversation {

    private int messageSize;
    private String messageToSend = "";
    private String gotMessage = "";
    private static final String oneLine = "abcdefghijklm\n";

    private String readMessage(Reader rd) throws IOException {
        String wholeMessage = "";
        BufferedReader br = new BufferedReader(rd);
        for (; ; ) {
            String line = br.readLine();
            if (line == null) {
                break;
            }
            wholeMessage += line + "\n";
        }
        return wholeMessage;
    }

    public BufferedReaderSessionImpl(int messageSize) {
        super(false);
        this.messageSize = messageSize;
        initSendMessage();
    }

    private void initSendMessage() {
        for (int idx = 0; idx < messageSize / oneLine.length(); idx++) {
            messageToSend += oneLine;
        }
        logger.log(Level.INFO, "XXX:initSendMessage:{0}", messageToSend);
    }

    @Override
    public void startTalk(Session s) throws IOException {
        logger.log(Level.INFO, "XXX: Send message:{0}", messageToSend);
        javax.websocket.RemoteEndpoint.Basic basic = s.getBasicRemote();
        Writer wr = basic.getSendWriter();
        wr.write(messageToSend);
        wr.close();
    }

    @Override
    public void onServerMessageHandler(Reader reader, Session session) throws IOException {
        logger.log(Level.INFO, "XXX: HERE I AM!!!");
        String message = readMessage(reader);
        logger.log(Level.INFO, "XXX: bounce message:{0}", message);
        Writer wr = session.getBasicRemote().getSendWriter();
        wr.write(message);
        wr.close();
    }

    @Override
    public void onClientMessageHandler(Reader reader, Session session) throws IOException {
        String message = readMessage(reader);
        if (message.equals(messageToSend)) {
            closeTheSessionFromClient(session);
        }
    }

    @Override
    public SessionLifeCycle getSessionConversation(boolean partial) {
        return new BufferedReaderSessionImpl(1024);
    }

    @Override
    public void onServerMessageHandler(Reader message, Session session, boolean last) throws IOException {

    }

    @Override
    public void onClientMessageHandler(Reader message, Session session, boolean last) throws IOException {

    }

    @Override
    public void startTalkPartial(Session s) throws IOException {

    }
}
