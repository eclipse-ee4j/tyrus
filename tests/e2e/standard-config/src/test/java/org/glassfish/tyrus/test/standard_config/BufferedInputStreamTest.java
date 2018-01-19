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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * Tests the BufferedInputStream and bug fix TYRUS-274
 * <p/>
 * Client opens DataOutputStream to write int and server uses DataInputStream to read int and verify the message
 *
 * @author Raghuram Krishnamchari (raghuramcbz at gmail.com) jira/github user: raghucbz
 */
public class BufferedInputStreamTest extends TestContainer {

    @Test
    public void testClient() throws DeploymentException {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        Server server = startServer(BufferedInputStreamEndpoint.class);

        try {
            final CountDownLatch messageLatch = new CountDownLatch(1);
            BufferedInputStreamClient bisc = new BufferedInputStreamClient(messageLatch);
            ClientManager client = createClient();
            client.connectToServer(bisc, cec, getURI(BufferedInputStreamEndpoint.class));

            assertTrue(messageLatch.await(3, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    /**
     * BufferedInputStream Server Endpoint.
     *
     * @author Raghuram Krishnamchari (raghuramcbz at gmail.com) jira/github user: raghucbz
     */
    @ServerEndpoint(value = "/bufferedinputstreamserver")
    public static class BufferedInputStreamEndpoint {

        public static int MESSAGE = 1234;

        @OnOpen
        public void init() {
            System.out.println("BufferedInputStreamServer opened");
        }

        @OnMessage
        public void onMessage(Session session, InputStream inputStream) {
            System.out.println("BufferedInputStreamServer got message: " + inputStream);
            try {
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                int messageReceived = dataInputStream.readInt();

                // assertTrue("Server did not get the right message: " + messageReceived, messageReceived ==
                // BufferedInputStreamTest.MESSAGE);
                if (messageReceived == BufferedInputStreamEndpoint.MESSAGE) {
                    System.out.println("Server successfully got message: " + messageReceived);
                    session.getBasicRemote().sendText("ok");
                }
            } catch (Exception e) {
                System.out.println("BufferedInputStreamServer exception: " + e);
                e.printStackTrace();
            }
        }
    }

    /**
     * BufferedInputStream Client Endpoint.
     *
     * @author Raghuram Krishnamchari (raghuramcbz at gmail.com) jira/github user: raghucbz
     */
    public class BufferedInputStreamClient extends Endpoint {

        private final CountDownLatch messageLatch;

        public BufferedInputStreamClient(CountDownLatch messageLatch) {
            this.messageLatch = messageLatch;
        }

        @Override
        public void onOpen(Session session, EndpointConfig EndpointConfig) {
            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    if (message.equals("ok")) {
                        messageLatch.countDown();
                    }
                }
            });

            System.out.println("BufferedInputStreamClient opened !!");
            try {
                OutputStream outputStream = session.getBasicRemote().getSendStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                dataOutputStream.writeInt(BufferedInputStreamEndpoint.MESSAGE);
                dataOutputStream.close();
                System.out.println("## BufferedInputStreamClient - binary message sent");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(Session session, Throwable thr) {
            thr.printStackTrace();
        }
    }
}
