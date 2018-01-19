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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

import junit.framework.Assert;
import static junit.framework.Assert.fail;

/**
 * When the capacity of a thread pool has been exhausted, tasks get queued. This tests test that enqueued tasks get
 * executed when threads are not busy anymore.
 * <p/>
 * The default queue and a queue provided by the user is tested.
 *
 * @author Petr Janouch
 */
public class EnqueuedTasksTest extends TestContainer {

    @Test
    public void testUserProvidedQueue() {
        ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig();
        CountDownLatch enqueueLatch = new CountDownLatch(10);
        threadPoolConfig.setQueue(new CountingQueue(enqueueLatch));
        testEnqueuedTasksGetExecuted(threadPoolConfig, enqueueLatch);
    }

    @Test
    public void testDefaultQueue() {
        ThreadPoolConfig threadPoolConfig = ThreadPoolConfig.defaultConfig();
        testEnqueuedTasksGetExecuted(threadPoolConfig, null);
    }

    private void testEnqueuedTasksGetExecuted(ThreadPoolConfig threadPoolConfig, CountDownLatch enqueueLatch) {
        CountDownLatch blockingLatch = new CountDownLatch(1);
        CountDownLatch totalMessagesLatch = new CountDownLatch(20);
        CountDownLatch sessionCloseLatch = new CountDownLatch(20);
        Server server = null;
        try {
            server = startServer(AnnotatedServerEndpoint.class);

            ClientManager client = ClientManager.createClient(JdkClientContainer.class.getName());
            threadPoolConfig.setMaxPoolSize(10);
            client.getProperties().put(ClientProperties.WORKER_THREAD_POOL_CONFIG, threadPoolConfig);
            client.getProperties().put(ClientProperties.SHARED_CONTAINER_IDLE_TIMEOUT, 1);

            List<Session> sessions = new ArrayList<>();

            BlockingClientEndpoint clientEndpoint =
                    new BlockingClientEndpoint(blockingLatch, totalMessagesLatch, sessionCloseLatch);

            for (int i = 0; i < 20; i++) {
                Session session = client.connectToServer(clientEndpoint, getURI(AnnotatedServerEndpoint.class));
                sessions.add(session);
            }

            for (Session session : sessions) {
                session.getAsyncRemote().sendText("hi");
            }

            if (enqueueLatch == null) {
                // if latch counting enqueued tasks is not present (case when using default queue), just wait some time
                Thread.sleep(2000);
            } else {
                // 10 tasks got enqueued
                assertTrue(enqueueLatch.await(5, TimeUnit.SECONDS));
            }
            // let the blocked threads go
            blockingLatch.countDown();
            // check everything got delivered
            assertTrue(totalMessagesLatch.await(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            // just to be sure there are no blocked threads left.
            blockingLatch.countDown();
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

    @ServerEndpoint("/ThreadPoolTestServerEndpoint")
    public static class AnnotatedServerEndpoint {

        @OnMessage
        public String onMessage(String message) {
            return message;
        }
    }

    @ClientEndpoint
    public static class BlockingClientEndpoint {

        private final CountDownLatch blockingLatch;
        private final CountDownLatch totalMessagesLatch;
        private final CountDownLatch sessionCloseLatch;

        BlockingClientEndpoint(CountDownLatch blockingLatch, CountDownLatch totalMessagesLatch,
                               CountDownLatch sessionCloseLatch) {
            this.blockingLatch = blockingLatch;
            this.totalMessagesLatch = totalMessagesLatch;
            this.sessionCloseLatch = sessionCloseLatch;
        }

        @OnMessage
        public void onMessage(String message) throws InterruptedException {
            blockingLatch.await();

            if (totalMessagesLatch != null) {
                totalMessagesLatch.countDown();
            }
        }

        @OnClose
        public void onClose(Session session) {
            sessionCloseLatch.countDown();
        }
    }

    /**
     * A wrapper of {@link java.util.LinkedList} that counts enqueued elements.
     */
    private static class CountingQueue extends LinkedList<Runnable> implements Queue<Runnable> {

        private static final long serialVersionUID = -1356740236369553900L;
        private final CountDownLatch enqueueLatch;

        CountingQueue(CountDownLatch enqueueLatch) {
            this.enqueueLatch = enqueueLatch;
        }

        @Override
        public boolean offer(Runnable runnable) {
            enqueueLatch.countDown();
            return super.offer(runnable);
        }
    }
}
