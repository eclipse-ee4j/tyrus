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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class SessionTimeoutTest extends TestContainer {

    @ServerEndpoint(value = "/timeout3")
    public static class SessionTimeoutEndpoint {
        private static final CountDownLatch onClosedCalled = new CountDownLatch(1);
        private static final long TIMEOUT = 300;

        @OnOpen
        public void onOpen(Session session) {
            session.setMaxIdleTimeout(TIMEOUT);
        }

        @OnClose
        public void onClose(CloseReason closeReason) {
            //TYRUS-230
            if (closeReason.getCloseCode() == CloseReason.CloseCodes.CLOSED_ABNORMALLY) {
                onClosedCalled.countDown();
            }
        }
    }

    @Test
    public void testSessionTimeout() throws DeploymentException {
        Server server = startServer(SessionTimeoutEndpoint.class, ServiceEndpoint.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            final CountDownLatch latch = new CountDownLatch(1);
            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {

                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    //TYRUS-230
                    assertEquals(1000, closeReason.getCloseCode().getCode());
                    latch.countDown();
                }
            }, cec, getURI(SessionTimeoutEndpoint.class));

            assertTrue(latch.await(5, TimeUnit.SECONDS));

            testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "SessionTimeoutEndpoint");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint(value = "/servicesessiontimeout")
    public static class ServiceEndpoint {

        @OnMessage
        public String onMessage(String message) throws InterruptedException {
            if (message.equals("SessionTimeoutEndpoint")) {
                if (SessionTimeoutEndpoint.onClosedCalled.await(1, TimeUnit.SECONDS)) {
                    return POSITIVE;
                }
            } else if (message.equals("SessionNoTimeoutEndpoint")) {
                if (!SessionNoTimeoutEndpoint.onClosedCalled.get()) {
                    return POSITIVE;
                }
            } else if (message.equals("SessionTimeoutChangedEndpoint")) {
                if (SessionTimeoutChangedEndpoint.latch.await(1, TimeUnit.SECONDS)
                        && SessionTimeoutChangedEndpoint.closedNormally.get()) {
                    return POSITIVE;
                }
            }

            return NEGATIVE;
        }
    }

    @ServerEndpoint(value = "/timeout2")
    public static class SessionNoTimeoutEndpoint {
        public static final AtomicBoolean onClosedCalled = new AtomicBoolean(false);
        private static final long TIMEOUT = 400;
        private final AtomicInteger counter = new AtomicInteger(0);

        @OnOpen
        public void onOpen(Session session) {
            session.setMaxIdleTimeout(TIMEOUT);
            onClosedCalled.set(false);
        }

        @OnMessage
        public void onMessage(String message, Session session) {
            System.out.println("Message received: " + message);
            if (counter.incrementAndGet() == 3) {
                try {
                    if (!onClosedCalled.get()) {
                        session.getBasicRemote().sendText(POSITIVE);
                    } else {
                        session.getBasicRemote().sendText(NEGATIVE);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @OnClose
        public void onClose() {
            onClosedCalled.set(true);
        }
    }

    @Test
    public void testSessionNoTimeoutRaised() throws DeploymentException {
        Server server = startServer(SessionNoTimeoutEndpoint.class, ServiceEndpoint.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            final CountDownLatch latch = new CountDownLatch(1);

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            System.out.println("Client message received");
                            assertEquals(POSITIVE, message);
                            latch.countDown();
                        }
                    });

                    try {
                        session.getBasicRemote().sendText("Nothing");
                        Thread.sleep(250);
                        session.getBasicRemote().sendText("Nothing");
                        Thread.sleep(250);
                        session.getBasicRemote().sendText("Nothing");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(SessionNoTimeoutEndpoint.class));
            assertTrue(latch.await(2, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint(value = "/timeout4")
    public static class SessionTimeoutChangedEndpoint {
        public static final CountDownLatch latch = new CountDownLatch(1);
        public static final AtomicBoolean closedNormally = new AtomicBoolean(false);
        private long timeoutSetTime;
        private static final long TIMEOUT1 = 300;
        private static final long TIMEOUT2 = 700;
        private static boolean first = true;

        @OnOpen
        public void onOpen(Session session) {
            session.setMaxIdleTimeout(TIMEOUT1);
            timeoutSetTime = System.currentTimeMillis();
        }

        @OnMessage
        public String message(String message, Session session) {
            if (first) {
                session.setMaxIdleTimeout(TIMEOUT2);
                timeoutSetTime = System.currentTimeMillis();
                first = false;
            }
            return "message";
        }

        @OnClose
        public void onClose() {
            closedNormally.set(System.currentTimeMillis() - timeoutSetTime - TIMEOUT2 < 20);
            latch.countDown();
        }
    }

    @Test
    public void testSessionTimeoutChanged() throws DeploymentException {
        Server server = startServer(SessionTimeoutChangedEndpoint.class, ServiceEndpoint.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();
            client.connectToServer(new TestEndpointAdapter() {
                @Override
                public void onMessage(String message) {

                }

                @Override
                public void onOpen(Session session) {
                    try {
                        session.getBasicRemote().sendText("Nothing");
                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }
            }, cec, getURI(SessionTimeoutChangedEndpoint.class));

            testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "SessionTimeoutChangedEndpoint");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint(value = "/timeout1")
    public static class SessionClientTimeoutEndpoint {
        public static final AtomicBoolean clientOnCloseCalled = new AtomicBoolean(false);

    }

    @Test
    public void testSessionClientTimeoutSession() throws DeploymentException {
        Server server = startServer(SessionClientTimeoutEndpoint.class);
        final CountDownLatch onCloseLatch = new CountDownLatch(1);
        SessionClientTimeoutEndpoint.clientOnCloseCalled.set(false);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();
            Session session = client.connectToServer(new TestEndpointAdapter() {
                @Override
                public void onMessage(String message) {
                }

                @Override
                public void onOpen(Session session) {
                }

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    SessionClientTimeoutEndpoint.clientOnCloseCalled.set(true);
                    onCloseLatch.countDown();
                }
            }, cec, getURI(SessionClientTimeoutEndpoint.class));
            session.setMaxIdleTimeout(200);

            onCloseLatch.await(2, TimeUnit.SECONDS);
            assertTrue(SessionClientTimeoutEndpoint.clientOnCloseCalled.get());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testSessionClientTimeoutSessionOnOpen() throws DeploymentException {
        Server server = startServer(SessionClientTimeoutEndpoint.class);
        final CountDownLatch onCloseLatch = new CountDownLatch(1);
        SessionClientTimeoutEndpoint.clientOnCloseCalled.set(false);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();
            client.connectToServer(new TestEndpointAdapter() {
                @Override
                public void onMessage(String message) {
                }

                @Override
                public void onOpen(Session session) {
                    session.setMaxIdleTimeout(200);
                }

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    SessionClientTimeoutEndpoint.clientOnCloseCalled.set(true);
                    onCloseLatch.countDown();
                }
            }, cec, getURI(SessionClientTimeoutEndpoint.class));

            onCloseLatch.await(2, TimeUnit.SECONDS);
            assertTrue(SessionClientTimeoutEndpoint.clientOnCloseCalled.get());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testSessionClientTimeoutContainer() throws DeploymentException {
        Server server = startServer(SessionClientTimeoutEndpoint.class);
        final CountDownLatch onCloseLatch = new CountDownLatch(1);
        SessionClientTimeoutEndpoint.clientOnCloseCalled.set(false);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            final ClientManager client = createClient();
            client.setDefaultMaxSessionIdleTimeout(200);
            Session session = client.connectToServer(new TestEndpointAdapter() {
                @Override
                public void onMessage(String message) {
                }

                @Override
                public void onOpen(Session session) {
                }

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    SessionClientTimeoutEndpoint.clientOnCloseCalled.set(true);
                    onCloseLatch.countDown();
                }
            }, cec, getURI(SessionClientTimeoutEndpoint.class));

            onCloseLatch.await(2, TimeUnit.SECONDS);
            assertTrue(SessionClientTimeoutEndpoint.clientOnCloseCalled.get());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testSessionTimeoutReset1() throws DeploymentException {
        Server server = startServer(SessionClientTimeoutEndpoint.class);
        final CountDownLatch onCloseLatch = new CountDownLatch(1);
        SessionClientTimeoutEndpoint.clientOnCloseCalled.set(false);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            final ClientManager client = createClient();
            client.setDefaultMaxSessionIdleTimeout(1000);
            Session session = client.connectToServer(new TestEndpointAdapter() {
                @Override
                public void onMessage(String message) {
                }

                @Override
                public void onOpen(Session session) {
                }

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    System.out.println(System.currentTimeMillis() + "### !closed " + closeReason);
                    SessionClientTimeoutEndpoint.clientOnCloseCalled.set(true);
                    onCloseLatch.countDown();
                }
            }, cec, getURI(SessionClientTimeoutEndpoint.class));

            assertTrue(session.getMaxIdleTimeout() == 1000);
            session.setMaxIdleTimeout(0);

            assertFalse(onCloseLatch.await(4, TimeUnit.SECONDS));
            assertFalse(SessionClientTimeoutEndpoint.clientOnCloseCalled.get());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testSessionTimeoutReset2() throws DeploymentException {
        Server server = startServer(SessionClientTimeoutEndpoint.class);
        final CountDownLatch onCloseLatch = new CountDownLatch(1);
        SessionClientTimeoutEndpoint.clientOnCloseCalled.set(false);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            final ClientManager client = createClient();
            client.setDefaultMaxSessionIdleTimeout(1000);
            Session session = client.connectToServer(new TestEndpointAdapter() {
                @Override
                public void onMessage(String message) {
                }

                @Override
                public void onOpen(Session session) {
                }

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    System.out.println(System.currentTimeMillis() + "### !closed " + closeReason);
                    SessionClientTimeoutEndpoint.clientOnCloseCalled.set(true);
                    onCloseLatch.countDown();
                }
            }, cec, getURI(SessionClientTimeoutEndpoint.class));

            assertTrue(session.getMaxIdleTimeout() == 1000);
            session.setMaxIdleTimeout(-10);

            assertFalse(onCloseLatch.await(4, TimeUnit.SECONDS));
            assertFalse(SessionClientTimeoutEndpoint.clientOnCloseCalled.get());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
