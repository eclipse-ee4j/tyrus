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

package org.glassfish.tyrus.test.standard_config;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.MessageHandler;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests the ServerContainer.getOpenSessions method.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class SessionGetOpenSessionsTest extends TestContainer {

    @ServerEndpoint(value = "/customremote/hello1")
    public static class SessionTestEndpoint {

        @OnOpen
        public void onOpen(Session s) {
            System.out.println("s ### opened! " + s);
        }

        @OnMessage
        public String onMessage(String message, Session session) {
            if (message.equals("count")) {
                return String.valueOf(session.getOpenSessions().size());
            }

            return null;
        }

        @OnError
        public void onError(Throwable t) {
            t.printStackTrace();
        }
    }

    @Test
    public void testGetOpenSessions() throws DeploymentException {
        final CountDownLatch messageLatch = new CountDownLatch(1);

        Server server = startServer(SessionTestEndpoint.class);

        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

        try {
            Thread.sleep(1000);
            final ClientManager client = createClient();
            for (int i = 0; i < 2; i++) {
                client.connectToServer(new TestEndpointAdapter() {
                    @Override
                    public void onOpen(Session session) {
                        System.out.println("c ### opened! " + session);
                        try {
                            session.getBasicRemote().sendText("a");
                        } catch (IOException e) {
                            // nothing
                        }
                    }

                    @Override
                    public void onMessage(String s) {
                    }
                }, cec, getURI(SessionTestEndpoint.class));
            }

            for (int i = 0; i < 2; i++) {
                client.connectToServer(new TestEndpointAdapter() {
                    @Override
                    public void onOpen(Session session) {
                        System.out.println("c ### opened! " + session);
                        try {
                            session.getBasicRemote().sendText("a");
                        } catch (IOException e) {
                            // nothing
                        }
                    }

                    @Override
                    public void onMessage(String s) {
                    }
                }, cec, getURI(SessionTestEndpoint.class));
            }

            client.connectToServer(new TestEndpointAdapter() {
                @Override
                public void onOpen(Session session) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            assertEquals("5", message);
                            messageLatch.countDown();
                        }
                    });

                    System.out.println("c ### opened! " + session);
                    try {
                        session.getBasicRemote().sendText("count");
                    } catch (IOException e) {
                        // nothing
                    }
                }

                @Override
                public void onMessage(String s) {
                }
            }, cec, getURI(SessionTestEndpoint.class));

            messageLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
