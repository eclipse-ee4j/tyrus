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

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
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
import static org.junit.Assert.assertNotNull;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class MaxMessageSizeTest extends TestContainer {

    private volatile CountDownLatch messageLatch;
    private volatile String receivedMessage;

    @ServerEndpoint(value = "/endpoint1")
    public static class Endpoint1 {

        public static volatile CloseReason closeReason = null;
        public static volatile CountDownLatch closeLatch = new CountDownLatch(1);
        public static volatile Throwable throwable = null;


        @OnOpen
        public void onOpen(Session session) {
        }

        @OnMessage(maxMessageSize = 5)
        public String onMessage(String message) {
            return message;
        }

        @OnClose
        public void onClose(CloseReason c) {
            closeReason = c;
            closeLatch.countDown();
        }

        @OnError
        public void onError(Session s, Throwable t) {
            // onError needs to be called after session is closed.
            if (!s.isOpen()) {
                throwable = t;
            }
        }
    }

    @ServerEndpoint(value = "/servicemaxmessage")
    public static class ServiceEndpoint {

        @OnMessage
        public String onMessage(String message) throws InterruptedException {
            if (message.equals("THROWABLE") && Endpoint1.throwable != null) {
                return POSITIVE;
            } else if (message.equals("CLEANUP")) {
                Endpoint1.closeReason = null;
                Endpoint1.throwable = null;
                Endpoint1.closeLatch = new CountDownLatch(1);
                return POSITIVE;
            } else if (message.equals("POSITIVE_EXPECTED")) {
                // if we expect a positive result, we allow waiting for the close reason for a while for stability reasons

                /* There is a race, since the Session#close just sends a close frame asynchronously and does not wait for
                   the connection to be really closed, so in some rare cases the call to the service endpoint can overtake
                   the closing handshake completion. */
                Endpoint1.closeLatch.await(1, TimeUnit.SECONDS);

                if (Endpoint1.closeReason != null
                        && Endpoint1.closeReason.getCloseCode().equals(CloseReason.CloseCodes.TOO_BIG)) {
                    return POSITIVE;
                }
            } else if (message.equals("NEGATIVE_EXPECTED")){
                if (Endpoint1.closeReason != null
                        && Endpoint1.closeReason.getCloseCode().equals(CloseReason.CloseCodes.TOO_BIG)) {
                    return POSITIVE;
                }
            }

            return NEGATIVE;
        }
    }

    @ServerEndpoint(value = "/endpoint2")
    public static class Endpoint2 {

        @OnMessage(maxMessageSize = 5)
        public String doThat(Session s, String message, boolean last) {
            return message;
        }
    }

    @Test
    public void runTestBasic() throws DeploymentException {
        Server server = startServer(Endpoint1.class, ServiceEndpoint.class);

        try {
            messageLatch = new CountDownLatch(1);

            final ClientEndpointConfig clientConfiguration = ClientEndpointConfig.Builder.create().build();
            ClientManager client = createClient();

            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                receivedMessage = message;
                                messageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText("TEST1");
                    } catch (IOException e) {
                        // do nothing.
                    }
                }
            }, clientConfiguration, getURI(Endpoint1.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
            assertEquals("TEST1", receivedMessage);

            final CountDownLatch closedLatch = new CountDownLatch(1);

            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.getBasicRemote().sendText("LONG--");
                    } catch (IOException e) {
                        // do nothing.
                    }
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    if (closeReason.getCloseCode().equals(CloseReason.CloseCodes.TOO_BIG)) {
                        closedLatch.countDown();
                    }
                }
            }, clientConfiguration, getURI(Endpoint1.class));
            closedLatch.await(5, TimeUnit.SECONDS);


            testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "THROWABLE");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void runTestAsync() throws DeploymentException {
        Server server = startServer(Endpoint2.class);

        try {
            messageLatch = new CountDownLatch(1);

            final ClientEndpointConfig clientConfiguration = ClientEndpointConfig.Builder.create().build();
            ClientManager client = createClient();

            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                receivedMessage = message;
                                messageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText("TEST1", false);
                    } catch (IOException e) {
                        // do nothing.
                    }
                }
            }, clientConfiguration, getURI(Endpoint2.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
            assertEquals("TEST1", receivedMessage);

            messageLatch = new CountDownLatch(1);

            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.getBasicRemote().sendText("LONG--", false);
                    } catch (IOException e) {
                        // do nothing.
                    }
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    if (closeReason.getCloseCode().equals(CloseReason.CloseCodes.TOO_BIG)) {
                        messageLatch.countDown();
                    }
                }
            }, clientConfiguration, getURI(Endpoint2.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @ClientEndpoint
    public static class MyClientEndpoint {

        public static volatile CountDownLatch latch;
        public static volatile Throwable throwable;
        public static volatile CloseReason reason;

        @OnMessage(maxMessageSize = 3)
        public void onMessage(String message) {
            latch.countDown();
        }

        @OnError
        public void onError(Session s, Throwable t) {
            // onError needs to be called after session is closed.
            if (!s.isOpen()) {
                throwable = t;
            }

            if (latch.getCount() > 0) {
                latch.countDown();
            }
        }

        @OnClose
        public void onClose(Session session, CloseReason reason) {
            MyClientEndpoint.reason = reason;
        }
    }

    @Test
    public void testClientOne() throws DeploymentException {
        Server server = startServer(Endpoint1.class, ServiceEndpoint.class);

        try {
            ClientManager client = createClient();

            final Session session = client.connectToServer(MyClientEndpoint.class, getURI(Endpoint1.class));
            testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "CLEANUP");

            MyClientEndpoint.latch = new CountDownLatch(1);
            MyClientEndpoint.throwable = null;
            MyClientEndpoint.reason = null;
            session.getBasicRemote().sendText("t");
            MyClientEndpoint.latch.await(1, TimeUnit.SECONDS);
            assertEquals(0, MyClientEndpoint.latch.getCount());

            testViaServiceEndpoint(client, ServiceEndpoint.class, NEGATIVE, "NEGATIVE_EXPECTED");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testClientTwo() throws DeploymentException {
        Server server = startServer(Endpoint1.class, ServiceEndpoint.class);

        try {
            ClientManager client = createClient();

            final Session session = client.connectToServer(MyClientEndpoint.class, getURI(Endpoint1.class));
            testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "CLEANUP");

            MyClientEndpoint.latch = new CountDownLatch(1);
            MyClientEndpoint.throwable = null;
            MyClientEndpoint.reason = null;
            session.getBasicRemote().sendText("te");
            MyClientEndpoint.latch.await(1, TimeUnit.SECONDS);
            assertEquals(0, MyClientEndpoint.latch.getCount());

            testViaServiceEndpoint(client, ServiceEndpoint.class, NEGATIVE, "NEGATIVE_EXPECTED");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testClientThree() throws DeploymentException {
        Server server = startServer(Endpoint1.class, ServiceEndpoint.class);

        try {
            ClientManager client = createClient();

            final Session session = client.connectToServer(MyClientEndpoint.class, getURI(Endpoint1.class));
            testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "CLEANUP");

            MyClientEndpoint.latch = new CountDownLatch(1);
            MyClientEndpoint.throwable = null;
            MyClientEndpoint.reason = null;
            session.getBasicRemote().sendText("tes");
            MyClientEndpoint.latch.await(1, TimeUnit.SECONDS);
            assertEquals(0, MyClientEndpoint.latch.getCount());

            testViaServiceEndpoint(client, ServiceEndpoint.class, NEGATIVE, "NEGATIVE_EXPECTED");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testClientFour() throws DeploymentException {
        Server server = startServer(Endpoint1.class, ServiceEndpoint.class);

        try {
            ClientManager client = createClient();

            final Session session = client.connectToServer(MyClientEndpoint.class, getURI(Endpoint1.class));
            testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "CLEANUP");

            MyClientEndpoint.latch = new CountDownLatch(1);
            MyClientEndpoint.throwable = null;
            MyClientEndpoint.reason = null;
            session.getBasicRemote().sendText("test");
            MyClientEndpoint.latch.await(1, TimeUnit.SECONDS);
            assertEquals(0, MyClientEndpoint.latch.getCount());
            assertNotNull(MyClientEndpoint.throwable);

            testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "POSITIVE_EXPECTED");
            assertEquals("CloseReason on client is not: " + CloseReason.CloseCodes.TOO_BIG.getCode(),
                         CloseReason.CloseCodes.TOO_BIG.getCode(), MyClientEndpoint.reason.getCloseCode().getCode());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
