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

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ModifyRequestResponseHeadersTest extends TestContainer {

    private String receivedMessage;

    private static final String SENT_MESSAGE = "Always pass on what you have learned.";
    private static final String HEADER_NAME = "myHeader";
    private static final String[] HEADER_VALUE = {"\"Always two there are, a master and an apprentice.\"", "b", "c"};

    @ServerEndpoint(value = "/echo6", configurator = MyServerConfigurator.class)
    public static class TestEndpoint {

        @OnMessage
        public String onMessage(String message) {
            return message;
        }
    }

    public static class MyServerConfigurator extends ServerEndpointConfig.Configurator {

        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            final List<String> list = request.getHeaders().get(HEADER_NAME);

            try {
                // TYRUS-208: HandshakeRequest.getHeaders() should return read-only map.
                request.getHeaders().put("test", Arrays.asList("TYRUS-208"));
                return;
            } catch (UnsupportedOperationException e) {
                // expected.
            }

            try {
                // TYRUS-211: HandshakeRequest.getHeaders() should return read-only map.
                request.getParameterMap().put("test", Arrays.asList("TYRUS-211"));
                return;
            } catch (UnsupportedOperationException e) {
                // expected.
            }

            response.getHeaders().put(HEADER_NAME, list);
            response.getHeaders().put("Origin", request.getHeaders().get("Origin"));
        }
    }

    public static class MyClientConfigurator extends ClientEndpointConfig.Configurator {
        static volatile boolean called = false;

        @Override
        public void beforeRequest(Map<String, List<String>> headers) {
            called = true;
            headers.put(HEADER_NAME, Arrays.asList(HEADER_VALUE));
            headers.put("Origin", Arrays.asList("myOrigin"));
        }

        @Override
        public void afterResponse(HandshakeResponse handshakeResponse) {
            final Map<String, List<String>> headers = handshakeResponse.getHeaders();

            assertEquals(HEADER_VALUE[0], headers.get(HEADER_NAME).get(0));
            assertEquals(HEADER_VALUE[1], headers.get(HEADER_NAME).get(1));
            assertEquals(HEADER_VALUE[2], headers.get(HEADER_NAME).get(2));
            assertEquals("myOrigin", headers.get("origin").get(0));
        }
    }

    @Test
    public void testHeadersProgrammatic() throws DeploymentException {
        Server server = startServer(TestEndpoint.class);
        MyClientConfigurator.called = false;

        final CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            final MyClientConfigurator clientConfigurator = new MyClientConfigurator();
            final ClientEndpointConfig cec =
                    ClientEndpointConfig.Builder.create().configurator(clientConfigurator).build();

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(final Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                receivedMessage = message;
                                messageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText(SENT_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(TestEndpoint.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
            assertTrue(MyClientConfigurator.called);
            assertEquals(SENT_MESSAGE, receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @ClientEndpoint(configurator = MyClientConfigurator.class)
    public static class MyClientEndpoint {
        public static final CountDownLatch messageLatch = new CountDownLatch(1);
        public static volatile String receivedMessage;

        @OnOpen
        public void onOpen(Session session) throws IOException {
            session.getBasicRemote().sendText(SENT_MESSAGE);
        }

        @OnMessage
        public void onMessage(String message) {
            receivedMessage = message;
            messageLatch.countDown();
        }
    }

    @Test
    public void testHeadersAnnotated() throws DeploymentException {
        Server server = startServer(TestEndpoint.class);
        MyClientConfigurator.called = false;

        try {
            ClientManager client = createClient();
            client.connectToServer(MyClientEndpoint.class, getURI(TestEndpoint.class));

            MyClientEndpoint.messageLatch.await(5, TimeUnit.SECONDS);
            assertTrue(MyClientConfigurator.called);
            assertEquals(0, MyClientEndpoint.messageLatch.getCount());
            assertEquals(SENT_MESSAGE, MyClientEndpoint.receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
