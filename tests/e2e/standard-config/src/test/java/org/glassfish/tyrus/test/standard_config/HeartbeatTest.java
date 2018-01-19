/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.TyrusSession;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Petr Janouch
 */
public class HeartbeatTest extends TestContainer {

    private static final String PONG_RECEIVED = "pong received";

    @ServerEndpoint(value = "/replyingHeartbeatEndpoint")
    public static class ReplyingHeartbeatEndpoint {

        @OnMessage
        public void onPong(PongMessage pongMessage, Session session) throws IOException {
            session.getBasicRemote().sendText(PONG_RECEIVED);
        }
    }

    @Test
    public void testHeartbeatClient() throws DeploymentException {
        Server server = startServer(ReplyingHeartbeatEndpoint.class);
        try {
            final CountDownLatch messageLatch = new CountDownLatch(3);
            ClientManager client = createClient();
            Session session = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    TyrusSession tyrusSession = (TyrusSession) session;
                    tyrusSession.setHeartbeatInterval(200);

                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            if (message.equals(PONG_RECEIVED)) {
                                messageLatch.countDown();
                            }
                        }
                    });
                }

            }, ClientEndpointConfig.Builder.create().build(), getURI(ReplyingHeartbeatEndpoint.class));
            assertTrue(messageLatch.await(3, TimeUnit.SECONDS));
            session.close();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint(value = "/heartbeatServerEndpoint")
    public static class HeartbeatServerEndpoint {

        @OnOpen
        public void onOpen(Session session) {
            TyrusSession tyrusSession = (TyrusSession) session;
            tyrusSession.setHeartbeatInterval(200);
        }

    }

    @Test
    public void testHeartbeatServer() throws DeploymentException {
        Server server = startServer(HeartbeatServerEndpoint.class);

        try {
            final CountDownLatch pongLatch = new CountDownLatch(3);
            ClientManager client = createClient();
            Session session = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {
                        @Override
                        public void onMessage(PongMessage message) {
                            pongLatch.countDown();
                        }
                    });
                }
            }, ClientEndpointConfig.Builder.create().build(), getURI(HeartbeatServerEndpoint.class));
            assertTrue(pongLatch.await(3, TimeUnit.SECONDS));

            session.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint(value = "/notReplyingHeartbeatEndpoint")
    public static class NotReplyingHeartbeatEndpoint {
    }

    @Test
    public void testSessionTimeout() throws DeploymentException {
        Server server = startServer(NotReplyingHeartbeatEndpoint.class);
        try {
            final CountDownLatch closeLatch = new CountDownLatch(1);
            ClientManager client = createClient();
            Session session = client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    TyrusSession tyrusSession = (TyrusSession) session;
                    tyrusSession.setHeartbeatInterval(100);
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    closeLatch.countDown();
                }

            }, ClientEndpointConfig.Builder.create().build(), getURI(NotReplyingHeartbeatEndpoint.class));
            session.setMaxIdleTimeout(500);
            assertFalse(closeLatch.await(1, TimeUnit.SECONDS));
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testHeartbeatCancel() throws DeploymentException {
        Server server = startServer(ReplyingHeartbeatEndpoint.class);
        final AtomicBoolean firstReplyReceived = new AtomicBoolean(false);
        try {
            final CountDownLatch secondReplyLatch = new CountDownLatch(1);
            ClientManager client = createClient();
            Session session = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(final Session session, EndpointConfig config) {
                    TyrusSession tyrusSession = (TyrusSession) session;
                    tyrusSession.setHeartbeatInterval(300);

                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            if (message.equals(PONG_RECEIVED) && !firstReplyReceived.get()) {
                                TyrusSession tyrusSession = (TyrusSession) session;
                                tyrusSession.setHeartbeatInterval(0);
                                firstReplyReceived.set(true);
                            } else {
                                secondReplyLatch.countDown();
                            }
                        }
                    });
                }

            }, ClientEndpointConfig.Builder.create().build(), getURI(ReplyingHeartbeatEndpoint.class));
            assertFalse(secondReplyLatch.await(1, TimeUnit.SECONDS));
            session.close();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
