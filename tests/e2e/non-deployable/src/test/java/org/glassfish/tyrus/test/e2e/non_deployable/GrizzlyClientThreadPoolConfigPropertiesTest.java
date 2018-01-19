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

package org.glassfish.tyrus.test.e2e.non_deployable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientProperties;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test that both {@link GrizzlyClientProperties} and {@link ClientProperties} are supported when configuring Grizzly
 * client worker thread pool.
 *
 * @author Petr Janouch
 */
public class GrizzlyClientThreadPoolConfigPropertiesTest extends TestContainer {

    /**
     * Test that {@link GrizzlyClientProperties#WORKER_THREAD_POOL_CONFIG} is supported with {@link
     * org.glassfish.grizzly.threadpool.ThreadPoolConfig}.
     */
    @Test
    public void testGrizzlyThreadPoolConfigGrizzlyProperties() {
        testThreadPoolConfigProperties(GrizzlyClientProperties.WORKER_THREAD_POOL_CONFIG, true);
    }

    /**
     * Test that {@link ClientProperties#WORKER_THREAD_POOL_CONFIG} is supported with {@link
     * org.glassfish.grizzly.threadpool.ThreadPoolConfig}.
     */
    @Test
    public void testGrizzlyThreadPoolConfigClientProperties() {
        testThreadPoolConfigProperties(ClientProperties.WORKER_THREAD_POOL_CONFIG, true);
    }

    /**
     * Test that {@link ClientProperties#WORKER_THREAD_POOL_CONFIG} is supported with {@link
     * org.glassfish.tyrus.client.ThreadPoolConfig}.
     */
    @Test
    public void testTyrusThreadPoolConfigClientProperties() {
        testThreadPoolConfigProperties(ClientProperties.WORKER_THREAD_POOL_CONFIG, false);
    }

    private void testThreadPoolConfigProperties(String workerThreadPoolProperty, boolean useGrizzlyConfig) {
        /*
            Also setting client.getProperties().put(ClientProperties.SHARED_CONTAINER, ... ) is supported - if a test
             running
            before this test does that, this test might fail.
         */
        if (System.getProperties().getProperty(ClientProperties.SHARED_CONTAINER) != null) {
            // test not valid with shared container.
            return;
        }

        Server server = null;
        try {
            final CountDownLatch workerPoolLatch = new CountDownLatch(1);

            server = startServer(AnnotatedServerEndpoint.class);
            ClientManager client = ClientManager.createClient();

            if (useGrizzlyConfig) {
                ThreadPoolConfig workerThreadPoolConfig =
                        ThreadPoolConfig.defaultConfig().setThreadFactory(new ThreadFactory() {

                            @Override
                            public Thread newThread(Runnable r) {
                                workerPoolLatch.countDown();
                                return new Thread(r);
                            }
                        });

                client.getProperties().put(workerThreadPoolProperty, workerThreadPoolConfig);
            } else {
                org.glassfish.tyrus.client.ThreadPoolConfig workerThreadPoolConfig = org.glassfish.tyrus.client
                        .ThreadPoolConfig.defaultConfig().setThreadFactory(new ThreadFactory() {

                            @Override
                            public Thread newThread(Runnable r) {
                                workerPoolLatch.countDown();
                                return new Thread(r);
                            }
                        });

                client.getProperties().put(workerThreadPoolProperty, workerThreadPoolConfig);
            }

            client.connectToServer(AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class));

            assertTrue(workerPoolLatch.await(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint("/threadPoolConfigEchoEndpoint")
    private static class AnnotatedServerEndpoint {
    }

    @ClientEndpoint
    private static class AnnotatedClientEndpoint {
    }
}
