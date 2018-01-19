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
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
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
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class BlockingStreamingTextTest extends TestContainer {

    @Test
    public void testBlockingStreamingTextServer() {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

        Server server = null;

        try {
            server = startServer(BlockingStreamingTextEndpoint.class);
            CountDownLatch messageLatch = new CountDownLatch(1);

            BlockingStreamingTextClient bstc = new BlockingStreamingTextClient(messageLatch);
            ClientManager client = createClient();
            client.connectToServer(bstc, cec, getURI(BlockingStreamingTextEndpoint.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            System.out.println("SENT: " + bstc.sentMessage);
            System.out.println("RECEIVED: " + bstc.receivedMessage);
            Assert.assertTrue("Client got back what it sent, all pieces in the right order.",
                              bstc.sentMessage.equals(bstc.receivedMessage));
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
    @ServerEndpoint(value = "/blocking-streaming")
    public static class BlockingStreamingTextEndpoint {
        class MyCharacterStreamHandler implements MessageHandler.Whole<Reader> {
            Session session;

            MyCharacterStreamHandler(Session session) {
                this.session = session;
            }

            @Override
            public void onMessage(Reader r) {
                System.out.println("BLOCKINGSTREAMSERVER: on message reader called");
                StringBuilder sb = new StringBuilder();
                try {
                    int i;
                    while ((i = r.read()) != -1) {
                        sb.append((char) i);
                    }
                    r.close();

                    String receivedMessage = sb.toString();
                    System.out.println("BLOCKINGSTREAMSERVER received: " + receivedMessage);

                    Writer w = session.getBasicRemote().getSendWriter();
                    w.write(receivedMessage.substring(0, 4));
                    w.write(receivedMessage.substring(4, receivedMessage.length()));
                    w.close();
                    System.out.println("BLOCKINGSTREAMSERVER sent back: " + receivedMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @OnOpen
        public void onOpen(final Session session) {
            System.out.println("BLOCKINGSERVER opened !");
            session.addMessageHandler(new MyCharacterStreamHandler(session));
        }

    }

    /**
     * @author Danny Coward (danny.coward at oracle.com)
     */
    public static class BlockingStreamingTextClient extends Endpoint {
        String receivedMessage;
        private final CountDownLatch messageLatch;
        String sentMessage;

        public BlockingStreamingTextClient(CountDownLatch messageLatch) {
            this.messageLatch = messageLatch;
        }

        @Override
        public void onOpen(Session session, EndpointConfig EndpointConfig) {
            System.out.println("BLOCKINGCLIENT opened !");

            send(session);

            session.addMessageHandler(new MessageHandler.Whole<Reader>() {
                final StringBuilder sb = new StringBuilder();

                @Override
                public void onMessage(Reader r) {
                    System.out.println("BLOCKINGCLIENT onMessage called ");
                    try {
                        int i;
                        while ((i = r.read()) != -1) {
                            sb.append((char) i);
                        }
                        receivedMessage = sb.toString();
                        System.out.println("BLOCKINGCLIENT received: " + receivedMessage);
                        messageLatch.countDown();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }


                }
            });
        }

        public void send(Session session) {
            try {
                StringBuilder sb = new StringBuilder();
                String part;
                for (int i = 0; i < 10; i++) {
                    part = "blk" + i;
                    session.getBasicRemote().sendText(part, false);
                    sb.append(part);
                }
                part = "END";
                session.getBasicRemote().sendText(part, true);
                sb.append(part);
                sentMessage = sb.toString();
                System.out.println("BLOCKINGCLIENT Sent: " + sentMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
