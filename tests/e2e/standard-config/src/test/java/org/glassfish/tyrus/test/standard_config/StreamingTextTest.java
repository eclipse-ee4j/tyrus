/*
 * Copyright (c) 2012, 2021 Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the basic client behavior, sending and receiving message
 *
 * @author Danny Coward
 * @author Martin Matula
 */
public class StreamingTextTest extends TestContainer {

    @Test
    public void testClient() throws DeploymentException {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        Server server = startServer(StreamingTextEndpoint.class);

        try {
            CountDownLatch messageLatch = new CountDownLatch(1);

            StreamingTextClient stc = new StreamingTextClient(messageLatch);
            ClientManager client = createClient();
            client.connectToServer(stc, cec, getURI(StreamingTextEndpoint.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertTrue("The client did not get anything back", stc.gotSomethingBack);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    /**
     * @author Danny Coward
     * @author Martin Matula
     */
    @ServerEndpoint(value = "/streamingtext")
    public static class StreamingTextEndpoint {
        private Session session;

        @OnOpen
        public void onOpen(Session session) {
            System.out.println("STREAMINGSERVER opened !");
            this.session = session;

            session.addMessageHandler(new MessageHandler.Partial<String>() {
                StringBuilder sb = new StringBuilder();

                @Override
                public void onMessage(String text, boolean last) {
                    System.out.println("STREAMINGSERVER piece came: " + text);
                    sb.append(text);
                    if (last) {
                        System.out.println("STREAMINGSERVER whole message: " + sb.toString());
                        sb = new StringBuilder();
                    } else {
                        System.out.println("Resuming the client...");
                        synchronized (StreamingTextClient.class) {
                            StreamingTextClient.class.notify();
                        }
                    }
                }

            });

            try {
                System.out.println(session.getBasicRemote());
                sendPartial("thank ", false);
                sendPartial("you ", false);
                sendPartial("very ", false);
                sendPartial("much ", false);
                sendPartial("!", true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        private void sendPartial(String partialString, boolean isLast) throws IOException, InterruptedException {
            System.out.println("Server sending: " + partialString);
            session.getBasicRemote().sendText(partialString, isLast);
        }
    }

    /**
     * @author Danny Coward
     * @author Martin Matula
     */
    public static class StreamingTextClient extends Endpoint {
        boolean gotSomethingBack = false;
        private final CountDownLatch messageLatch;
        private Session session;

        public StreamingTextClient(CountDownLatch messageLatch) {
            this.messageLatch = messageLatch;
        }

        //    @Override
        //    public EndpointConfig getEndpointConfig() {
        //        return null;
        //    }

        @Override
        public void onOpen(Session session, EndpointConfig EndpointConfig) {
            System.out.println("STREAMINGCLIENT opened !");

            this.session = session;

            try {
                sendPartial("here", false);
                sendPartial("is ", false);
                sendPartial("a ", false);
                sendPartial("stream.", true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            session.addMessageHandler(new MessageHandler.Partial<String>() {
                StringBuilder sb = new StringBuilder();

                @Override
                public void onMessage(String text, boolean last) {
                    System.out.println("STREAMINGCLIENT piece came: " + text);
                    sb.append(text);
                    if (last) {
                        System.out.println("STREAMINGCLIENT received whole message: " + sb.toString());
                        sb = new StringBuilder();
                        gotSomethingBack = true;
                        messageLatch.countDown();
                    }
                }
            });
        }

        private void sendPartial(String partialString, boolean isLast) throws IOException, InterruptedException {
            System.out.println("Client sending: " + partialString);
            synchronized (StreamingTextClient.class) {
                session.getBasicRemote().sendText(partialString, isLast);
                if (!isLast) {
                    System.out.println("Waiting for the server to process the partial string...");
                    StreamingTextClient.class.wait(5000);
                }
            }
        }
    }
}
