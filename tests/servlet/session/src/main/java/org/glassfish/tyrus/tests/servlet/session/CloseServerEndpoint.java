/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.servlet.session;

import java.io.IOException;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@ServerEndpoint(value = "/closeserver", configurator = SingletonConfigurator.class)
public class CloseServerEndpoint {
    private volatile boolean addMessageHandlerExceptionThrown = false;
    private volatile boolean removeMessageHandlerExceptionThrown = false;
    private volatile boolean getAsyncRemoteExceptionThrown = false;
    private volatile boolean getBasicRemoteExceptionThrown = false;
    private volatile boolean inCloseSendTextExceptionThrown = false;
    private volatile boolean inCloseGetTimeoutExceptionThrown = false;

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            session.addMessageHandler(null);
        } catch (IllegalStateException e) {
            addMessageHandlerExceptionThrown = true;
        }

        try {
            session.removeMessageHandler(null);
        } catch (IllegalStateException e) {
            removeMessageHandlerExceptionThrown = true;
        }

        try {
            session.getBasicRemote();
        } catch (IllegalStateException e) {
            getBasicRemoteExceptionThrown = true;
        }

        try {
            session.getAsyncRemote();
        } catch (IllegalStateException e) {
            getAsyncRemoteExceptionThrown = true;
        }
    }

    @OnClose
    public void onClose(Session session) {

        try {
            session.getMaxIdleTimeout();
        } catch (Exception e) {
            inCloseGetTimeoutExceptionThrown = true;
        }

        try {
            session.getBasicRemote().sendText("Hello.");
        } catch (Exception e) {
            inCloseSendTextExceptionThrown = true;
        }
    }

    @OnError
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    boolean isAddMessageHandlerExceptionThrown() {
        return addMessageHandlerExceptionThrown;
    }

    boolean isRemoveMessageHandlerExceptionThrown() {
        return removeMessageHandlerExceptionThrown;
    }

    boolean isGetAsyncRemoteExceptionThrown() {
        return getAsyncRemoteExceptionThrown;
    }

    boolean isGetBasicRemoteExceptionThrown() {
        return getBasicRemoteExceptionThrown;
    }

    boolean isInCloseSendTextExceptionThrown() {
        return inCloseSendTextExceptionThrown;
    }

    boolean isInCloseGetTimeoutExceptionThrown() {
        return inCloseGetTimeoutExceptionThrown;
    }
}
