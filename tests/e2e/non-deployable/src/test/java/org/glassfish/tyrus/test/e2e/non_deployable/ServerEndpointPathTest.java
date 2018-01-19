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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Cannot be moved to standard tests due the expected deployment exception.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ServerEndpointPathTest extends TestContainer {

    private volatile String receivedMessage;

    @ServerEndpoint("/{a}")
    public static class WSL1ParamServer {

        @OnMessage
        public String echo(@PathParam("a") String param, String echo) {
            return echo + param + getClass().getName();
        }
    }

    @ServerEndpoint("/a")
    public static class WSL1ExactServer {

        @OnMessage
        public String echo(String echo) {
            return echo + getClass().getName();
        }
    }

    @Test
    public void testExactMatching() throws DeploymentException {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        Server server = startServer(WSL1ExactServer.class, WSL1ParamServer.class);

        try {
            final CountDownLatch messageLatch = new CountDownLatch(1);

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                receivedMessage = message;
                                messageLatch.countDown();
                            }
                        });
                        session.getBasicRemote().sendText("Client says hello");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI("/a"));

            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals("Client says hello" + WSL1ExactServer.class.getName(), receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint("/{samePath}")
    public static class AEndpoint {
    }

    @Test(expected = DeploymentException.class)
    public void testEquivalentPaths() throws DeploymentException {
        Server server = new Server(WSL1ParamServer.class, AEndpoint.class);
        server.start();
    }
}
