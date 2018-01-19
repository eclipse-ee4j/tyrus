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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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
 * Tests the basic client behavior, sending and receiving binary messages
 *
 * @author Danny Coward (danny.coward at oracle.com)
 */
public class BlockingBinaryTest extends TestContainer {

    @Test
    public void testClient() throws DeploymentException {

        Server server = startServer(BlockingBinaryEndpoint.class);

        try {
            CountDownLatch messageLatch = new CountDownLatch(1);

            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            BlockingBinaryClient sbc = new BlockingBinaryClient(messageLatch);
            ClientManager client = createClient();
            client.connectToServer(sbc, cec, getURI(BlockingBinaryEndpoint.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertTrue("Client did not receive anything.", sbc.gotTheSameThingBack);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    /**
     * @author Danny Coward (danny.coward at oracle.com)
     */
    @ServerEndpoint(value = "/blockingbinary")
    public static class BlockingBinaryEndpoint extends Endpoint {
        private Session session;
        private String message;

        @Override
        @OnOpen
        public void onOpen(Session session, EndpointConfig EndpointConfig) {
            System.out.println("BLOCKINGBSERVER opened !");
            this.session = session;

            session.addMessageHandler(new MessageHandler.Whole<InputStream>() {
                final StringBuilder sb = new StringBuilder();

                @Override
                public void onMessage(InputStream is) {
                    try {
                        int i;
                        while ((i = is.read()) != -1) {
                            System.out.println("BLOCKINGBSERVER read " + (char) i + " from the input stream.");
                            sb.append((char) i);
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                    System.out.println("BLOCKINGBSERVER read " + sb + " from the input stream.");
                    message = sb.toString();
                    reply();
                }
            });
        }

        public void reply() {
            System.out.println("BLOCKINGBSERVER replying");
            try {
                OutputStream os = session.getBasicRemote().getSendStream();
                os.write(message.getBytes());
                os.close();
                System.out.println("BLOCKINGBSERVER replied");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /**
     * @author Danny Coward (danny.coward at oracle.com)
     */
    public static class BlockingBinaryClient extends Endpoint {
        volatile boolean gotTheSameThingBack = false;
        private final CountDownLatch messageLatch;
        private Session session;
        static String MESSAGE_0 = "here ";
        static String MESSAGE_1 = "is ";
        static String MESSAGE_2 = "a ";
        static String MESSAGE_3 = "string ! ";

        public BlockingBinaryClient(CountDownLatch messageLatch) {
            this.messageLatch = messageLatch;
        }

        @Override
        public void onOpen(Session session, EndpointConfig EndpointConfig) {

            System.out.println("BLOCKINGBCLIENT opened !");

            this.session = session;

            session.addMessageHandler(new MessageHandler.Whole<InputStream>() {
                StringBuilder sb = new StringBuilder();

                @Override
                public void onMessage(InputStream is) {
                    int i;

                    try {
                        while ((i = is.read()) != -1) {
                            sb.append((char) i);
                        }

                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }


                    gotTheSameThingBack = sb.toString().equals(MESSAGE_0 + MESSAGE_1 + MESSAGE_2 + MESSAGE_3);
                    if (!gotTheSameThingBack) {
                        System.out.println("### error: " + sb.toString());
                        System.out.println("### error: " + MESSAGE_0 + MESSAGE_1 + MESSAGE_2 + MESSAGE_3);
                    }
                    System.out.println("BLOCKINGBCLIENT received whole message:" + sb.toString());
                    sb = new StringBuilder();
                    messageLatch.countDown();

                }
            });

            try {
                System.out.println("BLOCKINGBCLIENT Client sending data to the blocking output stream. ");
                OutputStream os = session.getBasicRemote().getSendStream();

                os.write(MESSAGE_0.getBytes());
                os.write(MESSAGE_1.getBytes());
                os.write(MESSAGE_2.getBytes());
                os.write(MESSAGE_3.getBytes());
                os.close();

                System.out.println("### BLOCKINGBCLIENT stream closed");
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private void sendPartial(String partialString, boolean isLast) throws IOException, InterruptedException {
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(partialString.getBytes()), isLast);
        }
    }
}
