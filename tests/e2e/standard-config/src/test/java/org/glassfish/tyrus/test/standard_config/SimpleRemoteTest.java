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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.standard_config.bean.SimpleRemoteTestEndpoint;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the basic behaviour of remote
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class SimpleRemoteTest extends TestContainer {
    private String receivedMessage;
    private static final String SENT_MESSAGE = "Hello World";
    private static final Logger LOGGER = Logger.getLogger(SimpleRemoteTest.class.getName());

    @Test
    public void testSimpleRemote() throws DeploymentException {
        final CountDownLatch messageLatch = new CountDownLatch(1);
        Server server = startServer(SimpleRemoteTestEndpoint.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            final ClientManager client = createClient();
            client.connectToServer(new TestEndpointAdapter() {
                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }

                @Override
                public void onOpen(Session session) {
                    try {
                        session.addMessageHandler(new TestTextMessageHandler(this));
                        session.getBasicRemote().sendText(SENT_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(String message) {
                    receivedMessage = message;
                    messageLatch.countDown();
                }
            }, cec, getURI(SimpleRemoteTestEndpoint.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertTrue("The received message is the same as the sent one", receivedMessage.equals(SENT_MESSAGE));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testSimpleRemoteMT() throws DeploymentException {
        final int clients = 10;
        final CountDownLatch messageLatch = new CountDownLatch(2 * clients);
        final AtomicInteger msgNumber = new AtomicInteger(0);
        Server server = startServer(SimpleRemoteTestEndpoint.class);

        try {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

                        final CountDownLatch perClientLatch = new CountDownLatch(2);
                        final String[] message = new String[]{SENT_MESSAGE + msgNumber.incrementAndGet(),
                                SENT_MESSAGE + msgNumber.incrementAndGet()};
                        // replace ClientManager with MockClientEndpoint to confirm the test passes if the backend
                        // does not have issues
                        final ClientManager client = createClient();
                        final Session session = client.connectToServer(new Endpoint() {

                            @Override
                            public void onOpen(Session session, EndpointConfig EndpointConfig) {
                                try {
                                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                                        @Override
                                        public void onMessage(String s) {
                                            perClientLatch.countDown();
                                            String testString = message[(int) perClientLatch.getCount()];
                                            assertEquals(testString, s);
                                            messageLatch.countDown();
                                        }
                                    });
                                    session.getBasicRemote().sendText(message[1]);
                                    Thread.sleep(100);
                                    session.getBasicRemote().sendText(message[0]);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onError(Session session, Throwable thr) {
                                LOGGER.log(Level.SEVERE, "onError: ");
                                thr.printStackTrace();
                            }
                        }, cec, getURI(SimpleRemoteTestEndpoint.class));
                        perClientLatch.await(5, TimeUnit.SECONDS);
                        session.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            for (int i = 0; i < clients; i++) {
                new Thread(runnable).start();
            }
            messageLatch.await(5, TimeUnit.SECONDS);
            assertTrue("The following number of messages was not delivered correctly: " + messageLatch.getCount()
                               + ". See exception traces above for the complete list of issues.",
                       0 == messageLatch.getCount());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
