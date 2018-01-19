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

package org.glassfish.tyrus.test.servlet.session;

import java.io.IOException;
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
import org.glassfish.tyrus.tests.servlet.session.CloseClientEndpoint;
import org.glassfish.tyrus.tests.servlet.session.CloseServerEndpoint;
import org.glassfish.tyrus.tests.servlet.session.ServiceEndpoint;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import junit.framework.Assert;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class SessionCloseApplicationTest extends TestContainer {
    private static final String CONTEXT_PATH = "/session-test";
    private static String messageReceived1 = "not received.";
    private static String messageReceived2 = "not received.";
    private static boolean inCloseSendMessageExceptionThrown1 = false;
    private static boolean inCloseSendMessageExceptionThrown2 = false;

    public SessionCloseApplicationTest() {
        setContextPath(CONTEXT_PATH);
    }

    @Test
    public void testCloseSessionServer() throws DeploymentException {

        final CountDownLatch clientLatch = new CountDownLatch(1);

        boolean exceptionAddMessageHandlerThrown = false;
        boolean exceptionRemoveMessageHandlerThrown = false;
        boolean exceptionGetAsyncRemoteThrown = false;
        boolean exceptionGetBasicRemoteThrown = false;

        final Server server = startServer(CloseServerEndpoint.class, ServiceEndpoint.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();
            Session clientSession = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig endpointConfig) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {

                        @Override
                        public void onMessage(String s) {
                        }
                    });
                    try {
                        session.getBasicRemote().sendText("message");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(javax.websocket.Session session, javax.websocket.CloseReason closeReason) {
                    try {
                        session.getBasicRemote().sendText("Hello.");
                    } catch (Exception e) {
                        inCloseSendMessageExceptionThrown1 = true;
                    }

                    // clientLatch.countDown();
                }

                @Override
                public void onError(javax.websocket.Session session, Throwable thr) {
                    thr.printStackTrace();
                }
            }, cec, getURI(CloseServerEndpoint.class.getAnnotation(ServerEndpoint.class).value()));

            // assertTrue(clientLatch.await(5, TimeUnit.SECONDS));
            clientLatch.await(5, TimeUnit.SECONDS);

            try {
                clientSession.addMessageHandler(null);
            } catch (IllegalStateException e) {
                exceptionAddMessageHandlerThrown = true;
            }

            try {
                clientSession.removeMessageHandler(null);
            } catch (IllegalStateException e) {
                exceptionRemoveMessageHandlerThrown = true;
            }

            try {
                clientSession.getBasicRemote();
            } catch (IllegalStateException e) {
                exceptionGetBasicRemoteThrown = true;
            }

            try {
                clientSession.getAsyncRemote();
            } catch (IllegalStateException e) {
                exceptionGetAsyncRemoteThrown = true;
            }

            Assert.assertEquals(true, exceptionAddMessageHandlerThrown);
            Assert.assertEquals(true, exceptionGetAsyncRemoteThrown);
            Assert.assertEquals(true, exceptionGetBasicRemoteThrown);
            Assert.assertEquals(true, exceptionRemoveMessageHandlerThrown);
            Assert.assertEquals(true, inCloseSendMessageExceptionThrown1);

            final CountDownLatch messageLatch = new CountDownLatch(1);

            ClientManager client2 = createClient();
            client2.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig endpointConfig) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {

                        @Override
                        public void onMessage(String s) {
                            messageReceived1 = s;
                            messageLatch.countDown();
                        }
                    });
                    try {
                        session.getBasicRemote().sendText("server");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onError(javax.websocket.Session session, Throwable thr) {
                    thr.printStackTrace();
                }

            }, cec, getURI(ServiceEndpoint.class.getAnnotation(ServerEndpoint.class).value()));
            messageLatch.await(3, TimeUnit.SECONDS);
            assertEquals("Received message was not 1.", messageReceived1, "1");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testCloseSessionClient() throws DeploymentException {

        final CountDownLatch clientLatch = new CountDownLatch(1);

        boolean exceptionAddMessageHandlerThrown = false;
        boolean exceptionRemoveMessageHandlerThrown = false;
        boolean exceptionGetAsyncRemoteThrown = false;
        boolean exceptionGetBasicRemoteThrown = false;

        final Server server = startServer(CloseClientEndpoint.class, ServiceEndpoint.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();
            Session clientSession = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(final Session session, EndpointConfig endpointConfig) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {

                        @Override
                        public void onMessage(String s) {
                            try {
                                session.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    try {
                        session.getBasicRemote().sendText("message");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(javax.websocket.Session session, javax.websocket.CloseReason closeReason) {
                    try {
                        session.getBasicRemote().sendText("Hello");
                    } catch (Exception e) {
                        inCloseSendMessageExceptionThrown2 = true;
                    }
                }

                @Override
                public void onError(javax.websocket.Session session, Throwable thr) {
                    thr.printStackTrace();
                }
            }, cec, getURI(CloseClientEndpoint.class.getAnnotation(ServerEndpoint.class).value()));

            clientLatch.await(5, TimeUnit.SECONDS);

            try {
                clientSession.addMessageHandler(null);
            } catch (IllegalStateException e) {
                exceptionAddMessageHandlerThrown = true;
            }

            try {
                clientSession.removeMessageHandler(null);
            } catch (IllegalStateException e) {
                exceptionRemoveMessageHandlerThrown = true;
            }

            try {
                clientSession.getBasicRemote();
            } catch (IllegalStateException e) {
                exceptionGetBasicRemoteThrown = true;
            }

            try {
                clientSession.getAsyncRemote();
            } catch (IllegalStateException e) {
                exceptionGetAsyncRemoteThrown = true;
            }

            Assert.assertEquals(true, exceptionAddMessageHandlerThrown);
            Assert.assertEquals(true, exceptionGetAsyncRemoteThrown);
            Assert.assertEquals(true, exceptionGetBasicRemoteThrown);
            Assert.assertEquals(true, exceptionRemoveMessageHandlerThrown);
            Assert.assertEquals(true, inCloseSendMessageExceptionThrown2);

            final CountDownLatch messageLatch = new CountDownLatch(1);

            ClientManager client2 = createClient();
            client2.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig endpointConfig) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {

                        @Override
                        public void onMessage(String s) {
                            messageReceived2 = s;
                            messageLatch.countDown();
                        }
                    });
                    try {
                        session.getBasicRemote().sendText("client");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onError(javax.websocket.Session session, Throwable thr) {
                    thr.printStackTrace();
                }

            }, cec, getURI(ServiceEndpoint.class.getAnnotation(ServerEndpoint.class).value()));
            messageLatch.await(3, TimeUnit.SECONDS);
            assertEquals("Received message was not 1.", messageReceived2, "1");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testSessionTimeoutClient() throws DeploymentException {

        final Server server = startServer(CloseClientEndpoint.class, ServiceEndpoint.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();
            Session clientSession = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(final Session session, EndpointConfig endpointConfig) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {

                        @Override
                        public void onMessage(String s) {
                        }
                    });
                }

                @Override
                public void onError(javax.websocket.Session session, Throwable thr) {
                    thr.printStackTrace();
                }
            }, cec, getURI(CloseClientEndpoint.class.getAnnotation(ServerEndpoint.class).value()));

            clientSession.setMaxIdleTimeout(100);
            Thread.sleep(200);
            Assert.assertFalse("Session is not closed", clientSession.isOpen());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
