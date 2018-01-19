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

package org.glassfish.tyrus.tests.servlet.remote;

import java.nio.ByteBuffer;

import javax.websocket.OnMessage;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@ServerEndpoint(value = "/byhandler", configurator = SingletonConfigurator.class)
public class TimeoutEndpointResultByHandler {

    private byte[] longMessage = new byte[1000000];

    private volatile boolean timeoutRaised = false;

    @OnMessage
    public void onMessage(String s, Session session) {
        SendHandler sendHandler = new SendHandler() {
            @Override
            public void onResult(SendResult sendResult) {
                System.out.println("Result: " + sendResult.isOK());
                if (!sendResult.isOK()) {
                    timeoutRaised = true;
                }
            }
        };

        for (int i : longMessage) {
            longMessage[i] = 0;
        }

        session.getAsyncRemote().setSendTimeout(1);
        System.out.println("Message sent: ");
        session.getAsyncRemote().sendBinary(ByteBuffer.wrap(longMessage), sendHandler);
    }

    boolean isTimeoutRaised() {
        return timeoutRaised;
    }
}
