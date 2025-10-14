/*
 * Copyright (c) 2025 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.test.e2e.jdk21;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class VirtualThreadTestTest extends TestContainer {

    @ServerEndpoint("/virtual")
    public static class VirtualThreadTesterEndpoint {

        @OnMessage
        public Boolean onMessage(Session session, String message) throws ExecutionException, InterruptedException {
            ExecutorService executorService = ((TyrusServerContainer) session.getContainer()).getExecutorService();
            Future<Boolean> future = executorService.submit(() -> {
                return Thread.currentThread().isVirtual();
            });
            return future.get().booleanValue();
        }
    }

    @Test
    public void virtualThreadTest() throws DeploymentException, InterruptedException, IOException {
        getServerProperties().put(TyrusWebSocketEngine.USE_VIRTUAL_THREADS, true);
        final Server server = startServer(VirtualThreadTesterEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);
        final CountDownLatch onCloseLatch = new CountDownLatch(1);
        final AtomicBoolean response = new AtomicBoolean(false);

        try {
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(Boolean.class, message -> {
                            response.set(message);
                            messageLatch.countDown();
                        });
                        session.getBasicRemote().sendText("Hi");
                    } catch (IOException e) {
                        // do nothing
                    }
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    onCloseLatch.countDown();
                }

            }, ClientEndpointConfig.Builder.create().build(), getURI(VirtualThreadTesterEndpoint.class));
            assertTrue(messageLatch.await(1, TimeUnit.SECONDS));
            Assert.assertEquals(true, response.get());

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void virtualThreadClientTest() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(VirtualThreadTesterEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);
        final CountDownLatch onCloseLatch = new CountDownLatch(1);
        final AtomicBoolean response = new AtomicBoolean(false);

        try {
            final ClientManager client = createClient();
            client.getProperties().put(TyrusWebSocketEngine.USE_VIRTUAL_THREADS, true);
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    session.getContainer();
                    try {
                        response.set(((ClientManager) session.getContainer()).getExecutorService().submit(
                                () -> Thread.currentThread().isVirtual()).get()
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    messageLatch.countDown();
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    onCloseLatch.countDown();
                }

            }, ClientEndpointConfig.Builder.create().build(), getURI(VirtualThreadTesterEndpoint.class));
            assertTrue(messageLatch.await(1, TimeUnit.SECONDS));
            Assert.assertEquals(true, response.get());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

}
