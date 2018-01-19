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

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ServerSessionConnectToServerTest extends TestContainer {

    private CountDownLatch messageLatch;

    @Test
    public void testConnectToServerWithinServerEndpoint() throws DeploymentException, IOException,
            InterruptedException {
        final Server server = startServer(ConnectToServerEndpoint.class, ConnectToServerEchoEndpoint.class);
        try {
            messageLatch = new CountDownLatch(1);

            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {


                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    try {
                        final String anotherEndpointURI = getURI(ConnectToServerEchoEndpoint.class).toString();

                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                System.out.println("### Client received: " + message);
                                assertEquals(anotherEndpointURI, message);

                                messageLatch.countDown();
                            }
                        });
                        session.getBasicRemote().sendText(anotherEndpointURI);
                        System.out.println("### Message from client sent.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(ConnectToServerEndpoint.class));

            assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint(value = "/connectToServerEndpoint")
    public static class ConnectToServerEndpoint {

        CountDownLatch messageLatch = new CountDownLatch(1);
        String receivedMessage;

        @OnMessage
        public String onMessage(String message, Session session) throws IOException, DeploymentException,
                InterruptedException {

            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
            final WebSocketContainer serverWebSocketContainer = session.getContainer();

            serverWebSocketContainer.connectToServer(new Endpoint() {

                @Override
                public void onOpen(final Session session, EndpointConfig config) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                System.out.println("### Server endpoint received: " + message);

                                if (message
                                        .equals("Yo Dawg, I heard you like clients, so we put client into server so "
                                                        + "you can connectToServer while you connectToServer.")
                                        && (serverWebSocketContainer.equals(session.getContainer()))) {
                                    messageLatch.countDown();
                                }
                            }
                        });
                        session.getBasicRemote().sendText(
                                "Yo Dawg, I heard you like clients, so we put client into server so you can "
                                        + "connectToServer while you connectToServer.");
                        System.out.println("### Message from client running inside server endpoint sent.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }, cec, URI.create(message));

            if (messageLatch.await(3, TimeUnit.SECONDS)) {
                return message;
            } else {
                return null;
            }
        }
    }

    @ServerEndpoint(value = "/connectToServerEchoEndpoint")
    public static class ConnectToServerEchoEndpoint {

        @OnMessage
        public String onMessage(String message) {
            return message;
        }
    }
}
