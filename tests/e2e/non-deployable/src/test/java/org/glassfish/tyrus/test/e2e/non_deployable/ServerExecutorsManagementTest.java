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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.container.inmemory.InMemoryClientContainer;
import org.glassfish.tyrus.core.TyrusSession;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.server.TyrusServerConfiguration;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests that unmanaged executor services allocated by Grizzly server container get released after the server has been stopped.
 * <p/>
 * Since executor services cannot be obtained directly, server is started and stopped multiple times and it is tested that
 * the number of active threads does not exceed a given limit.
 * </p>
 * This test is not deployable, because the container on Glassfish uses managed executor services and number of threads
 * on a long-time-running Glassfish could exceed the hard limit checked by this test.
 *
 * @author Petr Janouch
 */
public class ServerExecutorsManagementTest extends TestContainer {

    @Test
    public void testExecutorServicesGetClosed() {
        try {
            ClientManager clientManager = createClient();
            for (int i = 0; i < 100; i++) {
                Server server = startServer(BroadcastingEndpoint.class);
                try {
                    final CountDownLatch messageLatch = new CountDownLatch(1);
                    clientManager.connectToServer(new Endpoint() {
                        @Override
                        public void onOpen(Session session, EndpointConfig config) {
                            session.addMessageHandler(new MessageHandler.Whole<String>() {

                                @Override
                                public void onMessage(String message) {
                                    messageLatch.countDown();
                                }
                            });
                        }
                    }, getURI(BroadcastingEndpoint.class));
                    assertTrue(messageLatch.await(5, TimeUnit.SECONDS));


                } finally {
                    stopServer(server);
                }
            }

            int activeThreadsCount = Thread.activeCount();
            assertTrue("Number of active threads is " + activeThreadsCount, activeThreadsCount < 50);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testInMemoryContainerExecutorServicesGetClosed() {
        try {
            ClientManager clientManager = ClientManager.createClient(InMemoryClientContainer.class.getName());
            ServerApplicationConfig serverConfig = new TyrusServerConfiguration(
                    new HashSet<Class<?>>(Arrays.<Class<?>>asList(BroadcastingEndpoint.class)),
                    Collections.<ServerEndpointConfig>emptySet());
            ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
            cec.getUserProperties().put(InMemoryClientContainer.SERVER_CONFIG, serverConfig);

            for (int i = 0; i < 100; i++) {
                final CountDownLatch messageLatch = new CountDownLatch(1);
                Session session = clientManager.connectToServer(new Endpoint() {
                    @Override
                    public void onOpen(Session session, EndpointConfig config) {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {

                            @Override
                            public void onMessage(String message) {
                                messageLatch.countDown();
                            }
                        });
                    }
                }, cec, URI.create("ws://inmemory/serverExecutorsManagementEndpoint"));

                assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
                session.close();
            }

            int activeThreadsCount = Thread.activeCount();
            assertTrue("Number of active threads is " + activeThreadsCount, activeThreadsCount < 50);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /* Tyrus broadcast is parallel by default and it uses executor service managed by the server container,
     so using broadcast ensures that executor service on server is invoked */
    @ServerEndpoint("/serverExecutorsManagementEndpoint")
    public static class BroadcastingEndpoint {

        @OnOpen
        public void onOpen(Session session) {
            ((TyrusSession) session).broadcast("Hi");
        }
    }
}
