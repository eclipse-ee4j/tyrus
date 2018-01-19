/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.qa;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Michal Conos (michal.conos at oracle.com)
 */
public class HandshakeTest {

    private final String CONTEXT_PATH = "/browser-test";
    private final String DEFAULT_HOST = "localhost";
    private final int DEFAULT_PORT = 8025;
    private final Set<Class<?>> endpointClasses = new HashSet<Class<?>>() {
        {
            add(HandshakeBean.class);
        }
    };

    /**
     * Start embedded server unless "tyrus.test.host" system property is
     * specified.
     *
     * @return new {@link Server} instance or {@code null} if "tyrus.test.host"
     *         system property is set.
     */
    private Server startServer() throws DeploymentException {
        final String host = System.getProperty("tyrus.test.host");
        if (host == null) {
            final Server server = new Server(DEFAULT_HOST, DEFAULT_PORT, CONTEXT_PATH, null, endpointClasses);
            server.start();
            return server;
        } else {
            return null;
        }
    }

    private String getHost() {
        final String host = System.getProperty("tyrus.test.host");
        if (host != null) {
            return host;
        }
        return DEFAULT_HOST;
    }

    private int getPort() {
        final String port = System.getProperty("tyrus.test.port");
        if (port != null) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException nfe) {
                // do nothing
            }
        }
        return DEFAULT_PORT;
    }

    private URI getURI() {
        try {
            return new URI("ws", null, getHost(), getPort(), CONTEXT_PATH + "/chat", null, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void stopServer(Server server) {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testChatBean() throws DeploymentException, InterruptedException, IOException {
        final Server server = startServer();

        final CountDownLatch stopConversation = new CountDownLatch(1);

        final ClientManager client = ClientManager.createClient();
        client.connectToServer(new Endpoint() {
            ConnState state = ConnState.NONE;
            boolean sendOver = false;

            @Override
            public void onOpen(final Session session, EndpointConfig EndpointConfig) {

                System.out.println("client.container:" + session.getContainer().toString());
                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {

                            try {
                                System.out.println("RECEIVED: " + message);
                                assertEquals(message, state.getSendMsg());
                                if (!sendOver) {
                                    System.out.println("SENDING : " + state.getExpMsg());
                                    session.getBasicRemote().sendText(state.getExpMsg());
                                    state = ConnState.next(state.getExpMsg(), state);
                                    if (state == ConnState.BROWSER_OKAY) {
                                        sendOver = true;
                                        stopConversation.countDown();
                                    }
                                }
                            } catch (IOException ex) {
                                Logger.getLogger(HandshakeTest.class.getName()).log(Level.SEVERE, null, ex);
                            }


                        }
                    });


                    System.out.println("SENDING : " + state.getExpMsg());
                    session.getBasicRemote().sendText(state.getExpMsg());
                    state = ConnState.next(state.getExpMsg(), state);
                } catch (IOException e) {
                    // do nothing
                }

            }
        }, ClientEndpointConfig.Builder.create().build(), getURI());

        stopConversation.await(10, TimeUnit.SECONDS);
        if (stopConversation.getCount() != 0) {
            fail();
        }

        stopServer(server);
    }
}
