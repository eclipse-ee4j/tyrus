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

package org.glassfish.tyrus.tests.servlet.async;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Basic test for TyrusRemoteEndpoint.Async.sendText()
 *
 * @author Jitendra Kotamraju
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class AsyncTextTest extends TestContainer {

    private static final int MESSAGE_NO = 100;
    private static final String CONTEXT_PATH = "/servlet-test-async";

    public AsyncTextTest() {
        setContextPath(CONTEXT_PATH);
    }

    @Test
    public void testTextFuture() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(TextFutureEndpoint.class, ServiceEndpoint.class);

        CountDownLatch sentLatch = new CountDownLatch(MESSAGE_NO);
        CountDownLatch receivedLatch = new CountDownLatch(MESSAGE_NO);

        try {
            final ClientManager client = createClient();
            client.connectToServer(
                    new AsyncFutureClient(sentLatch, receivedLatch),
                    ClientEndpointConfig.Builder.create().build(),
                    getURI(TextFutureEndpoint.class.getAnnotation(ServerEndpoint.class).value()));

            sentLatch.await(5, TimeUnit.SECONDS);
            receivedLatch.await(5, TimeUnit.SECONDS);

            // Check the number of received messages
            Assert.assertEquals("Didn't send all the messages. ", 0, sentLatch.getCount());
            Assert.assertEquals("Didn't receive all the messages. ", 0, receivedLatch.getCount());

            final CountDownLatch serviceLatch = new CountDownLatch(1);
            client.connectToServer(
                    new Endpoint() {
                        @Override
                        public void onOpen(Session session, EndpointConfig config) {
                            session.addMessageHandler(new MessageHandler.Whole<Integer>() {
                                @Override
                                public void onMessage(Integer message) {
                                    Assert.assertEquals("Server callback wasn't called at all cases.", 0,
                                                        message.intValue());
                                    serviceLatch.countDown();
                                }
                            });
                            try {
                                session.getBasicRemote().sendText(ServiceEndpoint.TEXT_FUTURE);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, ClientEndpointConfig.Builder.create().build(),
                    getURI(ServiceEndpoint.class.getAnnotation(ServerEndpoint.class).value()));

            serviceLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals("Didn't receive all the messages. ", 0, receivedLatch.getCount());
        } finally {
            stopServer(server);
        }
    }

    // Client endpoint that sends messages asynchronously
    public static class AsyncFutureClient extends Endpoint {
        private final CountDownLatch sentLatch;
        private final CountDownLatch receivedLatch;
        private final long noMessages;

        public AsyncFutureClient(CountDownLatch sentLatch, CountDownLatch receivedLatch) {
            noMessages = sentLatch.getCount();
            this.sentLatch = sentLatch;
            this.receivedLatch = receivedLatch;
        }

        @Override
        public void onOpen(Session session, EndpointConfig EndpointConfig) {
            try {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String message) {
                        receivedLatch.countDown();
                    }
                });

                for (int i = 0; i < noMessages; i++) {
                    Future future = session.getAsyncRemote().sendText("Message");
                    future.get();
                    sentLatch.countDown();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testTextHandler() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(TextHandlerEndpoint.class, ServiceEndpoint.class);

        CountDownLatch sentLatch = new CountDownLatch(MESSAGE_NO);
        CountDownLatch receivedLatch = new CountDownLatch(MESSAGE_NO);

        try {
            final ClientManager client = createClient();
            client.connectToServer(
                    new AsyncHandlerClient(sentLatch, receivedLatch),
                    ClientEndpointConfig.Builder.create().build(),
                    getURI(TextHandlerEndpoint.class.getAnnotation(ServerEndpoint.class).value()));

            sentLatch.await(5, TimeUnit.SECONDS);
            receivedLatch.await(5, TimeUnit.SECONDS);

            // Check the number of received messages
            Assert.assertEquals("Didn't send all the messages. ", 0, sentLatch.getCount());
            Assert.assertEquals("Didn't receive all the messages. ", 0, receivedLatch.getCount());

            final CountDownLatch serviceLatch = new CountDownLatch(1);
            client.connectToServer(
                    new Endpoint() {
                        @Override
                        public void onOpen(Session session, EndpointConfig config) {
                            session.addMessageHandler(new MessageHandler.Whole<Integer>() {
                                @Override
                                public void onMessage(Integer message) {
                                    Assert.assertEquals("Server callback wasn't called at all cases.", 0,
                                                        message.intValue());
                                    serviceLatch.countDown();
                                }
                            });
                            try {
                                session.getBasicRemote().sendText(ServiceEndpoint.TEXT_HANDLER);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, ClientEndpointConfig.Builder.create().build(),
                    getURI(ServiceEndpoint.class.getAnnotation(ServerEndpoint.class).value()));

            serviceLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals("Didn't receive all the messages. ", 0, receivedLatch.getCount());
        } finally {
            stopServer(server);
        }
    }

    // Client endpoint that sends messages asynchronously
    public static class AsyncHandlerClient extends Endpoint {
        private final CountDownLatch sentLatch;
        private final CountDownLatch receivedLatch;
        private final long noMessages;

        public AsyncHandlerClient(CountDownLatch sentLatch, CountDownLatch receivedLatch) {
            noMessages = sentLatch.getCount();
            this.sentLatch = sentLatch;
            this.receivedLatch = receivedLatch;
        }

        @Override
        public void onOpen(Session session, EndpointConfig EndpointConfig) {
            try {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String message) {
                        receivedLatch.countDown();
                    }
                });

                for (int i = 0; i < noMessages; i++) {
                    session.getAsyncRemote().sendText("Message", new SendHandler() {
                        @Override
                        public void onResult(SendResult result) {
                            if (result.isOK()) {
                                sentLatch.countDown();
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
