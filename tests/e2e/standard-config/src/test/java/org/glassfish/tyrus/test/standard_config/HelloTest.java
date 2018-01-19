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
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.standard_config.bean.EchoEndpoint;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the basic echo.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class HelloTest extends TestContainer {

    private CountDownLatch messageLatch;

    private volatile String receivedMessage;

    private static final String SENT_MESSAGE = "Hello World";

    @Test
    public void testHello() throws DeploymentException {
        final Server server = startServer(EchoEndpoint.class);
        try {
            messageLatch = new CountDownLatch(1);

            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();
            client.connectToServer(new TestEndpointAdapter() {

                private Session session;

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }

                @Override
                public void onOpen(Session session) {

                    this.session = session;

                    try {
                        session.addMessageHandler(new TestTextMessageHandler(this));
                        session.getBasicRemote().sendText(SENT_MESSAGE);
                        System.out.println("Hello message sent.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(String message) {
                    receivedMessage = message;

                    // TYRUS-141
                    if (session.getNegotiatedSubprotocol() != null) {
                        messageLatch.countDown();
                    }
                }
            }, cec, getURI(EchoEndpoint.class));
            messageLatch.await(1, TimeUnit.SECONDS);
            Assert.assertEquals(0L, messageLatch.getCount());
            Assert.assertEquals(SENT_MESSAGE, receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testHelloAsyncClient() throws DeploymentException {
        final Server server = startServer(EchoEndpoint.class);
        try {
            messageLatch = new CountDownLatch(1);

            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();
            client.asyncConnectToServer(new TestEndpointAdapter() {

                private Session session;

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }

                @Override
                public void onOpen(Session session) {

                    this.session = session;

                    try {
                        session.addMessageHandler(new TestTextMessageHandler(this));
                        session.getBasicRemote().sendText(SENT_MESSAGE);
                        System.out.println("Hello message sent.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(String message) {
                    receivedMessage = message;

                    // TYRUS-141
                    if (session.getNegotiatedSubprotocol() != null) {
                        messageLatch.countDown();
                    }
                }
            }, cec, getURI(EchoEndpoint.class));

            messageLatch.await(3, TimeUnit.SECONDS);
            Assert.assertEquals(0L, messageLatch.getCount());
            Assert.assertEquals(SENT_MESSAGE, receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }


    public static CountDownLatch messageLatchEndpoint;
    public static volatile String receivedMessageEndpoint;

    // TYRUS-63: connectToServer with Endpoint class
    // http://java.net/jira/browse/TYRUS-63
    @Test
    public void testHelloEndpointClass() throws DeploymentException {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        Server server = startServer(EchoEndpoint.class);

        try {
            messageLatchEndpoint = new CountDownLatch(1);

            WebSocketContainer client = ContainerProvider.getWebSocketContainer();
            client.connectToServer(MyEndpoint.class, cec, getURI(EchoEndpoint.class));
            messageLatchEndpoint.await(5, TimeUnit.SECONDS);
            Assert.assertEquals(0L, messageLatchEndpoint.getCount());
            Assert.assertEquals(SENT_MESSAGE, receivedMessageEndpoint);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testHelloEndpointClassAsyncClient() throws DeploymentException {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        Server server = startServer(EchoEndpoint.class);

        try {
            messageLatchEndpoint = new CountDownLatch(1);

            createClient().asyncConnectToServer(MyEndpoint.class, cec, getURI(EchoEndpoint.class));
            messageLatchEndpoint.await(5, TimeUnit.SECONDS);
            Assert.assertEquals(0L, messageLatchEndpoint.getCount());
            Assert.assertEquals(SENT_MESSAGE, receivedMessageEndpoint);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    public static final class MyEndpoint extends Endpoint implements MessageHandler.Whole<String> {
        @Override
        public void onOpen(Session session, EndpointConfig config) {
            try {
                session.addMessageHandler(this);
                session.getBasicRemote().sendText(SENT_MESSAGE);
                System.out.println("Hello message sent.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onMessage(String message) {
            receivedMessageEndpoint = message;
            messageLatchEndpoint.countDown();
        }
    }
}
