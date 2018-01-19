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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test for all supported message types.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class BinaryTest extends TestContainer {

    private CountDownLatch messageLatch;


    private static final byte[] BINARY_MESSAGE = new byte[]{1, 2, 3, 4};
    private static final String TEXT_MESSAGE = "Always pass on what you have learned.";

    private volatile ByteBuffer receivedMessageBuffer;
    private volatile byte[] receivedMessageArray;

    private final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

    /**
     * Bean to test correct processing of binary message.
     *
     * @author Stepan Kopriva (stepan.kopriva at oracle.com)
     */
    @ServerEndpoint(value = "/binary2")
    public static class BinaryByteBufferEndpoint {

        @OnMessage
        public ByteBuffer echo(ByteBuffer message) {
            return message;
        }
    }

    @Test
    public void testBinaryByteBufferBean() throws DeploymentException {
        Server server = startServer(BinaryByteBufferEndpoint.class);

        try {
            messageLatch = new CountDownLatch(1);

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.getBasicRemote().sendBinary(ByteBuffer.wrap(BINARY_MESSAGE));
                        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                            @Override
                            public void onMessage(ByteBuffer data) {
                                receivedMessageBuffer = data;
                                messageLatch.countDown();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(BinaryByteBufferEndpoint.class));
            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertArrayEquals("The received message is the same as the sent one", BINARY_MESSAGE,
                                     receivedMessageBuffer.array());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testDirectByteBuffer() throws DeploymentException {
        Server server = startServer(BinaryByteBufferEndpoint.class);
        final Charset UTF8 = Charset.forName("UTF-8");

        try {
            messageLatch = new CountDownLatch(1);

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        ByteBuffer buffer = ByteBuffer.allocateDirect(100);
                        buffer.put(TEXT_MESSAGE.getBytes(UTF8));
                        buffer.flip();
                        session.getBasicRemote().sendBinary(buffer);
                        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                            @Override
                            public void onMessage(ByteBuffer data) {
                                receivedMessageBuffer = data;
                                messageLatch.countDown();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(BinaryByteBufferEndpoint.class));
            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertArrayEquals("The received message is the same as the sent one", TEXT_MESSAGE.getBytes(UTF8),
                                     receivedMessageBuffer.array());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint(value = "/binary1")
    public static class BinaryByteArrayEndpoint {

        @OnMessage
        public byte[] echo(byte[] message) {
            return message;
        }
    }

    @Test
    public void testBinaryByteArrayBean() throws DeploymentException {
        Server server = startServer(BinaryByteArrayEndpoint.class);

        try {
            messageLatch = new CountDownLatch(1);

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
                        @Override
                        public void onMessage(byte[] data) {
                            receivedMessageArray = data;
                            messageLatch.countDown();
                        }
                    });
                    try {
                        ByteBuffer buffer = ByteBuffer.wrap(BINARY_MESSAGE);
                        session.getBasicRemote().sendBinary(buffer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(BinaryByteArrayEndpoint.class));
            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertArrayEquals("The received message is the same as the sent one", BINARY_MESSAGE,
                                     receivedMessageArray);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    private ByteBuffer receivedMessageBinary;

    @ServerEndpoint(value = "/endpointbinary")
    public static class EndpointBinaryPartialReturningValue {

        @OnMessage
        public ByteBuffer doThatBinary(Session s, ByteBuffer message, boolean last) throws IOException {
            return message;
        }
    }

    @Test
    public void binaryPartialHandlerReturningValue() throws DeploymentException {
        Server server = startServer(EndpointBinaryPartialReturningValue.class);

        try {
            messageLatch = new CountDownLatch(1);

            final ClientEndpointConfig clientConfiguration = ClientEndpointConfig.Builder.create().build();
            ClientManager client = createClient();

            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                            @Override
                            public void onMessage(ByteBuffer message) {
                                receivedMessageBinary = message;
                                messageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendBinary(ByteBuffer.wrap("TEST1".getBytes()), false);
                    } catch (IOException e) {
                        // do nothing.
                    }
                }
            }, clientConfiguration, getURI(EndpointBinaryPartialReturningValue.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals(0, messageLatch.getCount());
            Assert.assertEquals(ByteBuffer.wrap("TEST1".getBytes()), receivedMessageBinary);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint(value = "/endpoint21")
    public static class EndpointBinaryPartialReturningValueByteArray {

        @OnMessage
        public byte[] doThatBinary(Session s, byte[] message, boolean last) throws IOException {
            return message;
        }
    }

    @Test
    public void binaryPartialHandlerReturningValueByteArray() throws DeploymentException {
        Server server = startServer(EndpointBinaryPartialReturningValueByteArray.class);

        try {
            messageLatch = new CountDownLatch(1);

            final ClientEndpointConfig clientConfiguration = ClientEndpointConfig.Builder.create().build();
            ClientManager client = createClient();

            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                            @Override
                            public void onMessage(ByteBuffer message) {
                                receivedMessageBinary = message;
                                messageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendBinary(ByteBuffer.wrap("TEST1".getBytes()), false);
                    } catch (IOException e) {
                        // do nothing.
                    }
                }
            }, clientConfiguration, getURI(EndpointBinaryPartialReturningValueByteArray.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals(0, messageLatch.getCount());
            Assert.assertEquals(ByteBuffer.wrap("TEST1".getBytes()), receivedMessageBinary);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
