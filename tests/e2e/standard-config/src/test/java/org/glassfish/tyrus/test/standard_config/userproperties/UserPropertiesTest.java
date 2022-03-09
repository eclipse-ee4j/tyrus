/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.test.standard_config.userproperties;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class UserPropertiesTest extends TestContainer {
    @Test
    public void testUserProperties() throws DeploymentException {
        UserPropertiesServerEndpointConfig.beforeHandShake(); //reset
        UserPropertiesServerEndpointConfig.AI.set(0);
        setServerProperties(new HashMap<>());

        Server server = startServer(UserPropertiesApplication.class);
        try {
            testOnce(1);
            testOnce(2);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }

        Assert.assertEquals(0, UserPropertiesServerEndpointConfig.AI.get());
    }

    @Test
    public void testConcurrencyProperties() throws DeploymentException {
        Server server = startServer(UserPropertiesApplication.class);
        Thread[] threads = new Thread[2];
        try {
            for (int i = 0; i != 2; i++) {
                final int cnt = i + 1;
                threads[i] = new Thread(() -> {
                    try {
                        testOnceAsync(cnt);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                threads[i].start();
            }
            for (int i = 0; i != 2; i++) {
                threads[i].join();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    private void testOnce(int cnt) throws DeploymentException, IOException, InterruptedException {
        AtomicReference<String> receivedTestMessage = new AtomicReference<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        ClientManager client = createClient();
        client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig config) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String message) {
                        receivedTestMessage.set(message);
                        messageLatch.countDown();
                    }
                });
                sendAnything(session, cnt);
            }
        }, getURI(new UserPropertiesServerEndpointConfig().getPath() + "?cnt=" + cnt));
        messageLatch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals("PASS", receivedTestMessage.get());
    }

    private void testOnceAsync(int cnt) throws DeploymentException, IOException, InterruptedException {
        AtomicReference<String> receivedTestMessage = new AtomicReference<>();
        CountDownLatch messageLatch = new CountDownLatch(1);

        ClientManager client = createClient();
        client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig config) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String message) {
                        receivedTestMessage.set(message);
                        messageLatch.countDown();
                    }
                });
                sendAnything(session, cnt);
            }
        }, ClientEndpointConfig.Builder.create().configurator(new ClientEndpointConfig.Configurator() {
                    @Override
                    public void beforeRequest(Map<String, List<String>> headers) {
                        headers.put(ConcurrencyUserPropertiesServer.KEY, Collections.singletonList(String.valueOf(cnt)));
                    }
           }).build(),
           getURI(new ConcurrencyUserPropertiesConfig().getPath() + "?cnt=" + cnt)
        );
        messageLatch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals("OK", receivedTestMessage.get());
    }

    private static void sendAnything(Session session, int cnt) {
        try {
            session.getBasicRemote().sendText("ANYTHING" + cnt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
