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
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.standard_config.bean.TestEndpoint;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Tests the basic client behavior, sending and receiving message.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class ClientTest extends TestContainer {

    private CountDownLatch messageLatch;

    private String receivedMessage;

    private static Session session;
    private static final String SENT_MESSAGE = "hello";

    @Test
    public void testClient() throws URISyntaxException, InterruptedException, IOException, DeploymentException {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        Server server = startServer(TestEndpoint.class);

        try {
            messageLatch = new CountDownLatch(1);

            ClientManager client = createClient();
            final Session clientSession = client.connectToServer(new TestEndpointAdapter() {
                private final ClientEndpointConfig configuration = ClientEndpointConfig.Builder.create().build();

                @Override
                public void onMessage(String message) {
                    receivedMessage = message;
                    messageLatch.countDown();
                    System.out.println("Received message = " + message);
                }

                @Override
                public EndpointConfig getEndpointConfig() {
                    return configuration;
                }

                @Override
                public void onOpen(Session session) {
                    ClientTest.session = session;
                    try {
                        session.addMessageHandler(new TestTextMessageHandler(this));
                        session.getBasicRemote().sendText(SENT_MESSAGE);
                        System.out.println("Sent message: " + SENT_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(TestEndpoint.class));

            assertEquals(getURI(TestEndpoint.class), clientSession.getRequestURI());

            messageLatch.await(5, TimeUnit.SECONDS);

            assertEquals(clientSession, ClientTest.session);
            assertEquals(SENT_MESSAGE, receivedMessage);
        } finally {
            stopServer(server);
        }
    }
}
