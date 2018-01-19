/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientContainer;
import org.glassfish.tyrus.container.grizzly.client.GrizzlyClientProperties;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.fail;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class GrizzlyClientThreadPoolConfigsTest extends TestContainer {

    @Test
    public void testCustomThreadFactories() throws DeploymentException {
        /*
            Also setting client.getProperties().put(ClientProperties.SHARED_CONTAINER, ... ) is supported - if a test
             running
            before this test does that, this test might fail.
         */
        if (System.getProperties().getProperty(ClientProperties.SHARED_CONTAINER) != null) {
            // test not valid with shared container.
            return;
        }

        Server server = startServer(EchoEndpoint.class);

        try {
            server.start();
            final CountDownLatch workerThreadLatch = new CountDownLatch(1);
            final CountDownLatch selectorThreadLatch = new CountDownLatch(1);
            final CountDownLatch messageLatch = new CountDownLatch(1);

            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = ClientManager.createClient(GrizzlyClientContainer.class.getName());

            client.getProperties().put(
                    GrizzlyClientProperties.WORKER_THREAD_POOL_CONFIG,
                    ThreadPoolConfig.defaultConfig().setThreadFactory(new ThreadFactory() {
                        @Override
                        public Thread newThread(Runnable r) {
                            Logger.getLogger(GrizzlyClientThreadPoolConfigsTest.class.getName())
                                  .log(Level.INFO, "Worker thread factory called: " + r);
                            workerThreadLatch.countDown();
                            return new Thread(r);
                        }
                    }));

            client.getProperties().put(
                    GrizzlyClientProperties.SELECTOR_THREAD_POOL_CONFIG,
                    ThreadPoolConfig.defaultConfig().setThreadFactory(new ThreadFactory() {
                        @Override
                        public Thread newThread(Runnable r) {
                            Logger.getLogger(GrizzlyClientThreadPoolConfigsTest.class.getName())
                                  .log(Level.INFO, "Selector thread factory called: " + r);
                            selectorThreadLatch.countDown();
                            return new Thread(r);
                        }
                    }));


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
                        session.getBasicRemote().sendText("test");
                    } catch (IOException e) {
                        fail();
                    }
                }
            }, cec, getURI(EchoEndpoint.class));

            messageLatch.await(5, TimeUnit.SECONDS);

            Assert.assertEquals(0, messageLatch.getCount());
            Assert.assertEquals(0, workerThreadLatch.getCount());
            Assert.assertEquals(0, selectorThreadLatch.getCount());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            server.stop();
        }
    }

    /**
     * Bean for basic echo test.
     *
     * @author Stepan Kopriva (stepan.kopriva at oracle.com)
     */
    @ServerEndpoint(value = "/echoendpoint")
    public static class EchoEndpoint {

        @OnMessage
        public String doThat(String message, Session session) {

            // TYRUS-141
            if (session.getNegotiatedSubprotocol() != null) {
                return message;
            }

            return null;
        }
    }
}
