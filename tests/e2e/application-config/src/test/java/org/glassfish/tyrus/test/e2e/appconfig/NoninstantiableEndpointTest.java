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

package org.glassfish.tyrus.test.e2e.appconfig;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.TyrusServerEndpointConfig;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.server.TyrusServerConfiguration;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests non-instantiable endpoints.
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
public class NoninstantiableEndpointTest extends TestContainer {

    public static final String PATH_PREFIX = "/non-instantiable-test";
    public static final String PROGRAMMATIC = PATH_PREFIX + "-programmatic";
    public static final String ANNOTATED = PATH_PREFIX + "-annotated";

    public NoninstantiableEndpointTest() {
        this.setContextPath("/e2e-test-appconfig");
    }

    public static class ServerDeployApplicationConfig extends TyrusServerConfiguration {
        public ServerDeployApplicationConfig() {
            super(new HashSet<Class<?>>() {
                {
                    add(AnnotatedAllMethodsEndpoint.class);
                    add(AnnotatedOpenMessageCloseMethodEndpoint.class);
                    add(AnnotatedOnOpenMethodEndpoint.class);
                    add(AnnotatedOnMessageMethodEndpoint.class);
                    add(AnnotatedOnCloseMethodEndpoint.class);
                }
            }, new HashSet<ServerEndpointConfig>() {
                {
                    add(TyrusServerEndpointConfig.Builder.create(ProgrammaticEndpoint.class, PROGRAMMATIC).build());
                }
            });
        }
    }

    // private class - cannot be instantiate by tyrus
    private static class ProgrammaticEndpoint extends Endpoint {
        @Override
        public void onOpen(final Session session, final EndpointConfig EndpointConfig) {
            System.out.println("SERVER onOpen: " + session);

            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    System.out.println("### Server: Message received: " + message);
                    try {
                        final String toSend = message + " (from your server)";
                        System.out.println("### Server: Sending: " + toSend);
                        session.getBasicRemote().sendText(toSend);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            System.out.println("SERVER onClose: " + session);
            System.out.println("SERVER onClose: " + closeReason);
        }
    }

    // private class - cannot be instantiate by tyrus
    @ServerEndpoint(ANNOTATED + "-omc")
    private static class AnnotatedOpenMessageCloseMethodEndpoint {
        @OnOpen
        public void onOpen(Session session) {
            System.out.println("SERVER onOpen: ");
        }

        @OnMessage
        public String onMessage(String message) {
            return message;
        }

        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            System.out.println("SERVER onClose: " + closeReason);
        }
    }

    // private class - cannot be instantiate by tyrus
    @ServerEndpoint(ANNOTATED + "-omce")
    static class AnnotatedAllMethodsEndpoint {
        @OnOpen
        public void onOpen(Session session) {
            System.out.println("SERVER onOpen: ");
        }

        @OnMessage
        public String onMessage(String message) {
            return message;
        }

        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            System.out.println("SERVER onClose: " + closeReason);
        }

        @OnError
        public void onError(Throwable t) {
            System.out.println(t);
        }
    }

    // private class - cannot be instantiate by tyrus
    @ServerEndpoint(ANNOTATED + "-m")
    static class AnnotatedOnMessageMethodEndpoint {
        @OnMessage
        public String onMessage(String message) {
            return message;
        }
    }

    // private class - cannot be instantiate by tyrus
    @ServerEndpoint(ANNOTATED + "-o")
    static class AnnotatedOnOpenMethodEndpoint {
        @OnOpen
        public void onOpen(Session session) {
            System.out.println("SERVER onOpen: ");
        }
    }

    // private class - cannot be instantiate by tyrus
    @ServerEndpoint(ANNOTATED + "-c")
    static class AnnotatedOnCloseMethodEndpoint {
        @OnClose
        public void onClose(Session session) {
            System.out.println("SERVER onClose: ");
        }
    }

    @Test
    public void testProgrammatic() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer(ServerDeployApplicationConfig.class);
        final ClientManager client = createClient();

        final CountDownLatch closeUnexpectedlyLatch = new CountDownLatch(1);

        try {
            Session session = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(final Session session, EndpointConfig config) {
                    System.out.println("CLIENT onOpen: " + session);
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            System.out.println("### CLIENT: Message received: " + message);
                        }
                    });
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    System.out.println("CLIENT onClose: " + session);
                    System.out.println("CLIENT onClose: " + closeReason);
                    if (closeReason.getCloseCode().getCode() == CloseReason.CloseCodes.UNEXPECTED_CONDITION.getCode()) {
                        closeUnexpectedlyLatch.countDown();
                    }

                }
            }, ClientEndpointConfig.Builder.create().build(), getURI(PROGRAMMATIC));

            assertTrue("Client should receive onClose with reason 1011 - Unexpected Condition",
                       closeUnexpectedlyLatch.await(1, TimeUnit.SECONDS));
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testAllMethodsEndpoints() throws DeploymentException, InterruptedException, IOException {
        // test endpoint with all methods implemented
        testUnexpectedConditionClose(AnnotatedAllMethodsEndpoint.class, false);
    }

    @Test
    public void testOpenMessageCloseMethodsEndpoints() throws DeploymentException, InterruptedException, IOException {
        // test endpoint with all except onError methods implemented
        testUnexpectedConditionClose(AnnotatedOpenMessageCloseMethodEndpoint.class, false);
    }

    @Test
    public void testOnOpenMethodEndpoints() throws DeploymentException, InterruptedException, IOException {
        // test endpoint with onOpen only method implemented

        testUnexpectedConditionClose(AnnotatedOnOpenMethodEndpoint.class, false);
    }

    @Test
    public void testOnMessageMethodEndpoints() throws DeploymentException, InterruptedException, IOException {
        // test endpoint with onMessage only method implemented
        testUnexpectedConditionClose(AnnotatedOnMessageMethodEndpoint.class, true);
    }

    @Test
    public void testOnCloseMethodEndpoints() throws DeploymentException, InterruptedException, IOException {
        // test endpoint with onClose only method implemented
        testNormalClose(AnnotatedOnCloseMethodEndpoint.class);
    }

    public void testUnexpectedConditionClose(final Class<?> endpointClass, boolean sendMessage) throws
            DeploymentException, InterruptedException, IOException {
        final Server server = startServer(ServerDeployApplicationConfig.class);
        final ClientManager client = createClient();

        final CountDownLatch closeUnexpectedlyLatch = new CountDownLatch(1);

        try {
            Session session = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(final Session session, EndpointConfig config) {
                    System.out.println("CLIENT onOpen: " + session);
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            System.out.println("CLIENT: Message received: " + message);
                        }
                    });
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    System.out.println("CLIENT onClose: " + session);
                    System.out.println("CLIENT onClose: " + closeReason);
                    if (closeReason.getCloseCode().getCode() == CloseReason.CloseCodes.UNEXPECTED_CONDITION.getCode()) {
                        closeUnexpectedlyLatch.countDown();
                    }

                }
            }, ClientEndpointConfig.Builder.create().build(), getURI(endpointClass));

            if (sendMessage) {
                session.getBasicRemote().sendText("hello");
            }

            assertTrue("Client should receive onClose with reason 1011 - Unexpected Condition",
                       closeUnexpectedlyLatch.await(1, TimeUnit.SECONDS));
        } finally {
            stopServer(server);
        }
    }

    public void testNormalClose(final Class<?> endpointClass) throws DeploymentException, InterruptedException,
            IOException {
        final Server server = startServer(ServerDeployApplicationConfig.class);
        final ClientManager client = createClient();

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        try {
            Session session = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(final Session session, EndpointConfig config) {
                    System.out.println("CLIENT onOpen: " + session);
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            System.out.println("CLIENT: Message received: " + message);
                        }
                    });
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    System.out.println("CLIENT onClose: " + session);
                    System.out.println("CLIENT onClose: " + closeReason);
                    if (closeReason.getCloseCode().getCode() == CloseReason.CloseCodes.NORMAL_CLOSURE.getCode()) {
                        countDownLatch.countDown();
                    }

                }
            }, ClientEndpointConfig.Builder.create().build(), getURI(endpointClass));

            session.close();

            assertTrue("Client should receive onClose with reason 1000 - Normal Closure",
                       countDownLatch.await(1, TimeUnit.SECONDS));
        } finally {
            stopServer(server);
        }
    }

}
