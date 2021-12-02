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

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * Tests the basic client behavior, sending and receiving message
 *
 * @author Danny Coward
 */
public class HelloBinaryTest extends TestContainer {

    @Test
    public void testClient() throws DeploymentException {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        Server server = startServer(HelloBinaryEndpoint.class);

        try {
            CountDownLatch messageLatch = new CountDownLatch(1);

            HelloBinaryClient htc = new HelloBinaryClient(messageLatch);
            ClientManager client = createClient();
            client.connectToServer(htc, cec, getURI(HelloBinaryEndpoint.class));

            assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
            assertTrue("The client got the same thing back", htc.echoWorked);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    /**
     * @author Danny Coward
     */

    @ServerEndpoint(value = "/hellobinary")
    public static class HelloBinaryEndpoint {

        @OnOpen
        public void init(Session session) {
            System.out.println("HELLOBSERVER opened");
            session.addMessageHandler(new MyMessageHandler(session));
        }

        class MyMessageHandler implements MessageHandler.Whole<ByteBuffer> {
            private Session session;

            MyMessageHandler(Session session) {
                this.session = session;
            }

            @Override
            public void onMessage(ByteBuffer message) {
                System.out.println("HELLOBSERVER got  message: " + message);
                try {
                    session.getBasicRemote().sendBinary(message);
                    System.out.println("### HELLOBSERVER sent message: " + message);
                } catch (Exception e) {

                }
            }

        }

    }
}
