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
import java.util.logging.Level;

import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import org.glassfish.tyrus.tests.qa.lifecycle.SessionLifeCycle;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class StringSessionImpl extends SessionLifeCycle<String> {

    String gotPartial = "";

    public StringSessionImpl(boolean partial) {
        super(partial);
    }

    @Override
    public void onServerMessageHandler(String message, Session session) throws IOException {
        logger.log(Level.INFO, "StringSessionImpl: onServerMessage: {0}", message);
        session.getBasicRemote().sendText(message);
    }

    @Override
    public void onClientMessageHandler(String message, Session session) throws IOException {
        logger.log(Level.INFO, "StringSessionImpl: onClientMessage: {0}", message);
        if (message.equals("client.open")) {
            closeTheSessionFromClient(session);
        }
    }

    @Override
    public void startTalk(Session s) throws IOException {
        logger.log(Level.INFO, "startTalk with client.open");
        s.getBasicRemote().sendText("client.open");
    }

    @Override
    public void onServerMessageHandler(String message, Session session, boolean last) throws IOException {
        session.getBasicRemote().sendText(message, last);
    }

    @Override
    public void onClientMessageHandler(String message, Session session, boolean last) throws IOException {
        gotPartial += message;
        if (last) {
            logger.log(Level.INFO, "Last one: {0}", gotPartial);
            if (gotPartial.equals("client.openclient.openclient.openclient.openclient.open")) {
                closeTheSessionFromClient(session);
            }
        }
    }

    @Override
    public void startTalkPartial(Session s) throws IOException {
        gotPartial = "";
        Basic remote = s.getBasicRemote();
        remote.sendText("client.open", false);
        remote.sendText("client.open", false);
        remote.sendText("client.open", false);
        remote.sendText("client.open", false);
        remote.sendText("client.open", true);
    }
}
