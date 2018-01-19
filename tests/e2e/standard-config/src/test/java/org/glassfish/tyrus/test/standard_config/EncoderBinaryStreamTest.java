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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.coder.CoderAdapter;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class EncoderBinaryStreamTest extends TestContainer {

    private CountDownLatch messageLatch;

    /**
     * Exception thrown during execution @OnOpen annotated method.
     *
     * @author Danny Coward (danny.coward at oracle.com)
     */
    @ServerEndpoint(value = "/apple", encoders = {BinaryStreamEncoder.class})
    public static class OnOpenErrorTestEndpoint {
        @OnMessage
        public Apple message(String message, Session session) {
            return new Apple();
        }
    }

    public static class Apple {

    }

    public static class BinaryStreamEncoder extends CoderAdapter implements Encoder.BinaryStream<Apple> {
        @Override
        public void encode(Apple object, OutputStream os) throws EncodeException, IOException {
            os.write("apple".getBytes());
        }
    }

    @Test
    public void testBinaryStreamEncoder() throws DeploymentException {
        Server server = startServer(OnOpenErrorTestEndpoint.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            messageLatch = new CountDownLatch(1);
            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                        @Override
                        public void onMessage(ByteBuffer message) {
                            if ("apple".equals(new String(message.array()))) {
                                messageLatch.countDown();
                            }

                        }
                    });
                    try {
                        session.getBasicRemote().sendText("something");
                    } catch (IOException e) {

                    }
                }
            }, cec, getURI(OnOpenErrorTestEndpoint.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
