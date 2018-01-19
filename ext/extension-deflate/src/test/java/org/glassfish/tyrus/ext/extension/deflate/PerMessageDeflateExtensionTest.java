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

package org.glassfish.tyrus.ext.extension.deflate;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.server.TyrusServerConfiguration;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class PerMessageDeflateExtensionTest extends TestContainer {

    public static class ServerDeployApplicationConfig extends TyrusServerConfiguration {
        public ServerDeployApplicationConfig() {
            super(Collections.<Class<?>>emptySet(), new HashSet<ServerEndpointConfig>() {
                {
                    add(ServerEndpointConfig.Builder.create(EchoEndpoint.class, "/compressionExtensionTest")
                                                    .extensions(
                                                            Arrays.<Extension>asList(new PerMessageDeflateExtension()))
                                                    .build());
                }

                private static final long serialVersionUID = -6065653369480760041L;
            });
        }
    }

    public static class EchoEndpoint extends Endpoint {

        @Override
        public void onOpen(final Session session, EndpointConfig config) {
            session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
                @Override
                public void onMessage(byte[] message) {
                    try {
                        session.getBasicRemote().sendBinary(ByteBuffer.wrap(message));
                    } catch (IOException e) {
                        // ignore.
                    }
                }
            });
        }
    }

    @Test
    public void testCompressedExtension() throws DeploymentException {
        Server server = startServer(ServerDeployApplicationConfig.class);
        final CountDownLatch messageLatch = new CountDownLatch(5);

        try {
            ArrayList<Extension> extensions = new ArrayList<Extension>();
            extensions.add(new PerMessageDeflateExtension());

            final ClientEndpointConfig clientConfiguration =
                    ClientEndpointConfig.Builder.create().extensions(extensions)
                                                .configurator(new LoggingClientEndpointConfigurator()).build();

            ClientManager client = ClientManager.createClient();
            final Session session = client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
                        @Override
                        public void onMessage(byte[] message) {
                            System.out.println("client onMessage: " + new String(message, Charset.forName("UTF-8")));
                            messageLatch.countDown();
                        }
                    });
                }
            }, clientConfiguration, getURI("/compressionExtensionTest"));

            assertTrue(session.getNegotiatedExtensions().size() > 0);

            boolean compressionNegotiated = false;
            for (Extension e : session.getNegotiatedExtensions()) {
                if (e instanceof PerMessageDeflateExtension) {
                    compressionNegotiated = true;
                }
            }

            assertTrue(compressionNegotiated);

            try {
                byte[] bytes = "Always pass on what you have learned.".getBytes(Charset.forName("UTF-8"));
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(bytes));
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(bytes));
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(bytes));
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(bytes));
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(bytes));
            } catch (IOException e) {
                e.printStackTrace();
            }

            assertTrue(messageLatch.await(1, TimeUnit.SECONDS));

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }

    }

    public static class LoggingClientEndpointConfigurator extends ClientEndpointConfig.Configurator {
        @Override
        public void beforeRequest(Map<String, List<String>> headers) {
            System.out.println("##### beforeRequest");
            System.out.println(headers);
            System.out.println();
        }

        @Override
        public void afterResponse(HandshakeResponse hr) {
            System.out.println("##### afterResponse");
            System.out.println(hr.getHeaders());
            System.out.println();
        }
    }
}
