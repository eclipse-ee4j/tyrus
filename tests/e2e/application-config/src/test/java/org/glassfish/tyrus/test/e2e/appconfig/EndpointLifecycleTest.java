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

package org.glassfish.tyrus.test.e2e.appconfig;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.server.TyrusServerConfiguration;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class EndpointLifecycleTest extends TestContainer {

    private static final String SENT_MESSAGE = "Hello World";

    private static final String PATH = "/EndpointLifecycleTest1";

    static final int iterations = 3;

    public EndpointLifecycleTest() {
        this.setContextPath("/e2e-test-appconfig");
    }

    public static class ServerDeployApplicationConfig extends TyrusServerConfiguration {
        public ServerDeployApplicationConfig() {
            super(new HashSet<Class<?>>() {
                {
                    add(Annotated.class);
                    add(ServiceEndpoint.class);
                }
            }, Collections.<ServerEndpointConfig>emptySet());
        }
    }

    @ServerEndpoint(value = "/servicelifecycletest")
    public static class ServiceEndpoint {

        @OnMessage
        public String onMessage(String message) {
            if (message.equals("Annotated")) {
                if (Annotated.getInstancesIds().size() == iterations) {
                    return POSITIVE;
                }
            } else if (message.equals("Programmatic")) {
                if (Programmatic.getInstancesIds().size() == iterations) {
                    return POSITIVE;
                }
            }

            return NEGATIVE;
        }
    }

    @Test
    public void testProgrammaticEndpoint() throws DeploymentException {

        final AtomicInteger msgNumber = new AtomicInteger(0);
        Server server = startServer(
                ProgrammaticEndpointApplicationConfiguration.class, ServerDeployApplicationConfig.class);

        final ClientManager client = createClient();
        try {
            for (int i = 0; i < iterations; i++) {
                try {
                    final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

                    final String message = SENT_MESSAGE + msgNumber.incrementAndGet();
                    // replace ClientManager with MockClientEndpoint to confirm the test passes if the backend
                    // does not have issues

                    client.connectToServer(new Endpoint() {
                        @Override
                        public void onOpen(Session session, EndpointConfig config) {
                            try {
                                session.addMessageHandler(new MessageHandler.Whole<String>() {
                                    @Override
                                    public void onMessage(String message) {

                                    }
                                });
                                session.getBasicRemote().sendText(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, cec, getURI(PATH));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "Programmatic");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testAnnotatedEndpoint() throws DeploymentException {

        final AtomicInteger msgNumber = new AtomicInteger(0);
        Server server = startServer(Annotated.class, ServiceEndpoint.class);

        final ClientManager client = createClient();
        try {
            for (int i = 0; i < iterations; i++) {
                try {
                    final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

                    final String message = SENT_MESSAGE + msgNumber.incrementAndGet();
                    // replace ClientManager with MockClientEndpoint to confirm the test passes if the backend
                    // does not have issues

                    client.connectToServer(new Endpoint() {

                        @Override
                        public void onOpen(Session session, EndpointConfig config) {
                            try {
                                session.addMessageHandler(new MessageHandler.Whole<String>() {
                                    @Override
                                    public void onMessage(String message) {

                                    }
                                });
                                session.getBasicRemote().sendText(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, cec, getURI("/EndpointLifecycleTest2"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            testViaServiceEndpoint(client, ServiceEndpoint.class, POSITIVE, "Annotated");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    public static class ProgrammaticEndpointApplicationConfiguration extends TyrusServerConfiguration {

        public ProgrammaticEndpointApplicationConfiguration() {
            super(Collections.<Class<?>>emptySet(), new HashSet<ServerEndpointConfig>() {
                {
                    add(ServerEndpointConfig.Builder.create(Programmatic.class, PATH).build());
                }
            });
        }
    }

    @ServerEndpoint(value = "/EndpointLifecycleTest2")
    public static class Annotated {

        private static final Set<String> instancesIds =
                Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

        @OnOpen
        public void onOpen(Session s) {
            instancesIds.add(this.toString());
        }

        @OnMessage
        public void onMessage(String message, Session session) {

        }

        public static Set<String> getInstancesIds() {
            return instancesIds;
        }
    }

    public static class Programmatic extends Endpoint {

        private static final Set<String> instancesIds =
                Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            instancesIds.add(this.toString());
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {

        }

        public static Set<String> getInstancesIds() {
            return instancesIds;
        }
    }
}
