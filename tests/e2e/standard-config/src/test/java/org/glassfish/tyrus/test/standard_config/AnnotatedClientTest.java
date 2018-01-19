/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.standard_config.bean.TestEndpoint;
import org.glassfish.tyrus.test.standard_config.decoder.TestDecoder;
import org.glassfish.tyrus.test.standard_config.message.TestMessage;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;


/**
 * Tests the client with the annotated version of the
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class AnnotatedClientTest extends TestContainer {

    private static volatile String receivedMessage;
    private static volatile String receivedTestMessage;
    private static volatile CountDownLatch messageLatch;

    @Test
    public void testAnnotatedInstance() throws DeploymentException {
        Server server = startServer(TestEndpoint.class);
        final ClientEndpointConfig configuration = ClientEndpointConfig.Builder.create().build();

        messageLatch = new CountDownLatch(1);

        try {
            ClientManager client = createClient();

            client.connectToServer(new TestEndpointAdapter() {
                @Override
                public EndpointConfig getEndpointConfig() {
                    return configuration;
                }

                @Override
                public void onOpen(Session session) {
                    try {
                        session.addMessageHandler(new TestTextMessageHandler(this));
                        session.getBasicRemote().sendText("hello");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(String message) {
                    receivedMessage = message;
                    messageLatch.countDown();
                }
            }, configuration, getURI(TestEndpoint.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals("hello", receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testAnnotatedInstanceAsyncClient() throws DeploymentException {
        Server server = startServer(TestEndpoint.class);
        final ClientEndpointConfig configuration = ClientEndpointConfig.Builder.create().build();

        messageLatch = new CountDownLatch(1);

        try {
            ClientManager client = createClient();


            client.asyncConnectToServer(new TestEndpointAdapter() {
                @Override
                public EndpointConfig getEndpointConfig() {
                    return configuration;
                }

                @Override
                public void onOpen(Session session) {
                    try {
                        session.addMessageHandler(new TestTextMessageHandler(this));
                        session.getBasicRemote().sendText("hello");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(String message) {
                    receivedMessage = message;
                    messageLatch.countDown();
                }
            }, configuration, getURI(TestEndpoint.class));
            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals("hello", receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testAnnotatedInstanceWithDecoding() throws DeploymentException {
        Server server = startServer(TestEndpoint.class);
        messageLatch = new CountDownLatch(1);

        try {
            ClientManager client = createClient();
            client.connectToServer(new ClientTestEndpoint(), getURI(TestEndpoint.class));
            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals("testHello", receivedTestMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testAnnotatedInstanceWithDecodingAsyncClient() throws DeploymentException {
        Server server = startServer(TestEndpoint.class);
        messageLatch = new CountDownLatch(1);

        try {
            ClientManager client = createClient();
            client.asyncConnectToServer(new ClientTestEndpoint(), getURI(TestEndpoint.class));
            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals("testHello", receivedTestMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testAnnotatedClass() throws DeploymentException {
        Server server = startServer(TestEndpoint.class);
        messageLatch = new CountDownLatch(1);
        receivedMessage = null;

        try {
            ClientManager client = createClient();
            client.connectToServer(SimpleClientTestEndpoint.class, getURI(TestEndpoint.class));
            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals("hello", receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testAnnotatedClassAsyncClient() throws DeploymentException {
        Server server = startServer(TestEndpoint.class);
        messageLatch = new CountDownLatch(1);
        receivedMessage = null;

        try {
            ClientManager client = createClient();
            client.asyncConnectToServer(SimpleClientTestEndpoint.class, getURI(TestEndpoint.class));
            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals("hello", receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testNonAnnotatedClass() throws Exception {
        WebSocketContainer wc = ContainerProvider.getWebSocketContainer();
        try {
            wc.connectToServer(String.class, getURI(TestEndpoint.class));
        } catch (DeploymentException de) {
            // Expected exception, ignore
        }
    }

    @Test
    public void testNonAnnotatedClassAsyncClient() throws Exception {
        try {
            createClient().asyncConnectToServer(String.class, getURI(TestEndpoint.class));
        } catch (DeploymentException de) {
            // Expected exception, ignore
        }
    }

    /**
     * Testing the basic annotations.
     *
     * @author Stepan Kopriva (stepan.kopriva at oracle.com)
     */
    @ClientEndpoint(decoders = {TestDecoder.class})
    public class ClientTestEndpoint {

        private static final String SENT_TEST_MESSAGE = "testHello";

        @OnOpen
        public void onOpen(Session p) {
            try {
                p.getBasicRemote().sendText(TestMessage.PREFIX + SENT_TEST_MESSAGE);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @OnMessage
        public void onTestMesage(TestMessage tm) {
            receivedTestMessage = tm.getData();
            messageLatch.countDown();
        }
    }

    @ClientEndpoint
    public static class SimpleClientTestEndpoint {
        private static final String SENT_MESSAGE = "hello";

        public SimpleClientTestEndpoint() {
        }

        @OnOpen
        public void onOpen(Session p) {
            try {
                p.getBasicRemote().sendText(SENT_MESSAGE);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @OnMessage
        public void onMessage(String message) {
            receivedMessage = message;
            messageLatch.countDown();
        }
    }
}
