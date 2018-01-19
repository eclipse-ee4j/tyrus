/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.echo.https;

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
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.fail;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class EchoTest extends TestContainer {

    public EchoTest() {
        setContextPath("/sample-echo-https");
    }

    @Test
    public void testEcho() throws DeploymentException, InterruptedException, IOException {
        if (System.getProperty("tyrus.test.host") == null) {
            return;
        }

        final Server server = startServer(EchoEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);
        final CountDownLatch onOpenLatch = new CountDownLatch(1);

        try {
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                System.out.println("### Received: " + message);

                                if (message.equals("Do or do not, there is no try. (from your server)")) {
                                    messageLatch.countDown();
                                } else if (message.equals("onOpen")) {
                                    onOpenLatch.countDown();
                                }
                            }
                        });

                        session.getBasicRemote().sendText("Do or do not, there is no try.");
                    } catch (IOException e) {
                        // do nothing
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(), getURI(EchoEndpoint.class, "wss"));

            messageLatch.await(1, TimeUnit.SECONDS);
            if (messageLatch.getCount() != 0 || onOpenLatch.getCount() != 0) {
                fail();
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testEcho100k() throws DeploymentException, InterruptedException, IOException {
        if (System.getProperty("tyrus.test.host") == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("1234567890123456789012345678901234567890123456789"
                              + "012345678901234567890123456789012345678901234567890");
        }

        final String MESSAGE = sb.toString();

        final Server server = startServer(EchoEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);
        final CountDownLatch onOpenLatch = new CountDownLatch(1);

        try {
            final ClientManager client = createClient();
            Session session = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            System.out.println("### Received: " + message);

                            if (message.equals(MESSAGE + " (from your server)")) {
                                messageLatch.countDown();
                            } else if (message.equals("onOpen")) {
                                onOpenLatch.countDown();
                            }
                        }
                    });

                }
            }, ClientEndpointConfig.Builder.create().build(), getURI(EchoEndpoint.class, "wss"));
            session.getBasicRemote().sendText(MESSAGE);

            messageLatch.await(60, TimeUnit.SECONDS);
            if (messageLatch.getCount() != 0 || onOpenLatch.getCount() != 0) {
                fail();
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }
}
