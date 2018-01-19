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

package org.glassfish.tyrus.test.standard_config;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.ThreadPoolConfig;
import org.glassfish.tyrus.core.TyrusSession;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Petr Janouch
 */
public class ParallelBroadcastTest extends TestContainer {

    /**
     * Number of created sessions.
     */
    public static final int SESSIONS_COUNT = 1000;
    /**
     * Number of threads used for executing the broadcast.
     */
    public static final int THREAD_COUNT = 7;

    /**
     * Test broadcasting by iterating over all sessions.
     */
    @Test
    public void testParallelBroadcast() {
        Server server = null;
        AtomicInteger messageCounter = new AtomicInteger(0);
        CountDownLatch messageLatch = new CountDownLatch(SESSIONS_COUNT);
        try {
            server = startServer(BroadcastServerEndpoint.class);
            ClientManager client = createClient();
            client.getProperties().put(ClientProperties.SHARED_CONTAINER, true);

            // The number of threads has to be limited, because all the clients will receive the broadcast
            // simultaneously, which might lead to creating too many threads and consequently a test failure.
            client.getProperties()
                  .put(ClientProperties.WORKER_THREAD_POOL_CONFIG, ThreadPoolConfig.defaultConfig().setMaxPoolSize(20));

            for (int i = 0; i < SESSIONS_COUNT - 1; i++) {
                client.connectToServer(new TextClientEndpoint(messageLatch, messageCounter),
                                       getURI(BroadcastServerEndpoint.class));
            }

            Session session = client.connectToServer(new TextClientEndpoint(messageLatch, messageCounter),
                                                     getURI(BroadcastServerEndpoint.class));
            session.getBasicRemote().sendText("Broadcast request");

            assertTrue(messageLatch.await(30, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            System.out.println("Received messages: " + messageCounter + "/" + SESSIONS_COUNT);
            stopServer(server);
        }
    }

    /**
     * Test Tyrus text broadcast, which is parallel by default.
     * <p/>
     * The number of threads used for the broadcast is {@code Math.min(Runtime.getRuntime().availableProcessors(),
     * sessions.size() / 16)}.
     */
    @Test
    public void testTyrusParallelTextBroadcast() {
        getServerProperties().put(TyrusWebSocketEngine.PARALLEL_BROADCAST_ENABLED, true);
        testTyrusTextBroadcast();
    }

    /**
     * Test Tyrus binary broadcast, which is parallel by default.
     * <p/>
     * The number of threads used for the broadcast is {@code Math.min(Runtime.getRuntime().availableProcessors(),
     * sessions.size() / 16)}.
     */
    @Test
    public void testTyrusParallelBinaryBroadcast() {
        getServerProperties().put(TyrusWebSocketEngine.PARALLEL_BROADCAST_ENABLED, true);
        testTyrusBinaryBroadcast();
    }

    /**
     * Test Tyrus text broadcast with parallel execution being disabled.
     * <p/>
     * The parallel broadcast is disabled only on Grizzly server.
     */
    @Test
    public void testTyrusNonParallelTextBroadcast() {
        // TODO: test on glassfish (servlet tests)
        if (System.getProperty("tyrus.test.host") != null) {
            return;
        }

        getServerProperties().put(TyrusWebSocketEngine.PARALLEL_BROADCAST_ENABLED, false);
        testTyrusTextBroadcast();
    }

    /**
     * Test Tyrus binary broadcast with parallel execution being disabled.
     * <p/>
     * The parallel broadcast is disabled only on Grizzly server.
     */
    @Test
    public void testTyrusNonParallelBinaryBroadcast() {
        // TODO: test on glassfish (servlet tests)
        if (System.getProperty("tyrus.test.host") != null) {
            return;
        }

        getServerProperties().put(TyrusWebSocketEngine.PARALLEL_BROADCAST_ENABLED, false);
        testTyrusBinaryBroadcast();
    }

    private void testTyrusTextBroadcast() {
        Server server = null;
        AtomicInteger messageCounter = new AtomicInteger(0);
        CountDownLatch messageLatch = new CountDownLatch(SESSIONS_COUNT);
        try {
            server = startServer(TyrusTextBroadcastServerEndpoint.class);
            ClientManager client = createClient();
            client.getProperties().put(ClientProperties.SHARED_CONTAINER, true);

            // The number of threads has to be limited, because all the clients will receive the broadcast
            // simultaneously, which might lead to creating too many threads and consequently a test failure.
            client.getProperties()
                  .put(ClientProperties.WORKER_THREAD_POOL_CONFIG, ThreadPoolConfig.defaultConfig().setMaxPoolSize(20));

            for (int i = 0; i < SESSIONS_COUNT - 1; i++) {
                client.connectToServer(new TextClientEndpoint(messageLatch, messageCounter),
                                       getURI(TyrusTextBroadcastServerEndpoint.class));
            }

            Session session = client.connectToServer(new TextClientEndpoint(messageLatch, messageCounter),
                                                     getURI(TyrusTextBroadcastServerEndpoint.class));
            session.getBasicRemote().sendText("Broadcast request");

            assertTrue(messageLatch.await(30, TimeUnit.SECONDS));

            Thread.sleep(2000);

            assertEquals(SESSIONS_COUNT, messageCounter.get());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            System.out.println("Received messages: " + messageCounter + "/" + SESSIONS_COUNT);
            stopServer(server);
        }
    }

    private void testTyrusBinaryBroadcast() {
        Server server = null;
        AtomicInteger messageCounter = new AtomicInteger(0);
        CountDownLatch messageLatch = new CountDownLatch(SESSIONS_COUNT);
        try {
            server = startServer(TyrusBinaryBroadcastServerEndpoint.class);
            ClientManager client = createClient();
            client.getProperties().put(ClientProperties.SHARED_CONTAINER, true);

            // The number of threads has to be limited, because all the clients will receive the broadcast
            // simultaneously, which might lead to creating too many threads and consequently a test failure.
            client.getProperties()
                  .put(ClientProperties.WORKER_THREAD_POOL_CONFIG, ThreadPoolConfig.defaultConfig().setMaxPoolSize(20));

            for (int i = 0; i < SESSIONS_COUNT - 1; i++) {
                client.connectToServer(new BinaryClientEndpoint(messageLatch, messageCounter),
                                       getURI(TyrusBinaryBroadcastServerEndpoint.class));
            }

            Session session = client.connectToServer(new BinaryClientEndpoint(messageLatch, messageCounter),
                                                     getURI(TyrusBinaryBroadcastServerEndpoint.class));
            session.getBasicRemote().sendText("Broadcast request");

            assertTrue(messageLatch.await(30, TimeUnit.SECONDS));

            Thread.sleep(2000);

            assertEquals(SESSIONS_COUNT, messageCounter.get());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            System.out.println("Received messages: " + messageCounter + "/" + SESSIONS_COUNT);
            stopServer(server);
        }
    }

    @ClientEndpoint
    public static class TextClientEndpoint {

        private final CountDownLatch messageLatch;
        private final AtomicInteger messageCounter;

        public TextClientEndpoint(CountDownLatch messageLatch, AtomicInteger messageCounter) {
            this.messageLatch = messageLatch;
            this.messageCounter = messageCounter;
        }

        @OnMessage
        public void onMessage(String message) {
            messageCounter.incrementAndGet();
            messageLatch.countDown();
        }
    }

    @ClientEndpoint
    public static class BinaryClientEndpoint {

        private final CountDownLatch messageLatch;
        private final AtomicInteger messageCounter;

        public BinaryClientEndpoint(CountDownLatch messageLatch, AtomicInteger messageCounter) {
            this.messageLatch = messageLatch;
            this.messageCounter = messageCounter;
        }

        @OnMessage
        public void onMessage(ByteBuffer message) {
            messageCounter.incrementAndGet();
            messageLatch.countDown();
        }
    }

    @ServerEndpoint("/parallelBroadcastEndpoint")
    public static class BroadcastServerEndpoint {

        @OnMessage
        public void onMessage(final Session session, String message) {
            final List<Session> openSessions = new ArrayList<Session>(session.getOpenSessions());
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

            for (int i = 0; i < THREAD_COUNT; i++) {
                final int partitionId = i;
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        int lowerBound = (SESSIONS_COUNT + THREAD_COUNT - 1) / THREAD_COUNT * partitionId;
                        int upperBound =
                                Math.min((SESSIONS_COUNT + THREAD_COUNT - 1) / THREAD_COUNT * (partitionId + 1),
                                         SESSIONS_COUNT);
                        System.out.println(
                                Thread.currentThread().getName() + " <" + lowerBound + ", " + upperBound + ")");

                        for (int j = lowerBound; j < upperBound; j++) {
                            openSessions.get(j).getAsyncRemote().sendText("Hi from " + partitionId);
                        }
                    }
                });
            }
        }
    }

    @ServerEndpoint("/parallelTyrusTextBroadcastEndpoint")
    public static class TyrusTextBroadcastServerEndpoint {

        @OnMessage
        public void onMessage(Session session, String message) {
            ((TyrusSession) session).broadcast("Hi from server");
        }
    }

    @ServerEndpoint("/parallelTyrusBinaryBroadcastEndpoint")
    public static class TyrusBinaryBroadcastServerEndpoint {

        @OnMessage
        public void onMessage(Session session, String message) {
            ((TyrusSession) session).broadcast(ByteBuffer.wrap("Hi from server".getBytes()));
        }
    }
}
