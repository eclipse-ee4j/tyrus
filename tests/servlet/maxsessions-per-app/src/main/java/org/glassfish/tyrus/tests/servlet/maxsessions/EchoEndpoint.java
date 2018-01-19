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

package org.glassfish.tyrus.tests.servlet.maxsessions;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

/**
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
public class EchoEndpoint extends Endpoint {

    // onClose (on server-side) should be called only for successfully opened sessions
    public static final AtomicBoolean forbiddenClose = new AtomicBoolean(false);

    @Override
    public void onOpen(final Session session, EndpointConfig config) {
        MaxSessionPerAppApplicationConfig.openLatch.countDown();
        try {
            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    try {
                        session.getBasicRemote().sendText(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            session.getBasicRemote().sendText("Do or do not, there is no try.");
        } catch (IOException e) {
            // do nothing
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        MaxSessionPerAppApplicationConfig.closeLatch.countDown();
        if (closeReason.getCloseCode().getCode() == CloseReason.CloseCodes.TRY_AGAIN_LATER.getCode()) {
            forbiddenClose.set(true);
        }
    }
}
