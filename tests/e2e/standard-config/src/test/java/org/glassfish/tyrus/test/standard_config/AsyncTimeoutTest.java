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

package org.glassfish.tyrus.test.standard_config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class AsyncTimeoutTest extends TestContainer {

    @ServerEndpoint(value = "/asyncTimeoutTest")
    public static class AsyncTimeoutEndpoint {

        @OnMessage
        public String onMessage(String message, Session session) {
            int i = 0;

            try {
                i = Integer.parseInt(message);
            } catch (NumberFormatException e) {
                // do nothing.
            }

            if (i > 0) {
                session.getContainer().setAsyncSendTimeout(i);
            }

            return Long.toString(session.getAsyncRemote().getSendTimeout());
        }
    }

    @Test
    public void testAsyncTimeout() throws DeploymentException {
        Server server = startServer(AsyncTimeoutEndpoint.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            final CountDownLatch latch1 = new CountDownLatch(1);
            final CountDownLatch latch2 = new CountDownLatch(1);
            final ClientManager client = createClient();

            Session session = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(
                            new MessageHandler.Whole<String>() {
                                @Override
                                public void onMessage(String message) {
                                    // we don't really care for this one.
                                    latch1.countDown();
                                }
                            }
                    );
                }
            }, cec, getURI(AsyncTimeoutEndpoint.class));

            session.getBasicRemote().sendText("2000");

            assertTrue(latch1.await(2, TimeUnit.SECONDS));

            session = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(
                            new MessageHandler.Whole<String>() {
                                @Override
                                public void onMessage(String message) {
                                    if (message.equals("2000")) {
                                        latch2.countDown();
                                    }
                                }
                            }
                    );
                }
            }, cec, getURI(AsyncTimeoutEndpoint.class));

            session.getBasicRemote().sendText("GET");

            assertTrue(latch2.await(2, TimeUnit.SECONDS));

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
