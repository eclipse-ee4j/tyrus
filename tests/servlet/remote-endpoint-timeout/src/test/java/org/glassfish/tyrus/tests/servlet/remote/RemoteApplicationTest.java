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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Ignore;
import org.junit.Test;

import junit.framework.Assert;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
@Ignore
public class RemoteApplicationTest extends TestContainer {

    private static String receivedMessage1;
    private static String receivedMessage2;
    private static String receivedMessage3;
    private static String receivedMessage4;
    private static final String CONTEXT_PATH = "/remote-test";

    public RemoteApplicationTest() {
        setContextPath(CONTEXT_PATH);
    }

    @Test
    public void testTimeoutByHandler() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(TimeoutEndpointResultByHandler.class, ServiceEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                            @Override
                            public void onMessage(ByteBuffer buffer) {
                                Assert.assertTrue(false);
                                messageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText("Message.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(),
                                   getURI(TimeoutEndpointResultByHandler.class.getAnnotation(ServerEndpoint.class)
                                                                              .value()));

            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals(1, messageLatch.getCount());

            final CountDownLatch serviceMessageLatch = new CountDownLatch(1);
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                System.out.println("Received message: " + message);
                                receivedMessage1 = message;
                                serviceMessageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText("handler");
                    } catch (IOException e) {
                        // do nothing
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(),
                                   getURI(ServiceEndpoint.class.getAnnotation(ServerEndpoint.class).value()));

            serviceMessageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals(0, serviceMessageLatch.getCount());
            Assert.assertTrue(receivedMessage1.equals("1"));
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testTimeoutByFuture() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(TimeoutEndpointResultByFuture.class, ServiceEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                            @Override
                            public void onMessage(ByteBuffer buffer) {
                                Assert.assertTrue(false);
                                messageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText("Message.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(),
                                   getURI(TimeoutEndpointResultByFuture.class.getAnnotation(ServerEndpoint.class)
                                                                             .value()));

            messageLatch.await(1, TimeUnit.SECONDS);
            Assert.assertEquals(1, messageLatch.getCount());

            final CountDownLatch serviceMessageLatch = new CountDownLatch(1);
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                System.out.println("Received message: " + message);
                                receivedMessage2 = message;
                                serviceMessageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText("future");
                    } catch (IOException e) {
                        // do nothing
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(),
                                   getURI(ServiceEndpoint.class.getAnnotation(ServerEndpoint.class).value()));

            serviceMessageLatch.await(1, TimeUnit.SECONDS);
            Assert.assertEquals(0, serviceMessageLatch.getCount());
            Assert.assertTrue(receivedMessage2.equals("1"));
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testNoTimeoutByHandler() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(NoTimeoutEndpointResultByHandler.class, ServiceEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);
        final String messageToSend = "M";

        try {
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String buffer) {
                                System.out.println("Received from action3: " + buffer);
                                Assert.assertTrue(messageToSend.equals(buffer));
                                messageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText(messageToSend);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(),
                                   getURI(NoTimeoutEndpointResultByHandler.class.getAnnotation(ServerEndpoint.class)
                                                                                .value()));

            messageLatch.await(2, TimeUnit.SECONDS);
            Assert.assertEquals("Received message should be one.", 0, messageLatch.getCount());

            final CountDownLatch serviceMessageLatch = new CountDownLatch(1);
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                System.out.println("Received message: " + message);
                                receivedMessage3 = message;
                                serviceMessageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText("nohandler");
                    } catch (IOException e) {
                        // do nothing
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(),
                                   getURI(ServiceEndpoint.class.getAnnotation(ServerEndpoint.class).value()));

            serviceMessageLatch.await(1, TimeUnit.SECONDS);
            Assert.assertEquals("One message should be received.", 0, serviceMessageLatch.getCount());
            Assert.assertTrue("Received service message should be \"1\".", receivedMessage3.equals("1"));
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testNoTimeoutByFuture() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(NoTimeoutEndpointResultByFuture.class, ServiceEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);
        final String messageToSend = "M";

        try {
            final ClientManager client = createClient();
            Session clientSession = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String buffer) {
                                System.out.println("Received from action3: " + buffer);
                                Assert.assertTrue(messageToSend.equals(buffer));
                                messageLatch.countDown();
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(), getURI(NoTimeoutEndpointResultByFuture.class
                                                                             .getAnnotation(ServerEndpoint.class)
                                                                             .value()));
            clientSession.getBasicRemote().sendText(messageToSend);

            messageLatch.await(3, TimeUnit.SECONDS);
            Assert.assertEquals("Received message should be one", 0, messageLatch.getCount());

            final CountDownLatch serviceMessageLatch = new CountDownLatch(1);
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                System.out.println("Received message: " + message);
                                receivedMessage4 = message;
                                serviceMessageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText("nofuture");
                    } catch (IOException e) {
                        // do nothing
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(),
                                   getURI(ServiceEndpoint.class.getAnnotation(ServerEndpoint.class).value()));

            serviceMessageLatch.await(1, TimeUnit.SECONDS);
            Assert.assertEquals("One message should be received.", 0, serviceMessageLatch.getCount());
            Assert.assertTrue("Received service message should be \"1\".", receivedMessage4.equals("1"));
        } finally {
            stopServer(server);
        }
    }
}
