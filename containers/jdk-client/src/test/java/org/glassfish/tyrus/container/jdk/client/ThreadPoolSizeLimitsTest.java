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
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.ThreadPoolConfig;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Petr Janouch
 */
public class ThreadPoolSizeLimitsTest extends TestContainer {

    @Test
    public void testBorderValues() {
        ThreadPoolConfig config = ThreadPoolConfig.defaultConfig();
        try {
            config.setCorePoolSize(-1);
            fail();
        } catch (Exception e) {
            // do nothing
        }

        config.setCorePoolSize(0);
        assertEquals(0, config.getCorePoolSize());

        try {
            config.setMaxPoolSize(2);
            fail();
        } catch (Exception e) {
            // do nothing
        }

        config.setMaxPoolSize(3);
        assertEquals(3, config.getMaxPoolSize());

        config.setQueueLimit(-2);
        assertEquals(-1, config.getQueueLimit());

        config.setQueueLimit(0);
        assertEquals(0, config.getQueueLimit());
    }

    /**
     * Tests that client with max two threads in the thread pool will work.
     */
    @Test
    public void testSmallestMaximalSize() {
        final CountDownLatch messageLatch = new CountDownLatch(1);
        final CountDownLatch sessionCloseLatch = new CountDownLatch(1);

        Server server = null;
        try {
            server = startServer(AnnotatedServerEndpoint.class);


            ClientManager client = ClientManager.createClient(JdkClientContainer.class.getName());
            ThreadPoolConfig config = ThreadPoolConfig.defaultConfig().setMaxPoolSize(3);
            client.getProperties().put(ClientProperties.WORKER_THREAD_POOL_CONFIG, config);

            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {

                        @Override
                        public void onMessage(String message) {
                            messageLatch.countDown();
                        }
                    });

                    try {
                        session.getBasicRemote().sendText("Hi");
                    } catch (IOException e) {
                        e.printStackTrace();
                        fail();
                    }
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    sessionCloseLatch.countDown();
                }
            }, ClientEndpointConfig.Builder.create().build(), getURI(AnnotatedServerEndpoint.class));

            assertTrue(messageLatch.await(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
            try {
                /* Tests in the package are sensitive to freeing resources. Unclosed sessions might hinder the next test
                (if the next test requires a fresh client thread pool) */
                assertTrue(sessionCloseLatch.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @ServerEndpoint("/threadPoolLimitsEndpoint")
    public static class AnnotatedServerEndpoint {

        @OnMessage
        public String onMessage(String message) {
            return message;
        }
    }
}
