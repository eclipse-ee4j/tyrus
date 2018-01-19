/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.cdi;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test works properly with EE container only.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class CdiTest extends TestContainer {

    private static final String SENT_MESSAGE = "Do or do not, there is no try.";
    private static volatile int result;

    public CdiTest() {
        setContextPath("/sample-cdi");
    }

    @Test
    public void testSimple() throws DeploymentException, InterruptedException, IOException {
        final String host = System.getProperty("tyrus.test.host");
        if (host == null) {
            return;
        }

        final CountDownLatch messageLatch = new CountDownLatch(1);

        final ClientManager client = ClientManager.createClient();
        client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig EndpointConfig) {
                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            assertEquals(message, String.format("%s (from your server)", SENT_MESSAGE));
                            messageLatch.countDown();
                        }
                    });

                    session.getBasicRemote().sendText(SENT_MESSAGE);
                } catch (IOException e) {
                    //do nothing
                }
            }
        }, ClientEndpointConfig.Builder.create().build(), getURI("/simple"));

        messageLatch.await(2, TimeUnit.SECONDS);
        assertEquals("Number of received messages is not 0.", 0, messageLatch.getCount());
    }

    @Test
    public void testStatefulOneClientTwoMessages() throws DeploymentException, InterruptedException, IOException {
        final String host = System.getProperty("tyrus.test.host");
        if (host == null) {
            return;
        }

        final CountDownLatch messageLatch = new CountDownLatch(2);

        final ClientManager client = ClientManager.createClient();
        client.connectToServer(new Endpoint() {
            boolean first = true;
            int firstMessage;
            int secondMessage;

            @Override
            public void onOpen(final Session session, EndpointConfig EndpointConfig) {
                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {

                        @Override
                        public void onMessage(String message) {
                            try {
                                if (first) {
                                    firstMessage = Integer.parseInt(message.split(":")[1]);
                                    messageLatch.countDown();
                                    try {
                                        session.getBasicRemote().sendText(SENT_MESSAGE);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    first = false;
                                } else {
                                    secondMessage = Integer.parseInt(message.split(":")[1]);
                                    assertEquals("secondMessage - firstMessage != 1", 1, secondMessage - firstMessage);
                                    messageLatch.countDown();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    session.getBasicRemote().sendText("Do or do not, there is no try.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, ClientEndpointConfig.Builder.create().build(), getURI("/injectingstateful"));

        messageLatch.await(2, TimeUnit.SECONDS);
        assertEquals("Number of received messages is not 0.", 0, messageLatch.getCount());

    }

    @Test
    public void testInjectedStatefulTwoMessagesFromTwoClients() throws InterruptedException, DeploymentException,
            IOException {
        testFromTwoClients("/injectingstateful", 0);
    }

    @Test
    public void testInjectedSingletonTwoMessagesFromTwoClients() throws InterruptedException, DeploymentException,
            IOException {
        testFromTwoClients("/injectingsingleton", 1);
    }

    @Test
    public void testStatefulTwoMessagesFromTwoClients() throws InterruptedException, DeploymentException, IOException {
        testFromTwoClients("/stateful", 0);
    }

    @Test
    public void testSingletonTwoMessagesFromTwoClients() throws InterruptedException, DeploymentException, IOException {
        testFromTwoClients("/singleton", 1);
    }

    public void testFromTwoClients(String path, int diff) throws DeploymentException, InterruptedException,
            IOException {
        final String host = System.getProperty("tyrus.test.host");
        if (host == null) {
            return;
        }
        ClientManager client = ClientManager.createClient();

        int value1 = testOneClient(client, path);
        int value2 = testOneClient(client, path);

        assertEquals("The difference is not as expected", diff, value2 - value1);
    }

    public int testOneClient(ClientManager client, String path) throws InterruptedException, DeploymentException,
            IOException {

        final CountDownLatch messageLatch = new CountDownLatch(1);
        client.connectToServer(new Endpoint() {

            @Override
            public void onOpen(final Session session, EndpointConfig EndpointConfig) {
                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            result = Integer.parseInt(message.split(":")[1]);
                            messageLatch.countDown();
                        }
                    });

                    session.getBasicRemote().sendText("Do or do not, there is no try.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Session session, Throwable thr) {
                thr.printStackTrace();
            }
        }, ClientEndpointConfig.Builder.create().build(), getURI(path));

        messageLatch.await(2, TimeUnit.SECONDS);
        assertEquals("Number of received messages is not 0.", 0, messageLatch.getCount());

        return result;
    }

    @Test
    public void testApplicationScoped() throws DeploymentException, InterruptedException, IOException {
        final String host = System.getProperty("tyrus.test.host");
        if (host == null) {
            return;
        }

        final CountDownLatch messageLatch = new CountDownLatch(1);

        final ClientManager client = ClientManager.createClient();
        client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig EndpointConfig) {
                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            System.out.println("IAS received: " + message);
                            assertEquals(message, String.format("%s (from your server)", SENT_MESSAGE));
                            messageLatch.countDown();
                        }
                    });

                    session.getBasicRemote().sendText(SENT_MESSAGE);
                } catch (IOException e) {
                    //do nothing
                }
            }
        }, ClientEndpointConfig.Builder.create().build(), getURI("/injectingappscoped"));

        messageLatch.await(2, TimeUnit.SECONDS);
        assertEquals("Number of received messages is not 0.", 0, messageLatch.getCount());
    }

    @Test
    public void testStatelessRemoteInterface() throws DeploymentException, InterruptedException, IOException {
        final String host = System.getProperty("tyrus.test.host");
        if (host == null) {
            return;
        }

        final CountDownLatch messageLatch = new CountDownLatch(1);

        final ClientManager client = ClientManager.createClient();
        client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig EndpointConfig) {
                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            assertEquals(message, SENT_MESSAGE);
                            messageLatch.countDown();
                        }
                    });

                    session.getBasicRemote().sendText(SENT_MESSAGE);
                } catch (IOException e) {
                    //do nothing
                }
            }
        }, ClientEndpointConfig.Builder.create().build(), getURI("/statelessRemoteInterfaceEndpoint"));

        assertTrue(messageLatch.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void testStatelessRemoteInterfaceRef() throws DeploymentException, InterruptedException, IOException {
        final String host = System.getProperty("tyrus.test.host");
        if (host == null) {
            return;
        }

        final CountDownLatch messageLatch = new CountDownLatch(1);

        final ClientManager client = ClientManager.createClient();
        client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig EndpointConfig) {
                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            assertEquals(message, SENT_MESSAGE);
                            messageLatch.countDown();
                        }
                    });

                    session.getBasicRemote().sendText(SENT_MESSAGE);
                } catch (IOException e) {
                    //do nothing
                }
            }
        }, ClientEndpointConfig.Builder.create().build(), getURI("/statelessRemoteInterfaceRefEndpoint"));

        assertTrue(messageLatch.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void testProgrammaticStatelessRemoteInterface() throws DeploymentException, InterruptedException,
            IOException {
        final String host = System.getProperty("tyrus.test.host");
        if (host == null) {
            return;
        }

        final CountDownLatch messageLatch = new CountDownLatch(1);

        final ClientManager client = ClientManager.createClient();
        client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig EndpointConfig) {
                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            assertEquals(message, SENT_MESSAGE);
                            messageLatch.countDown();
                        }
                    });

                    session.getBasicRemote().sendText(SENT_MESSAGE);
                } catch (IOException e) {
                    //do nothing
                }
            }
        }, ClientEndpointConfig.Builder.create().build(), getURI("/programmaticStatelessRemoteInterfaceEndpoint"));

        assertTrue(messageLatch.await(2, TimeUnit.SECONDS));
    }
}
