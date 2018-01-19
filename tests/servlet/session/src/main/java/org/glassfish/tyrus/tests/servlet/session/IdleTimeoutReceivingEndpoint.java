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

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@ServerEndpoint(value = "/idletimeoutreceiving", configurator = SingletonConfigurator.class)
public class IdleTimeoutReceivingEndpoint {
    private volatile boolean onCloseCalled = false;

    public static final long TIMEOUT = 500;

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxIdleTimeout(TIMEOUT);
    }

    @OnMessage
    public void onMessage(String message) {
        // do nothing.
    }

    @OnClose
    public void onClose(Session session) {
        onCloseCalled = true;
    }

    boolean isOnCloseCalled() {
        return onCloseCalled;
    }

    void setOnCloseCalled(boolean onCloseCalled) {
        this.onCloseCalled = onCloseCalled;
    }
}
