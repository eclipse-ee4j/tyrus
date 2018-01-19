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

package org.glassfish.tyrus.tests.servlet.maxsessions;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.TyrusServerEndpointConfig;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.server.TyrusServerConfiguration;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import junit.framework.Assert;

/**
 * Tests configuration and implementation of maximal number of open sessions.
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
public class MaxSessionsAppTest extends TestContainer {

    private static final String CONTEXT_PATH = "/max-sessions-per-app-test";

    private static final int NUMBER_OF_ENDPOINTS = 3;
    private static final int NUMBER_OF_CLIENTS_OVER_LIMIT = 3;
    private static final int NUMBER_OF_CLIENT_SESSIONS = MaxSessionPerAppApplicationConfig.MAX_SESSIONS_PER_APP
            + NUMBER_OF_CLIENTS_OVER_LIMIT;

    public MaxSessionsAppTest() {
        setContextPath(CONTEXT_PATH);
    }

    public static class ServerDeployApplicationConfig extends TyrusServerConfiguration {
        public ServerDeployApplicationConfig() {
            super(new HashSet<Class<?>>() {
                {
                    add(ServiceEndpoint.class);
                }
            }, new HashSet<ServerEndpointConfig>() {
                {
                    for (final String path : MaxSessionPerAppApplicationConfig.PATHS) {
                        add(TyrusServerEndpointConfig.Builder.create(EchoEndpoint.class, path).build());
                    }
                }
            });
        }
    }

    public void maxSessions() throws DeploymentException, InterruptedException, IOException {
        final ClientManager client = createClient();

        final CountDownLatch closeNormalLatch = new CountDownLatch(MaxSessionPerAppApplicationConfig
                                                                           .MAX_SESSIONS_PER_APP);
        final CountDownLatch closeOverLimitLatch = new CountDownLatch(NUMBER_OF_CLIENTS_OVER_LIMIT);

        final Session[] sessions = new Session[NUMBER_OF_CLIENT_SESSIONS];

        for (int i = 0; i < NUMBER_OF_CLIENT_SESSIONS; i++) {
            URI uri = getURI("/echo" + ((i % NUMBER_OF_ENDPOINTS) + 1));
            System.out.println(uri);
            sessions[i] = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            System.out.println(message);
                        }
                    });
                    try {
                        session.getBasicRemote().sendText("Do or do not, there is no try.");
                    } catch (IOException e) {
                        // session can be closed by server
                    }
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    System.out.println("Client-side close reason: " + closeReason);
                    if (closeReason.getCloseCode().getCode() == CloseReason.CloseCodes.NORMAL_CLOSURE.getCode()) {
                        closeNormalLatch.countDown();
                    } else if (closeReason.getCloseCode().getCode()
                            == CloseReason.CloseCodes.TRY_AGAIN_LATER.getCode()) {
                        closeOverLimitLatch.countDown();
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(), uri);
        }

        assertTrue(String.format("Session should be closed just %d times with close code 1013 - Try Again Later",
                                 NUMBER_OF_CLIENTS_OVER_LIMIT), closeOverLimitLatch.await(3, TimeUnit.SECONDS));

        for (int i = 0; i < NUMBER_OF_CLIENT_SESSIONS; i++) {
            System.out.printf("session[%d] is open? %s\n", i, sessions[i].isOpen());
            if (i < MaxSessionPerAppApplicationConfig.MAX_SESSIONS_PER_APP) {
                assertTrue("Session in limit is closed!", sessions[i].isOpen());
                sessions[i].close();
            } else {
                assertFalse("Session over limit should be closed!", sessions[i].isOpen());
            }
        }

        Assert.assertTrue(
                "Number of normal closures should be the same as the limit!",
                closeNormalLatch.await(1, TimeUnit.SECONDS));

        final CountDownLatch messageReceivedLatch = new CountDownLatch(1);
        Session session = client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig EndpointConfig) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String message) {
                        messageReceivedLatch.countDown();
                    }
                });
                try {
                    session.getBasicRemote().sendText("New session is available after close all of the opened "
                                                              + "sessions.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }, ClientEndpointConfig.Builder.create().build(), getURI("/echo1"));

        assertTrue(messageReceivedLatch.await(1, TimeUnit.SECONDS));

        session.close();

        testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "/echo");
        testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "reset");

        Thread.sleep(100);
    }

    @Test
    public void maxSessionsSingle() throws DeploymentException, InterruptedException, IOException {
        getServerProperties().put(TyrusWebSocketEngine.MAX_SESSIONS_PER_APP,
                                  MaxSessionPerAppApplicationConfig.MAX_SESSIONS_PER_APP);

        Server server = startServer(ServerDeployApplicationConfig.class);
        try {
            maxSessions();
            System.out.println("==================");
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void maxSessionsDurable() throws DeploymentException, InterruptedException, IOException {
        getServerProperties().put(TyrusWebSocketEngine.MAX_SESSIONS_PER_APP,
                                  MaxSessionPerAppApplicationConfig.MAX_SESSIONS_PER_APP);

        Server server = startServer(ServerDeployApplicationConfig.class);
        try {
            for (int i = 0; i < 10; i++) {
                maxSessions();
                System.out.println("==================");
            }
        } finally {
            stopServer(server);
        }
    }
}
