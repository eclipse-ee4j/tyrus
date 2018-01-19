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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.server.TyrusServerConfiguration;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ReaderWriterTest extends TestContainer {

    public ReaderWriterTest() {
        this.setContextPath("/e2e-test-appconfig");
    }

    public static class ServerDeployApplicationConfig extends TyrusServerConfiguration {
        public ServerDeployApplicationConfig() {
            super(new HashSet<Class<?>>() {
                {
                    add(ReaderEndpoint.class);
                }
            }, Collections.<ServerEndpointConfig>emptySet());
        }
    }

    @ServerEndpoint(value = "/reader")
    public static class ReaderEndpoint {

        @OnMessage
        public String readReader(Reader r) {
            char[] buffer = new char[64];
            try {
                int i = r.read(buffer);
                return new String(buffer, 0, i);
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static class ReaderEndpointApplicationConfiguration extends TyrusServerConfiguration {

        public ReaderEndpointApplicationConfiguration() {
            super(Collections.<Class<?>>emptySet(), new HashSet<ServerEndpointConfig>() {
                {
                    add(ServerEndpointConfig.Builder.create(ReaderProgrammaticEndpoint.class, "/readerProgrammatic")
                                                    .build());
                }
            });
        }
    }

    @Test
    public void testReaderAnnotated() throws DeploymentException {
        _testReader(ReaderEndpoint.class, ReaderEndpoint.class.getAnnotation(ServerEndpoint.class).value());
    }

    @Test
    public void testReaderProgrammatic() throws DeploymentException {
        _testReader(ReaderEndpointApplicationConfiguration.class, "/readerProgrammatic");
    }

    @Test
    public void testWriterAnnotated() throws DeploymentException {
        _testWriter(ReaderEndpoint.class, ReaderEndpoint.class.getAnnotation(ServerEndpoint.class).value());
    }


    @Test
    public void testWriterProgrammatic() throws DeploymentException {
        _testWriter(ReaderEndpointApplicationConfiguration.class, "/readerProgrammatic");
    }

    public void _testReader(Class<?> endpoint, String path) throws DeploymentException {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        Server server = startServer(endpoint);
        final CountDownLatch messageLatch;

        try {
            messageLatch = new CountDownLatch(1);
            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig configuration) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                if (message.equals("Do or do not, there is no try.")) {
                                    messageLatch.countDown();
                                }
                            }
                        });
                        session.getBasicRemote().sendText("Do or do not, there is no try.");
                    } catch (IOException e) {
                        fail();
                    }
                }
            }, cec, getURI(path));

            messageLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    public void _testWriter(Class<?> endpoint, String path) throws DeploymentException {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        Server server = startServer(endpoint);
        final CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig configuration) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                if (message.equals("Do or do not, there is no try.")) {
                                    messageLatch.countDown();
                                } else {
                                    System.err.println("Wrong message: " + message);
                                }
                            }
                        });
                        final Writer sendWriter = session.getBasicRemote().getSendWriter();
                        sendWriter.append("Do or do not, there is no try.");
                        sendWriter.close();
                    } catch (IOException e) {
                        fail();
                    }
                }
            }, cec, getURI(path));

            assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    public static class ReaderProgrammaticEndpoint extends Endpoint {
        @Override
        public void onOpen(final Session session, EndpointConfig config) {
            session.addMessageHandler(new MessageHandler.Whole<Reader>() {
                @Override
                public void onMessage(Reader r) {
                    char[] buffer = new char[64];
                    try {
                        int i = r.read(buffer);
                        session.getBasicRemote().sendText(new String(buffer, 0, i));
                    } catch (IOException e) {
                        //
                    }
                }
            });
        }
    }
}
