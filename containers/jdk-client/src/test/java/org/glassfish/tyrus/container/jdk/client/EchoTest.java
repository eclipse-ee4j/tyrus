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

package org.glassfish.tyrus.container.jdk.client;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import junit.framework.Assert;

/**
 * @author Petr Janouch
 */
public class EchoTest extends TestContainer {

    @Test
    public void testEcho() throws IOException, DeploymentException, InterruptedException {
        final Server server = startServer(EchoEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);
        final CountDownLatch onOpenLatch = new CountDownLatch(1);
        final CountDownLatch sessionCloseLatch = new CountDownLatch(1);

        try {
            final ClientManager client = ClientManager.createClient(JdkClientContainer.class.getName());
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

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    sessionCloseLatch.countDown();
                }
            }, ClientEndpointConfig.Builder.create().build(), getURI(EchoEndpoint.class));

            assertTrue(messageLatch.await(1, TimeUnit.SECONDS));
            assertTrue(onOpenLatch.await(1, TimeUnit.SECONDS));

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
            try {
                /* Tests in the package are sensitive to freeing resources. Unclosed sessions might hinder the next test
                (if the next test requires a fresh client thread pool) */
                Assert.assertTrue(sessionCloseLatch.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @ServerEndpoint("/echo")
    public static class EchoEndpoint {
        @OnMessage
        public String onMessage(String message) {
            System.out.println("# server echoed: " + message);
            return message + " (from your server)";
        }

        @OnOpen
        public void onOpen(Session session) throws IOException {
            session.getBasicRemote().sendText("onOpen");
        }
    }
}
