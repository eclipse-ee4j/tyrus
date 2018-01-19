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
import java.io.InputStream;
import java.nio.ByteBuffer;
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
import static org.junit.Assert.fail;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class InputStreamTest extends TestContainer {

    public InputStreamTest() {
        this.setContextPath("/e2e-test-appconfig");
    }

    public static class ServerDeployApplicationConfig extends TyrusServerConfiguration {
        public ServerDeployApplicationConfig() {
            super(new HashSet<Class<?>>() {
                {
                    add(InputStreamEndpoint.class);
                }
            }, Collections.<ServerEndpointConfig>emptySet());
        }
    }

    @ServerEndpoint(value = "/inputStream")
    public static class InputStreamEndpoint {

        @OnMessage
        public String readInputStream(InputStream is) {
            byte[] buffer = new byte[64];
            try {
                int i = is.read(buffer);
                return new String(buffer, 0, i);
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static class InputStreamApplicationConfiguration extends TyrusServerConfiguration {

        public InputStreamApplicationConfiguration() {
            super(Collections.<Class<?>>emptySet(), new HashSet<ServerEndpointConfig>() {
                {
                    add(ServerEndpointConfig.Builder.create(
                            InputStreamProgrammaticEndpoint.class, "/inputStreamProgrammatic").build());
                }
            });
        }
    }

    @Test
    public void testInputStreamAnnotated() throws DeploymentException {
        _testInputStream(InputStreamEndpoint.class,
                         InputStreamEndpoint.class.getAnnotation(ServerEndpoint.class).value());
    }

    @Test
    public void testInputStreamProgrammatic() throws DeploymentException {
        _testInputStream(InputStreamApplicationConfiguration.class, "/inputStreamProgrammatic");
    }

    public void _testInputStream(Class<?> endpoint, String path) throws DeploymentException {
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
                        session.getBasicRemote().sendBinary(
                                ByteBuffer.wrap("Do or do not, there is no try.".getBytes()));
                    } catch (IOException e) {
                        fail();
                    }
                }
            }, cec, getURI(path));

            messageLatch.await(5, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    public static class InputStreamProgrammaticEndpoint extends Endpoint {
        @Override
        public void onOpen(final Session session, EndpointConfig config) {
            session.addMessageHandler(new MessageHandler.Whole<InputStream>() {
                @Override
                public void onMessage(InputStream is) {
                    byte[] buffer = new byte[64];
                    try {
                        int i = is.read(buffer);
                        session.getBasicRemote().sendText(new String(buffer, 0, i));
                    } catch (IOException e) {
                        //
                    }
                }
            });
        }
    }
}
