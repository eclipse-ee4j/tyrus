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
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class PartialBinaryTest extends TestContainer {

    private CountDownLatch messageLatch;
    private static final byte[] BINARY_MESSAGE_1 = new byte[]{1, 2};
    private static final byte[] BINARY_MESSAGE_2 = new byte[]{3, 4};
    private String receivedMessage;

    private final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

    @ServerEndpoint(value = "/partialbinary")
    public static class WSByteArrayPartialAndSessionServer {

        StringBuffer sb = new StringBuffer();
        int messageCounter = 0;

        @OnMessage
        public void bytesToString(byte[] array, Session s, boolean finito) throws IOException {
            messageCounter++;
            sb.append(new String(array));
            if (messageCounter == 2) {
                s.getBasicRemote().sendText(sb.toString());
                sb = new StringBuffer();
            }
        }
    }

    @Test
    public void testBinaryByteArrayBean() throws DeploymentException {
        Server server = startServer(WSByteArrayPartialAndSessionServer.class);

        try {
            messageLatch = new CountDownLatch(1);

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.getBasicRemote().sendBinary(ByteBuffer.wrap(BINARY_MESSAGE_1), false);
                        session.getBasicRemote().sendBinary(ByteBuffer.wrap(BINARY_MESSAGE_2), false);
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String data) {
                                receivedMessage = data;
                                messageLatch.countDown();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(WSByteArrayPartialAndSessionServer.class));
            messageLatch.await(2, TimeUnit.SECONDS);
            Assert.assertEquals("The received message is the same as the sent one",
                                new String(BINARY_MESSAGE_1) + new String(BINARY_MESSAGE_2), receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint(value = "/partialbinary2")
    public static class WSByteBufferPartialAndSessionServer {

        StringBuffer sb = new StringBuffer();
        int messageCounter = 0;

        @OnMessage
        public void bytesToString(ByteBuffer buffer, Session s, boolean finito) throws IOException {
            messageCounter++;
            sb.append(new String(buffer.array()));
            if (messageCounter == 2) {
                s.getBasicRemote().sendText(sb.toString());
                sb = new StringBuffer();
            }
        }
    }

    @Test
    public void testBinaryByteBufferBean() throws DeploymentException {
        Server server = startServer(WSByteBufferPartialAndSessionServer.class);

        try {
            messageLatch = new CountDownLatch(1);

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.getBasicRemote().sendBinary(ByteBuffer.wrap(BINARY_MESSAGE_1), false);
                        session.getBasicRemote().sendBinary(ByteBuffer.wrap(BINARY_MESSAGE_2), false);
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String data) {
                                receivedMessage = data;
                                messageLatch.countDown();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(WSByteBufferPartialAndSessionServer.class));
            messageLatch.await(2, TimeUnit.SECONDS);
            Assert.assertEquals("The received message is the same as the sent one",
                                new String(BINARY_MESSAGE_1) + new String(BINARY_MESSAGE_2), receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
