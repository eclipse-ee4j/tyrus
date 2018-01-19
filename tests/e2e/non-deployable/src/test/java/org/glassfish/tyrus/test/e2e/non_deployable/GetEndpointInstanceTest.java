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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class GetEndpointInstanceTest extends TestContainer {
    private static final String SENT_MESSAGE = "Always pass on what you have learned.";

    private String receivedMessage;

    @ServerEndpoint(value = "/echoAnnotated", configurator = MyServerConfigurator.class)
    public static class MyEndpointAnnotated {

        @OnMessage
        public String onMessage(String message) {

            assertEquals(MyServerConfigurator.testEndpoint1, this);

            return message;
        }
    }

    public static class MyEndpointProgrammatic extends Endpoint implements MessageHandler.Whole<String> {

        Session session;

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            this.session = session;
            session.addMessageHandler(this);
        }

        @Override
        public void onMessage(String message) {

            assertEquals(MyServerConfigurator.testEndpoint2, this);

            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                // do nothing.
            }
        }
    }

    public static class MyServerConfigurator extends ServerEndpointConfig.Configurator {

        public static final MyEndpointAnnotated testEndpoint1 = new MyEndpointAnnotated();
        public static final MyEndpointProgrammatic testEndpoint2 = new MyEndpointProgrammatic();

        @Override
        public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
            if (endpointClass.equals(MyEndpointAnnotated.class)) {
                return (T) testEndpoint1;
            } else if (endpointClass.equals(MyEndpointProgrammatic.class)) {
                return (T) testEndpoint2;
            }

            throw new InstantiationException();
        }
    }

    public static class MyApplication implements ServerApplicationConfig {
        @Override
        public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
            return new HashSet<ServerEndpointConfig>(Arrays.asList(
                    ServerEndpointConfig.Builder.create(MyEndpointProgrammatic.class, "/echoProgrammatic")
                                                .configurator(new MyServerConfigurator()).build()));
        }

        @Override
        public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
            return new HashSet<Class<?>>(Arrays.<Class<?>>asList(MyEndpointAnnotated.class));
        }
    }

    @Test
    public void testAnnotated() throws DeploymentException {
        Server server = startServer(MyEndpointAnnotated.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

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
            }, cec, getURI(MyEndpointAnnotated.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
            assertEquals(SENT_MESSAGE, receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testProgrammatic() throws DeploymentException {
        final CountDownLatch messageLatch = new CountDownLatch(1);
        final Server server = startServer(MyApplication.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();

            System.out.println(getURI("/echoProgrammatic"));

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
            }, cec, getURI("/echoProgrammatic"));

            messageLatch.await(5, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
            assertEquals(SENT_MESSAGE, receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
