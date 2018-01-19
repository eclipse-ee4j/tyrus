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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.core.coder.CoderAdapter;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.server.TyrusServerConfiguration;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * See TYRUS-261.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class TextAndBinaryProgrammaticDecoder extends TestContainer {

    public TextAndBinaryProgrammaticDecoder() {
        this.setContextPath("/e2e-test-appconfig");
    }

    @Test
    public void test2Decoders1Handler() throws DeploymentException, IOException, InterruptedException {
        final Server server = startServer(ServerDeployApplicationConfig.class);

        try {
            final CountDownLatch textLatch = new CountDownLatch(1);
            final CountDownLatch binaryLatch = new CountDownLatch(1);

            final Session session = ContainerProvider.getWebSocketContainer().connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            if (message.equals("text")) {
                                textLatch.countDown();
                            } else if (message.equals("binary")) {
                                binaryLatch.countDown();
                            }
                        }
                    });
                }
            }, ClientEndpointConfig.Builder.create().build(), getURI("/textAndBinaryDecoderEndpoint"));

            session.getBasicRemote().sendText("test");
            session.getBasicRemote().sendBinary(ByteBuffer.wrap("test".getBytes("UTF-8")));

            assertTrue(textLatch.await(3, TimeUnit.SECONDS));
            assertTrue(binaryLatch.await(3, TimeUnit.SECONDS));
        } finally {
            server.stop();
        }
    }

    @Test
    public void testAnnotatedEndpointRegisteredProgramatically() throws DeploymentException, IOException,
            InterruptedException {
        final Server server = startServer(ServerDeployApplicationConfig.class);

        try {
            final CountDownLatch textLatch = new CountDownLatch(1);

            final Session session = ContainerProvider.getWebSocketContainer().connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            if (message.equals("text")) {
                                textLatch.countDown();
                            }
                        }
                    });
                }
            }, ClientEndpointConfig.Builder.create().build(), getURI("/annotatedEndpointRegisteredProgramatically"));

            session.getBasicRemote().sendText("text");

            assertTrue(textLatch.await(3, TimeUnit.SECONDS));
        } finally {
            server.stop();
        }
    }

    public static class ServerDeployApplicationConfig extends TyrusServerConfiguration {
        public ServerDeployApplicationConfig() {
            super(Collections.<Class<?>>emptySet(), new HashSet<ServerEndpointConfig>() {
                {
                    add(ServerEndpointConfig.Builder
                                .create(TextAndBinaryDecoderEndpoint.class, "/textAndBinaryDecoderEndpoint")
                                .decoders(Arrays.<Class<? extends Decoder>>asList(TextContainerDecoder.class,
                                                                                  BinaryContainerDecoder.class))
                                .build());
                    add(ServerEndpointConfig.Builder
                                .create(AnnotatedEndpoint.class, "/annotatedEndpointRegisteredProgramatically")
                                .build());

                }
            });
        }
    }

    @ServerEndpoint(value = "/annotatedEndpointRegisteredProgramatically")
    public static class AnnotatedEndpoint {
        @OnMessage
        public String onMessage(String message) {
            return message;
        }
    }

    public static class TextAndBinaryDecoderEndpoint extends Endpoint {

        @Override
        public void onOpen(final Session session, EndpointConfig config) {
            session.addMessageHandler(new MessageHandler.Whole<Message>() {
                @Override
                public void onMessage(Message message) {
                    try {
                        session.getBasicRemote().sendText(message.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                        // do nothing.
                    }
                }
            });
        }
    }

    public static class Message {

        final boolean type;

        public Message(boolean type) {
            this.type = type;
        }

        @Override
        public String toString() {
            if (type) {
                return "binary";
            } else {
                return "text";
            }
        }
    }

    public static class BinaryContainerDecoder extends CoderAdapter implements Decoder.Binary<Message> {
        @Override
        public Message decode(ByteBuffer bytes) throws DecodeException {
            return new Message(true);
        }

        @Override
        public boolean willDecode(ByteBuffer bytes) {
            return true;
        }
    }

    public static class TextContainerDecoder extends CoderAdapter implements Decoder.Text<Message> {
        @Override
        public Message decode(String s) throws DecodeException {
            return new Message(false);
        }

        @Override
        public boolean willDecode(String s) {
            return true;
        }
    }
}
