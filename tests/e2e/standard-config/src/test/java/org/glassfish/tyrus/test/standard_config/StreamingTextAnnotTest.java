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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.OnMessage;
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
public class StreamingTextAnnotTest extends TestContainer {

    @Test
    public void testClient() throws DeploymentException {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        Server server = startServer(StreamingTextAnnotEndpoint.class);

        try {
            CountDownLatch messageLatch = new CountDownLatch(1);

            StreamingTextTest.StreamingTextClient stc = new StreamingTextTest.StreamingTextClient(messageLatch);
            ClientManager client = createClient();
            client.connectToServer(stc, cec, getURI(StreamingTextAnnotEndpoint.class));

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
     * @author Martin Matula (martin.matula at oracle.com)
     */
    @ServerEndpoint(value = "/streamingtext1")
    public static class StreamingTextAnnotEndpoint {
        private Session session;
        private StringBuilder sb = new StringBuilder();

        @OnOpen
        public void onOpen(Session session) {
            System.out.println("STREAMINGSERVER opened !");
            this.session = session;
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

        @OnMessage
        public void onMessage(String text, boolean last) {
            System.out.println("STREAMINGSERVER piece came: " + text);
            sb.append(text);
            if (last) {
                System.out.println("STREAMINGSERVER whole message: " + sb.toString());
                sb = new StringBuilder();
            } else {
                System.out.println("Resuming the client...");
                synchronized (StreamingTextTest.StreamingTextClient.class) {
                    StreamingTextTest.StreamingTextClient.class.notify();
                }
            }
        }

        private void sendPartial(String partialString, boolean isLast) throws IOException, InterruptedException {
            System.out.println("Server sending: " + partialString);
            session.getBasicRemote().sendText(partialString, isLast);
        }
    }
}
