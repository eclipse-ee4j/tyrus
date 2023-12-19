/*
 * Copyright (c) 2012, 2023 Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.exception.DeploymentHandshakeException;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.spi.TyrusClientEndpointConfigurator;
import org.glassfish.tyrus.spi.UpgradeRequest;
import org.glassfish.tyrus.spi.UpgradeResponse;
import org.glassfish.tyrus.test.standard_config.bean.TestEndpoint;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests whether the HandShake parameters (sub-protocols, extensions) are sent correctly.
 *
 * @author Stepan Kopriva
 */
public class HandshakeTest extends TestContainer {

    private CountDownLatch messageLatch;

    private String receivedMessage;

    private static final String SENT_MESSAGE = "hello";

    public static class MyClientEndpointConfigurator extends ClientEndpointConfig.Configurator {

        public HandshakeResponse hr;

        @Override
        public void afterResponse(HandshakeResponse hr) {
            this.hr = hr;
        }
    }

    @Test
    public void testClient() throws DeploymentException {
        Server server = startServer(TestEndpoint.class);

        try {
            messageLatch = new CountDownLatch(1);

            ArrayList<String> subprotocols = new ArrayList<String>();
            subprotocols.add("asd");
            subprotocols.add("ghi");

            final MyClientEndpointConfigurator myClientEndpointConfigurator = new MyClientEndpointConfigurator();

            final ClientEndpointConfig cec =
                    ClientEndpointConfig.Builder.create().preferredSubprotocols(subprotocols)
                                                .configurator(myClientEndpointConfigurator).build();

            ClientManager client = createClient();
            client.connectToServer(new TestEndpointAdapter() {
                @Override
                public void onMessage(String message) {
                    receivedMessage = message;
                    messageLatch.countDown();
                    System.out.println("Received message = " + message);
                }

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }

                @Override
                public void onOpen(Session session) {
                    try {
                        session.addMessageHandler(new TestTextMessageHandler(this));
                        session.getBasicRemote().sendText(SENT_MESSAGE);
                        System.out.println("Sent message: " + SENT_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(TestEndpoint.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals(SENT_MESSAGE, receivedMessage);

            Map<String, List<String>> headers = myClientEndpointConfigurator.hr.getHeaders();

            String supportedSubprotocol = headers.get(HandshakeRequest.SEC_WEBSOCKET_PROTOCOL).get(0);
            Assert.assertEquals("asd", supportedSubprotocol);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }


    static final int STATUS = 499;
    static final String HEADER = "TEST_HEADER";

    public static class StatusSetterConfiguration extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            ((UpgradeResponse) response).setStatus(STATUS);
            response.getHeaders().put(HEADER, Collections.singletonList(HEADER));
        }
    }

    @Test
    public void customResponseTest() throws DeploymentException, IOException {
        @ServerEndpoint(value = "/status", configurator = StatusSetterConfiguration.class)
        class StatusSetterEndpoint {
            @OnMessage
            public void onMessage(String message) {
                throw new IllegalStateException("ON MESSAGE");
            }
        }

        final AtomicReference<Integer> status = new AtomicReference<>();
        final AtomicReference<String> header = new AtomicReference<>();

        Server server = startServer(StatusSetterEndpoint.class);

        ClientEndpointConfig.Configurator cecc = new ClientEndpointConfig.Configurator() {
            @Override
            public void afterResponse(HandshakeResponse hr) {
                status.set(((UpgradeResponse) hr).getStatus());
                header.set(((UpgradeResponse) hr).getFirstHeaderValue(HEADER));
            }
        };

        ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().configurator(cecc).build();

        try {
            ClientManager client = createClient();
            Session session = client.connectToServer(new TestEndpointAdapter() {
                @Override
                public void onMessage(String message) {
                }

                @Override
                public void onOpen(Session session) {
                    try {
                        session.getBasicRemote().sendText("This should never be sent");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }
            }, cec, getURI(StatusSetterEndpoint.class));

            throw new IllegalStateException("DeploymentException was not thrown");
        } catch (DeploymentException de) {
            Assert.assertEquals(STATUS, status.get().intValue());
            Assert.assertEquals(HEADER, header.get());
        } finally {
            server.stop();
        }
    }

    public static class Status401SetterConfiguration extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            ((UpgradeResponse) response).setStatus(401);
            response.getHeaders().put(HEADER, Collections.singletonList(HEADER));
        }
    }

    @Test
    public void test401InConfigurer() throws DeploymentException, IOException {
        @ServerEndpoint(value = "/status", configurator = Status401SetterConfiguration.class)
        class Status401SetterEndpoint {
            @OnMessage
            public void onMessage(String message) {
                throw new IllegalStateException("ON MESSAGE");
            }
        }

        final AtomicReference<Integer> status = new AtomicReference<>();
        final AtomicReference<String> header = new AtomicReference<>();

        Server server = startServer(Status401SetterEndpoint.class);

        ClientEndpointConfig.Configurator cecc = new ClientEndpointConfig.Configurator() {
            @Override
            public void afterResponse(HandshakeResponse hr) {
                status.set(((UpgradeResponse) hr).getStatus());
                header.set(((UpgradeResponse) hr).getFirstHeaderValue(HEADER));
            }
        };

        ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().configurator(cecc).build();

        try {
            ClientManager client = createClient();
            Session session = client.connectToServer(new TestEndpointAdapter() {
                @Override
                public void onMessage(String message) {
                }

                @Override
                public void onOpen(Session session) {
                    try {
                        session.getBasicRemote().sendText("This should never be sent");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }
            }, cec, getURI(Status401SetterEndpoint.class));

            throw new IllegalStateException("DeploymentException was not thrown");
        } catch (DeploymentHandshakeException de) {
            Assert.assertEquals(401, status.get().intValue());
            Assert.assertEquals(status.get().intValue(), de.getHttpStatusCode());
            Assert.assertEquals(HEADER, header.get());
        } finally {
            server.stop();
        }
    }

    @Test
    public void beforeRequestUpgradeRequest() throws DeploymentException, IOException {
        @ServerEndpoint(value = "/status", configurator = StatusSetterConfiguration.class)
        class StatusSetterEndpoint {
            @OnMessage
            public void onMessage(String message) {
                throw new IllegalStateException("ON MESSAGE");
            }
        }

        final AtomicReference<UpgradeRequest> requestAtomicReference = new AtomicReference<>();

        Server server = startServer(StatusSetterEndpoint.class);

        ClientEndpointConfig.Configurator cecc = new TyrusClientEndpointConfigurator() {
            @Override
            public void beforeRequest(UpgradeRequest upgradeRequest) {
                requestAtomicReference.set(upgradeRequest);
            }
        };

        ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().configurator(cecc).build();

        try {
            ClientManager client = createClient();
            Session session = client.connectToServer(new TestEndpointAdapter() {
                @Override
                public void onMessage(String message) {
                }

                @Override
                public void onOpen(Session session) {
                    try {
                        session.getBasicRemote().sendText("This should never be sent");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }
            }, cec, getURI(StatusSetterEndpoint.class));

            throw new IllegalStateException("DeploymentException was not thrown");
        } catch (DeploymentException de) {
            Assert.assertEquals(getURI(StatusSetterEndpoint.class), requestAtomicReference.get().getRequestURI());
        } finally {
            server.stop();
        }
    }

    public static final AtomicReference<UpgradeRequest> REQUEST_ATOMIC_REFERENCE = new AtomicReference<>();

    public static class AnnotatedBeforeRequestTestClientConfigurator extends TyrusClientEndpointConfigurator {
        @Override
        public void beforeRequest(UpgradeRequest upgradeRequest) {
            REQUEST_ATOMIC_REFERENCE.set(upgradeRequest);
        }
    }

    @ClientEndpoint(configurator = AnnotatedBeforeRequestTestClientConfigurator.class)
    public static class AnnotatedBeforeRequestTestClient {

    }

    @Test
    public void beforeRequestUpgradeRequestOnAnnotatedClient() throws DeploymentException, IOException {
        @ServerEndpoint(value = "/status", configurator = StatusSetterConfiguration.class)
        class StatusSetterEndpoint {
            @OnMessage
            public void onMessage(String message) {
                throw new IllegalStateException("ON MESSAGE");
            }
        }

        Server server = startServer(StatusSetterEndpoint.class);

        try {
            ClientManager client = createClient();
            Session session = client.connectToServer(AnnotatedBeforeRequestTestClient.class, getURI(StatusSetterEndpoint.class));
            throw new IllegalStateException("DeploymentException was not thrown");
        } catch (DeploymentException de) {
            Assert.assertEquals(getURI(StatusSetterEndpoint.class), REQUEST_ATOMIC_REFERENCE.get().getRequestURI());
        } finally {
            server.stop();
        }
    }
}
