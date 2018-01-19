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

package org.glassfish.tyrus.test.standard_config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
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
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class BinaryStreamDecoderTest extends TestContainer {
    public static class TestMessage {
        private final String message;

        public TestMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class BinaryStreamDecoder implements Decoder.BinaryStream<TestMessage> {

        @Override
        public TestMessage decode(InputStream is) throws DecodeException, IOException {
            int len = 0;
            int read = 0;
            byte[] buff = new byte[200];

            while (read != -1) {
                // 10 bytes per reader#read
                read = is.read(buff, len, 10);
                if (read != -1) {
                    len += read;
                }
            }

            return new TestMessage(new String(buff, 0, len, Charset.forName("UTF-8")));
        }

        @Override
        public void init(EndpointConfig config) {

        }

        @Override
        public void destroy() {

        }
    }

    @ServerEndpoint("/binaryStreamEndpointClient")
    public static class BinaryStreamClientEndpoint {

        @SuppressWarnings("UnusedParameters")
        @OnMessage
        public void onMessage(Session session, String message) throws IOException {
            final RemoteEndpoint.Basic basicRemote = session.getBasicRemote();
            basicRemote.sendBinary(ByteBuffer.wrap("test1".getBytes("UTF-8")), false);
            basicRemote.sendBinary(ByteBuffer.wrap("test2".getBytes("UTF-8")), false);
            basicRemote.sendBinary(ByteBuffer.wrap("test3".getBytes("UTF-8")), false);
            basicRemote.sendBinary(ByteBuffer.wrap("test4".getBytes("UTF-8")), false);
            basicRemote.sendBinary(ByteBuffer.wrap("test5".getBytes("UTF-8")), true);
        }
    }

    @ServerEndpoint(value = "/binaryStreamEndpointServer", decoders = {BinaryStreamDecoder.class})
    public static class BinaryStreamServerEndpoint {

        @SuppressWarnings("UnusedParameters")
        @OnMessage
        public String onMessage(Session session, TestMessage message) throws IOException {
            return message.getMessage();
        }
    }

    @Test
    public void testBinaryStreamDecoderClient() throws DeploymentException, IOException, InterruptedException {
        final Server server = startServer(BinaryStreamClientEndpoint.class);
        final CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            final ClientEndpointConfig build = ClientEndpointConfig.Builder.create().decoders(
                    Collections.<Class<? extends Decoder>>singletonList(BinaryStreamDecoder.class)).build();

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<TestMessage>() {
                        @Override
                        public void onMessage(TestMessage message) {
                            assertEquals("test1test2test3test4test5", message.getMessage());
                            messageLatch.countDown();
                        }
                    });

                    try {
                        session.getBasicRemote().sendText("start");
                    } catch (IOException e) {
                        // ignore.
                    }
                }
            }, build, getURI(BinaryStreamClientEndpoint.class));

            assertTrue(messageLatch.await(3, TimeUnit.SECONDS));
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testBinaryStreamDecoderServer() throws DeploymentException, IOException, InterruptedException {
        final Server server = startServer(BinaryStreamServerEndpoint.class);
        final CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            final ClientEndpointConfig build =
                    ClientEndpointConfig.Builder.create().decoders(
                            Collections.<Class<? extends Decoder>>singletonList(BinaryStreamDecoder.class)).build();

            final WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
            webSocketContainer.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            assertEquals("test1test2test3test4test5", message);
                            messageLatch.countDown();
                        }
                    });

                    try {
                        final RemoteEndpoint.Basic basicRemote = session.getBasicRemote();
                        basicRemote.sendBinary(ByteBuffer.wrap("test1".getBytes("UTF-8")), false);
                        basicRemote.sendBinary(ByteBuffer.wrap("test2".getBytes("UTF-8")), false);
                        basicRemote.sendBinary(ByteBuffer.wrap("test3".getBytes("UTF-8")), false);
                        basicRemote.sendBinary(ByteBuffer.wrap("test4".getBytes("UTF-8")), false);
                        basicRemote.sendBinary(ByteBuffer.wrap("test5".getBytes("UTF-8")), true);
                    } catch (IOException e) {
                        // ignore.
                    }
                }
            }, build, getURI(BinaryStreamServerEndpoint.class));

            assertTrue(messageLatch.await(3, TimeUnit.SECONDS));
        } finally {
            stopServer(server);
        }
    }
}
