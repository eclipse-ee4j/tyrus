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

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Petr Janouch
 */
public class ClientCloseTest extends TestContainer {

    @Test
    public void testEcho() throws IOException, DeploymentException, InterruptedException {
        final Server server = startServer(EchoEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(2);

        try {
            final ClientManager client1 = createClient();
            final ClientManager client2 = createClient();
            final Session session1 =
                    client1.connectToServer(createEndpoint(messageLatch), ClientEndpointConfig.Builder.create().build(),
                                            getURI(EchoEndpoint.class));
            final Session session2 =
                    client2.connectToServer(createEndpoint(messageLatch), ClientEndpointConfig.Builder.create().build(),
                                            getURI(EchoEndpoint.class));

            assertTrue(messageLatch.await(1, TimeUnit.SECONDS));
            session1.close();
            session2.getBasicRemote().sendText("msg");
            session2.close();

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    private Endpoint createEndpoint(final CountDownLatch messageLatch) {
        return new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig config) {
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String message) {
                        messageLatch.countDown();
                    }
                });
                try {
                    session.getBasicRemote().sendText("msg");
                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                }
            }
        };
    }

    @ServerEndpoint("/echo")
    public static class EchoEndpoint {
        @OnMessage
        public String onMessage(String message) {
            return message;
        }
    }
}
