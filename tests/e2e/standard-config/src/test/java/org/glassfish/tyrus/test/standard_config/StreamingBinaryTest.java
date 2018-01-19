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

package org.glassfish.tyrus.test.standard_config;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the basic client behavior, sending and receiving message
 *
 * @author Danny Coward (danny.coward at oracle.com)
 */
public class StreamingBinaryTest extends TestContainer {

    @Test
    public void testClient() throws DeploymentException {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        Server server = startServer(StreamingBinaryEndpoint.class);

        try {
            CountDownLatch messageLatch = new CountDownLatch(1);

            StreamingBinaryClient sbc = new StreamingBinaryClient(messageLatch);
            ClientManager client = createClient();
            client.connectToServer(sbc, cec, getURI(StreamingBinaryEndpoint.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertTrue("The client got an echo back of what it streamed", sbc.gotTheSameThingBack);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    /**
     * @author Danny Coward (danny.coward at oracle.com)
     * @author Martin Matula (martin.matula at oracle.com)
     */
    @ServerEndpoint(value = "/streamingbinary")
    public static class StreamingBinaryEndpoint {
        private Session session;
        private List<String> messages = new ArrayList<String>();

        @OnOpen
        public void onOpen(Session session) {
            System.out.println("STREAMINGBSERVER opened !");
            this.session = session;

            session.addMessageHandler(new MessageHandler.Partial<ByteBuffer>() {
                StringBuilder sb = new StringBuilder();

                @Override
                public void onMessage(ByteBuffer bb, boolean last) {
                    System.out.println("STREAMINGBSERVER piece came: " + new String(bb.array()));
                    sb.append(new String(bb.array()));
                    messages.add(new String(bb.array()));
                    if (last) {
                        System.out.println("STREAMINGBSERVER whole message: " + sb.toString());
                        sb = new StringBuilder();
                        reply();
                    }
                }
            });


        }

        public void reply() {
            try {
                sendPartial(ByteBuffer.wrap(messages.get(0).getBytes()), false);
                sendPartial(ByteBuffer.wrap(messages.get(1).getBytes()), false);
                sendPartial(ByteBuffer.wrap(messages.get(2).getBytes()), false);
                sendPartial(ByteBuffer.wrap(messages.get(3).getBytes()), true);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void sendPartial(ByteBuffer bb, boolean isLast) throws IOException, InterruptedException {
            System.out.println("STREAMINGBSERVER Server sending: " + new String(bb.array()));
            session.getBasicRemote().sendBinary(bb, isLast);
        }
    }

    /**
     * @author Danny Coward (danny.coward at oracle.com)
     */
    public static class StreamingBinaryClient extends Endpoint {
        boolean gotTheSameThingBack = false;
        private final CountDownLatch messageLatch;
        private Session session;
        static String MESSAGE_0 = "here ";
        static String MESSAGE_1 = "is ";
        static String MESSAGE_2 = "a ";
        static String MESSAGE_3 = "string ! ";

        public StreamingBinaryClient(CountDownLatch messageLatch) {
            this.messageLatch = messageLatch;
        }

        //    @Override
        //    public EndpointConfig getEndpointConfig() {
        //        return null;
        //    }

        @Override
        public void onOpen(Session session, EndpointConfig EndpointConfig) {

            System.out.println("STREAMINGBCLIENT opened !");

            this.session = session;

            session.addMessageHandler(new MessageHandler.Partial<ByteBuffer>() {
                StringBuilder sb = new StringBuilder();

                @Override
                public void onMessage(ByteBuffer bb, boolean last) {
                    System.out.println("STREAMINGBCLIENT piece came: " + new String(bb.array()));
                    sb.append(new String(bb.array()));
                    if (last) {
                        gotTheSameThingBack = sb.toString().equals(MESSAGE_0 + MESSAGE_1 + MESSAGE_2 + MESSAGE_3);
                        System.out.println("STREAMINGBCLIENT received whole message: " + sb);
                        sb = new StringBuilder();
                        messageLatch.countDown();
                    }
                }
            });

            try {
                sendPartial(MESSAGE_0, false);
                sendPartial(MESSAGE_1, false);
                sendPartial(MESSAGE_2, false);
                sendPartial(MESSAGE_3, true);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private void sendPartial(String partialString, boolean isLast) throws IOException, InterruptedException {
            System.out.println("STREAMINGBCLIENT Client sending: " + partialString + " " + isLast);
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(partialString.getBytes()), isLast);
        }
    }
}
