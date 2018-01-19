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

package org.glassfish.tyrus.test.standard_config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.MessageHandler;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.spi.UpgradeRequest;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * See https://java.net/jira/browse/TYRUS-205.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class SubProtocolTest extends TestContainer {

    @ServerEndpoint(value = "/subProtocolTest",
            subprotocols = {"MBLWS.huawei.com", "wamp", "v11.stomp", "v10.stomp", "soap"})
    public static class Endpoint {
        @OnOpen
        public void onOpen(Session s) throws IOException {
            s.getBasicRemote().sendText(s.getNegotiatedSubprotocol());
        }
    }

    @Test
    public void orderingTest() throws DeploymentException {
        Server server = startServer(Endpoint.class);

        try {
            final CountDownLatch messageLatch = new CountDownLatch(1);

            final ClientEndpointConfig clientEndpointConfig =
                    ClientEndpointConfig.Builder.create().preferredSubprotocols(
                            Arrays.asList("MBWS.huawei.com", "soap", "v10.stomp")).build();
            ContainerProvider.getWebSocketContainer().connectToServer(new javax.websocket.Endpoint() {
                @Override
                public void onOpen(final Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {

                            if (message.equals("soap") && session.getNegotiatedSubprotocol().equals("soap")) {
                                messageLatch.countDown();
                            }
                        }
                    });
                }
            }, clientEndpointConfig, getURI(Endpoint.class));

            messageLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testNoIntersection() throws DeploymentException {
        Server server = startServer(Endpoint.class);

        try {
            final CountDownLatch messageLatch = new CountDownLatch(1);

            final ClientEndpointConfig clientEndpointConfig =
                    ClientEndpointConfig.Builder.create()
                                                .configurator(new ClientEndpointConfig.Configurator() {
                                                    @Override
                                                    public void afterResponse(HandshakeResponse hr) {
                                                        final Map<String, List<String>> headers = hr.getHeaders();

                                                        // TYRUS-250: SEC_WEBSOCKET_PROTOCOL cannot be present when
                                                        // there is no negotiated
                                                        //            subprotocol.
                                                        assertNull(headers.get(UpgradeRequest.SEC_WEBSOCKET_PROTOCOL));
                                                    }
                                                }).preferredSubprotocols(Arrays.asList("a", "b", "c")).build();
            ClientManager client = createClient();
            client.connectToServer(new javax.websocket.Endpoint() {
                @Override
                public void onOpen(final Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {

                            if (message.equals("") && session.getNegotiatedSubprotocol().equals("")) {
                                messageLatch.countDown();
                            }
                        }
                    });
                }
            }, clientEndpointConfig, getURI(Endpoint.class));

            messageLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
