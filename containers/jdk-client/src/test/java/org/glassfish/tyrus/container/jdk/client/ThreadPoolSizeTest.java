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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.ThreadPoolConfig;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Tests that the JDK client thread pool is limited properly.
 * <p/>
 * It blocks client thread in @OnMessage and tests that the number of delivered messages equals maximal thread pool
 * size.
 *
 * @author Petr Janouch
 */
public class ThreadPoolSizeTest extends TestContainer {

    @Test
    public void testDefaultMaxThreadPoolSize() {
        ClientManager client = ClientManager.createClient(JdkClientContainer.class.getName());
        /**
         * default defined by {@link org.glassfish.tyrus.client.ThreadPoolConfig}
         */
        int maxThreads = Math.max(20, Runtime.getRuntime().availableProcessors());
        testMaxThreadPoolSize(maxThreads, client);
    }

    @Test
    public void testMaxThreadPoolSize() {
        ClientManager client = ClientManager.createClient(JdkClientContainer.class.getName());
        ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig().setMaxPoolSize(15);
        client.getProperties().put(ClientProperties.WORKER_THREAD_POOL_CONFIG, threadPoolConfig);
        testMaxThreadPoolSize(15, client);
    }

    private void testMaxThreadPoolSize(int maxThreadPoolSize, ClientManager client) {
        Server server = null;
        List<Session> sessions = new ArrayList<>();

        AtomicInteger messagesCounter = new AtomicInteger(0);
        CountDownLatch blockingLatch = new CountDownLatch(1);
        CountDownLatch messagesLatch = new CountDownLatch(maxThreadPoolSize);
        CountDownLatch sessionCloseLatch = new CountDownLatch(maxThreadPoolSize + 10);
        try {
            server = startServer(AnnotatedServerEndpoint.class);

            client.getProperties().put(ClientProperties.SHARED_CONTAINER_IDLE_TIMEOUT, 1);
            BlockingClientEndpoint clientEndpoint =
                    new BlockingClientEndpoint(messagesCounter, messagesLatch, blockingLatch, sessionCloseLatch);

            for (int i = 0; i < maxThreadPoolSize + 10; i++) {
                Session session = client.connectToServer(clientEndpoint, getURI(AnnotatedServerEndpoint.class));
                sessions.add(session);
            }

            for (Session session : sessions) {
                session.getAsyncRemote().sendText("hi");
            }

            // wait for all threads to get blocked
            assertTrue(messagesLatch.await(5, TimeUnit.SECONDS));
            // wait some more time (we test nothing gets delivered in this interval)
            Thread.sleep(1000);
            // assert number of delivered messages is equal to the thread pool size
            assertEquals(maxThreadPoolSize, messagesCounter.get());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            // let the blocked threads go
            blockingLatch.countDown();
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

    @ServerEndpoint("/ThreadPoolTestServerEndpoint")
    public static class AnnotatedServerEndpoint {

        @OnMessage
        public String onMessage(String message) {
            return message;
        }
    }

    @ClientEndpoint
    public static class BlockingClientEndpoint {

        private final AtomicInteger messagesCounter;
        private final CountDownLatch messagesLatch;
        private final CountDownLatch blockingLatch;
        private final CountDownLatch sessionCloseLatch;

        BlockingClientEndpoint(AtomicInteger messagesCounter, CountDownLatch messagesLatch,
                               CountDownLatch blockingLatch, CountDownLatch sessionCloseLatch) {
            this.messagesCounter = messagesCounter;
            this.messagesLatch = messagesLatch;
            this.blockingLatch = blockingLatch;
            this.sessionCloseLatch = sessionCloseLatch;
        }

        @OnMessage
        public void onMessage(String message) throws InterruptedException {

            if (messagesCounter != null) {
                messagesCounter.incrementAndGet();
            }

            if (messagesLatch != null) {
                messagesLatch.countDown();
            }

            blockingLatch.await();
        }

        @OnClose
        public void onClose(Session session) {
            sessionCloseLatch.countDown();
        }
    }
}
