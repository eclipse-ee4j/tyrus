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
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * https://java.net/jira/browse/TYRUS-203
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class Tyrus203Test extends TestContainer {

    @ServerEndpoint("/echo/{color}")
    public static class EchoServerEndpoint {
        @OnMessage
        public String echo(String message, @PathParam("color") String color) {
            return color + ":" + message;
        }
    }

    @Test
    public void test() throws DeploymentException {
        Server server = startServer(EchoServerEndpoint.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            final CountDownLatch client1messageLatch = new CountDownLatch(1);
            final CountDownLatch client2messageLatch = new CountDownLatch(1);
            ClientManager client = createClient();
            Session session1 = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(final Session session, EndpointConfig configuration) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            System.out.println(session.getId() + " Client1 @OnMessage -> " + message);
                            if (message.equals("first:test")) {
                                client1messageLatch.countDown();
                            }
                        }
                    });
                    // do nothing
                }
            }, cec, getURI("/echo/first"));

            Session session2 = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(final Session session, EndpointConfig configuration) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            System.out.println(session.getId() + " Client2 @OnMessage -> " + message);
                            if (message.equals("second:test")) {
                                client2messageLatch.countDown();
                            }

                        }
                    });
                    // do nothing
                }
            }, cec, getURI("/echo/second"));

            session1.getBasicRemote().sendText("test");
            session2.getBasicRemote().sendText("test");

            client1messageLatch.await(3, TimeUnit.SECONDS);
            client2messageLatch.await(3, TimeUnit.SECONDS);

            assertEquals(0, client1messageLatch.getCount());
            assertEquals(0, client2messageLatch.getCount());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
