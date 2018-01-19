/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.test.e2e.non_deployable;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.server.Server;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * Ephemeral port test.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class EphemeralPortTest {

    private static final String MESSAGE = "It's a trap!";
    private static final Logger LOGGER = Logger.getLogger(EphemeralPortTest.class.getName());

    @Test
    public void testEphemeralPort() throws DeploymentException, IOException, InterruptedException {
        Server server = new Server("localhost", -1, null, null, EphemeralPortTestEndpoint.class);
        server.start();

        final int port = server.getPort();
        final CountDownLatch latch = new CountDownLatch(1);

        try {
            ContainerProvider.getWebSocketContainer().connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {

                    session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            LOGGER.info("Session [" + session.getId() + "] RECEIVED: " + message);
                            if (MESSAGE.equals(message)) {
                                latch.countDown();
                            }
                        }
                    });

                    try {
                        session.getBasicRemote().sendText(MESSAGE);
                        LOGGER.info("Session [" + session.getId() + "] SENT: " + MESSAGE);
                    } catch (IOException e) {
                        // ignore.
                    }
                }


            }, URI.create("ws://localhost:" + port));

            assertTrue(latch.await(3, TimeUnit.SECONDS));
        } finally {
            server.stop();
        }
    }

    @Test
    public void testEphemeralPortParallel() throws InterruptedException {

        final AtomicBoolean failed = new AtomicBoolean(false);
        ArrayList<Thread> threads = new ArrayList<Thread>();

        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        testEphemeralPort();
                    } catch (DeploymentException | IOException | InterruptedException e) {
                        failed.set(true);
                    }
                }
            };
            threads.add(thread);
            thread.start();
        }

        for (Thread t : threads) {
            t.join();
        }
    }

    @ServerEndpoint("/")
    public static class EphemeralPortTestEndpoint {

        @OnMessage
        public String onMessage(String message) {
            return message;
        }
    }
}
