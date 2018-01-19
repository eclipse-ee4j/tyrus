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
import java.util.Timer;
import java.util.TimerTask;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@ServerEndpoint(value = "/idletimeoutsending", configurator = SingletonConfigurator.class)
public class IdleTimeoutSendingEndpoint {

    private volatile boolean onCloseCalled = false;

    public static final long TIMEOUT = 500;
    private Timer timer = null;

    @OnOpen
    public void onOpen(final Session session) {
        session.setMaxIdleTimeout(TIMEOUT);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    session.getBasicRemote().sendText("Some text.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 300);
    }

    @OnMessage
    public void onMessage(String message) {
        // do nothing.
    }

    @OnClose
    public void onClose(Session session) {
        onCloseCalled = true;
        if (timer != null) {
            timer.cancel();
        }
    }

    boolean isOnCloseCalled() {
        return onCloseCalled;
    }

    void setOnCloseCalled(boolean onCloseCalled) {
        this.onCloseCalled = onCloseCalled;
    }
}
