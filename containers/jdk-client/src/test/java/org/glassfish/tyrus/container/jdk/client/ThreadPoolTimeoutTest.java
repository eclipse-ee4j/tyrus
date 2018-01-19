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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnClose;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.ThreadPoolConfig;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test thread pool timeout and situation when the thread pool is idle, timeout is running and a client with different
 * thread pool config or timeout setting is initiated. In such cases, the idle thread pool should be terminated
 * and a new one created with the new settings.
 *
 * @author Petr Janouch
 */
public class ThreadPoolTimeoutTest extends TestContainer {

    /**
     * Test that client thread pool is destroyed after close timeout elapses. The opposite - thread pool gets reused
     * if a new client is initiated before the timeout elapses is tested by {@link #testSameConfig()}.
     */
    @Test
    public void testThreadPoolTimeout() {
        Server server = null;
        try {
            server = startServer(AnnotatedServerEndpoint.class);

            ClientManager client1 = ClientManager.createClient(JdkClientContainer.class.getName());
            final AtomicBoolean factory1Used = new AtomicBoolean(false);
            ThreadPoolConfig threadPoolConfig1 =
                    ThreadPoolConfig.defaultConfig()
                                    .setPoolName("testThreadPoolTimeout")
                                    .setThreadFactory(new ThreadFactory() {
                                        @Override
                                        public Thread newThread(Runnable r) {
                                            factory1Used.set(true);
                                            return new Thread(r);
                                        }

                                        // this will decide if the two ThreadPoolConfigs
                                        // equal or not
                                        @Override
                                        public boolean equals(Object obj) {
                                            return true;
                                        }
                                    });
            client1.getProperties().put(ClientProperties.WORKER_THREAD_POOL_CONFIG, threadPoolConfig1);
            client1.getProperties().put(ClientProperties.SHARED_CONTAINER_IDLE_TIMEOUT, 1);

            CountDownLatch sessionCloseLatch = new CountDownLatch(1);
            client1.connectToServer(new AnnotatedClientEndpoint(sessionCloseLatch),
                                    getURI(AnnotatedServerEndpoint.class));
            assertTrue(factory1Used.get());
            terminateServer(server, sessionCloseLatch);

            // wait for the thread pool to timeout and be closed
            Thread.sleep(3000);

            server = startServer(AnnotatedServerEndpoint.class);

            ClientManager client2 = ClientManager.createClient(JdkClientContainer.class.getName());
            final AtomicBoolean factory2Used = new AtomicBoolean(false);
            ThreadPoolConfig threadPoolConfig2 = ThreadPoolConfig.defaultConfig()
                                                                 .setPoolName("testThreadPoolTimeout")
                                                                 .setThreadFactory(new ThreadFactory() {
                                                                     @Override
                                                                     public Thread newThread(Runnable r) {
                                                                         factory2Used.set(true);
                                                                         return new Thread(r);
                                                                     }

                                                                     // this will decide if the two ThreadPoolConfigs
                                                                     // equal or not
                                                                     @Override
                                                                     public boolean equals(Object obj) {
                                                                         return true;
                                                                     }
                                                                 });
            client2.getProperties().put(ClientProperties.WORKER_THREAD_POOL_CONFIG, threadPoolConfig2);
            client2.getProperties().put(ClientProperties.SHARED_CONTAINER_IDLE_TIMEOUT, 1);

            sessionCloseLatch = new CountDownLatch(1);
            client2.connectToServer(new AnnotatedClientEndpoint(sessionCloseLatch),
                                    getURI(AnnotatedServerEndpoint.class));
            assertTrue(factory2Used.get());
            terminateServer(server, sessionCloseLatch);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    /**
     * Test that if there is an idle thread pool and a client with the same thread pool config is initiated, the idle
     * thread poll will be reused.
     */
    @Test
    public void testSameConfig() {
        Server server = null;
        try {
            server = startServer(AnnotatedServerEndpoint.class);

            ClientManager client1 = ClientManager.createClient(JdkClientContainer.class.getName());
            final AtomicBoolean factory1Used = new AtomicBoolean(false);
            ThreadPoolConfig threadPoolConfig1 = ThreadPoolConfig.defaultConfig()
                                                                 .setPoolName("testSameConfig")
                                                                 .setThreadFactory(new ThreadFactory() {
                                                                     @Override
                                                                     public Thread newThread(Runnable r) {
                                                                         factory1Used.set(true);
                                                                         return new Thread(r);
                                                                     }

                                                                     // this will decide if the two ThreadPoolConfigs
                                                                     // equal or not
                                                                     @Override
                                                                     public boolean equals(Object obj) {
                                                                         return true;
                                                                     }
                                                                 });
            client1.getProperties().put(ClientProperties.WORKER_THREAD_POOL_CONFIG, threadPoolConfig1);

            CountDownLatch sessionCloseLatch = new CountDownLatch(1);
            client1.connectToServer(new AnnotatedClientEndpoint(sessionCloseLatch),
                                    getURI(AnnotatedServerEndpoint.class));
            assertTrue(factory1Used.get());
            terminateServer(server, sessionCloseLatch);

            server = startServer(AnnotatedServerEndpoint.class);

            ClientManager client2 = ClientManager.createClient(JdkClientContainer.class.getName());
            final AtomicBoolean factory2Used = new AtomicBoolean(false);
            ThreadPoolConfig threadPoolConfig2 = ThreadPoolConfig.defaultConfig()
                                                                 .setPoolName("testSameConfig")
                                                                 .setThreadFactory(new ThreadFactory() {
                                                                     @Override
                                                                     public Thread newThread(Runnable r) {
                                                                         factory2Used.set(true);
                                                                         return new Thread(r);
                                                                     }

                                                                     // this will decide if the two ThreadPoolConfigs
                                                                     // equal or not
                                                                     @Override
                                                                     public boolean equals(Object obj) {
                                                                         return true;
                                                                     }
                                                                 });
            client2.getProperties().put(ClientProperties.WORKER_THREAD_POOL_CONFIG, threadPoolConfig2);

            sessionCloseLatch = new CountDownLatch(1);
            client2.connectToServer(new AnnotatedClientEndpoint(sessionCloseLatch),
                                    getURI(AnnotatedServerEndpoint.class));
            assertFalse(factory2Used.get());
            terminateServer(server, sessionCloseLatch);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    /**
     * Test that if there is an idle thread pool and a client with a different thread pool config is initiated, a new
     * thread pool with the new settings will be crated.
     */
    @Test
    public void testDifferentConfig() {
        Server server = null;
        try {
            server = startServer(AnnotatedServerEndpoint.class);

            ClientManager client1 = ClientManager.createClient(JdkClientContainer.class.getName());
            final AtomicBoolean factory1Used = new AtomicBoolean(false);
            ThreadPoolConfig threadPoolConfig1 = ThreadPoolConfig.defaultConfig().setThreadFactory(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    factory1Used.set(true);
                    return new Thread(r);
                }

                // this will decide if the two ThreadPoolConfigs equal or not
                @Override
                public boolean equals(Object obj) {
                    return false;
                }
            });
            client1.getProperties().put(ClientProperties.WORKER_THREAD_POOL_CONFIG, threadPoolConfig1);

            CountDownLatch sessionCloseLatch = new CountDownLatch(1);
            client1.connectToServer(new AnnotatedClientEndpoint(sessionCloseLatch),
                                    getURI(AnnotatedServerEndpoint.class));
            assertTrue(factory1Used.get());
            terminateServer(server, sessionCloseLatch);

            server = startServer(AnnotatedServerEndpoint.class);

            ClientManager client2 = ClientManager.createClient(JdkClientContainer.class.getName());
            final AtomicBoolean factory2Used = new AtomicBoolean(false);
            ThreadPoolConfig threadPoolConfig2 = ThreadPoolConfig.defaultConfig().setThreadFactory(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    factory2Used.set(true);
                    return new Thread(r);
                }

                // this will decide if the two ThreadPoolConfigs equal or not
                @Override
                public boolean equals(Object obj) {
                    return false;
                }
            });
            client2.getProperties().put(ClientProperties.WORKER_THREAD_POOL_CONFIG, threadPoolConfig2);

            sessionCloseLatch = new CountDownLatch(1);
            client2.connectToServer(new AnnotatedClientEndpoint(sessionCloseLatch),
                                    getURI(AnnotatedServerEndpoint.class));
            assertTrue(factory2Used.get());
            terminateServer(server, sessionCloseLatch);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    /**
     * Test that if there is an idle thread pool and a client with the same thread pool config, but different thread
     * pool timeout is initiated, a new thread pool with the new timeout will be initiated.
     */
    @Test
    public void testDifferentThreadPoolTimeout() {
        Server server = null;
        try {
            server = startServer(AnnotatedServerEndpoint.class);

            ClientManager client1 = ClientManager.createClient(JdkClientContainer.class.getName());
            final AtomicBoolean factory1Used = new AtomicBoolean(false);
            ThreadPoolConfig threadPoolConfig1 =
                    ThreadPoolConfig.defaultConfig()
                                    .setPoolName("testDifferentThreadPoolTimeout")
                                    .setThreadFactory(new ThreadFactory() {
                                        @Override
                                        public Thread newThread(Runnable r) {
                                            factory1Used.set(true);
                                            return new Thread(r);
                                        }

                                        // this will decide if the two ThreadPoolConfigs
                                        // equal or not
                                        @Override
                                        public boolean equals(Object obj) {
                                            return true;
                                        }
                                    });
            client1.getProperties().put(ClientProperties.WORKER_THREAD_POOL_CONFIG, threadPoolConfig1);
            client1.getProperties().put(ClientProperties.SHARED_CONTAINER_IDLE_TIMEOUT, 1);

            CountDownLatch sessionCloseLatch = new CountDownLatch(1);
            client1.connectToServer(new AnnotatedClientEndpoint(sessionCloseLatch),
                                    getURI(AnnotatedServerEndpoint.class));
            assertTrue(factory1Used.get());
            terminateServer(server, sessionCloseLatch);

            server = startServer(AnnotatedServerEndpoint.class);
            ClientManager client2 = ClientManager.createClient(JdkClientContainer.class.getName());
            final AtomicBoolean factory2Used = new AtomicBoolean(false);
            ThreadPoolConfig threadPoolConfig2 =
                    ThreadPoolConfig.defaultConfig()
                                    .setPoolName("testDifferentThreadPoolTimeout")
                                    .setThreadFactory(new ThreadFactory() {
                                        @Override
                                        public Thread newThread(Runnable r) {
                                            factory2Used.set(true);
                                            return new Thread(r);
                                        }

                                        // this will decide if the two ThreadPoolConfigs
                                        // equal or not
                                        @Override
                                        public boolean equals(Object obj) {
                                            return true;
                                        }
                                    });
            client2.getProperties().put(ClientProperties.WORKER_THREAD_POOL_CONFIG, threadPoolConfig2);
            client2.getProperties().put(ClientProperties.SHARED_CONTAINER_IDLE_TIMEOUT, 2);

            sessionCloseLatch = new CountDownLatch(1);
            client2.connectToServer(new AnnotatedClientEndpoint(sessionCloseLatch),
                                    getURI(AnnotatedServerEndpoint.class));
            assertTrue(factory2Used.get());
            terminateServer(server, sessionCloseLatch);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    /**
     * Stops server and waits for the connection on the client to be closed.
     */
    private void terminateServer(Server server, CountDownLatch sessionCloseLatch) {
        stopServer(server);
        try {
            /* Tests in the package are sensitive to freeing resources. Unclosed sessions might hinder the next test
                (if the next test requires a fresh client thread pool) */
            assertTrue(sessionCloseLatch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @ServerEndpoint("/threadPoolTimeoutTestEndpoint")
    public static class AnnotatedServerEndpoint {
    }

    @ClientEndpoint
    public static class AnnotatedClientEndpoint {

        private final CountDownLatch sessionCloseLatch;

        AnnotatedClientEndpoint(CountDownLatch sessionCloseLatch) {
            this.sessionCloseLatch = sessionCloseLatch;
        }

        @OnClose
        public void onClose(Session session) {
            sessionCloseLatch.countDown();
        }
    }
}
