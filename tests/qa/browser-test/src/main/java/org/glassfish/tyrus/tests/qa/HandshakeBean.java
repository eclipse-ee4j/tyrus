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

package org.glassfish.tyrus.tests.qa;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/chat")
public class HandshakeBean {

    private static ConnState state = ConnState.NONE;
    private static Logger logger = Logger.getLogger(HandshakeBean.class.getCanonicalName());

    public static ConnState getState() {
        return state;
    }

    public static void reset() {
        state = ConnState.NONE;
    }

    @OnOpen
    public void initSession(Session s) {
        logger.log(Level.INFO, "Someone connected:{0}", s.getRequestURI().toString());
        System.out.println("server.container:" + s.getContainer().toString());
        state = ConnState.SERVER_OPEN;
    }

    @OnMessage
    public String chatHandler(String message) {
        logger.log(Level.INFO, "start: message={0} state={1}\n", new Object[]{message, state});

        if (state == ConnState.BROWSER_OKAY) {
            return null;
        } else {
            state = ConnState.next(message, state);
            logger.log(Level.INFO, "next: message={0} state={1}\n", new Object[]{message, state});
            return state.getSendMsg();
        }
    }
}
