/*
 * Copyright (c) 2012, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.echo;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Pavel Bucek
 */
public class EchoTest extends TestContainer {

    public EchoTest() {
        setContextPath("/sample-echo");
    }

    @Test
    public void testEcho() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(EchoEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);
        final CountDownLatch onOpenLatch = new CountDownLatch(1);
        final CountDownLatch onCloseLatch = new CountDownLatch(1);

        try {
            final ClientManager client = createClient();

            final Session session = client.connectToServer(new Endpoint() {
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

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    System.out.println("### Client session closed: " + closeReason);
                    onCloseLatch.countDown();
                }
            }, ClientEndpointConfig.Builder.create().build(), getURI(EchoEndpoint.class));

            assertTrue(messageLatch.await(1, TimeUnit.SECONDS));
            assertTrue(onOpenLatch.await(1, TimeUnit.SECONDS));
            assertTrue(onCloseLatch.await(10, TimeUnit.SECONDS));

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }
}
