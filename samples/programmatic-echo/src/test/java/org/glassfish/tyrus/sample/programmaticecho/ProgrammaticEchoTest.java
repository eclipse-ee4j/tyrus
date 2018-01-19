/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.programmaticecho;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ProgrammaticEchoTest extends TestContainer {

    public ProgrammaticEchoTest() {
        setContextPath("/sample-programmatic-echo");
    }

    @Test
    public void testEcho() throws DeploymentException {
        final Server server = startServer(MyWsConfiguration.class);

        try {
            final CountDownLatch messageLatch = new CountDownLatch(1);
            final CountDownLatch onCloseLatch = new CountDownLatch(1);

            final ClientManager client = ClientManager.createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                assertEquals(message, "Do or do not, there is no try. (from your server)");
                                messageLatch.countDown();
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

            }, ClientEndpointConfig.Builder.create().build(), getURI("/echo"));

            assertTrue(messageLatch.await(1, TimeUnit.SECONDS));
            assertTrue(onCloseLatch.await(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            stopServer(server);
        }
    }
}
