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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.standard_config.bean.TestEndpoint;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests whether the HandShake parameters (sub-protocols, extensions) are sent correctly.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class HandshakeTest extends TestContainer {

    private CountDownLatch messageLatch;

    private String receivedMessage;

    private static final String SENT_MESSAGE = "hello";

    public static class MyClientEndpointConfigurator extends ClientEndpointConfig.Configurator {

        public HandshakeResponse hr;

        @Override
        public void afterResponse(HandshakeResponse hr) {
            this.hr = hr;
        }
    }

    @Test
    public void testClient() throws DeploymentException {
        Server server = startServer(TestEndpoint.class);

        try {
            messageLatch = new CountDownLatch(1);

            ArrayList<String> subprotocols = new ArrayList<String>();
            subprotocols.add("asd");
            subprotocols.add("ghi");

            final MyClientEndpointConfigurator myClientEndpointConfigurator = new MyClientEndpointConfigurator();

            final ClientEndpointConfig cec =
                    ClientEndpointConfig.Builder.create().preferredSubprotocols(subprotocols)
                                                .configurator(myClientEndpointConfigurator).build();

            ClientManager client = createClient();
            client.connectToServer(new TestEndpointAdapter() {
                @Override
                public void onMessage(String message) {
                    receivedMessage = message;
                    messageLatch.countDown();
                    System.out.println("Received message = " + message);
                }

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }

                @Override
                public void onOpen(Session session) {
                    try {
                        session.addMessageHandler(new TestTextMessageHandler(this));
                        session.getBasicRemote().sendText(SENT_MESSAGE);
                        System.out.println("Sent message: " + SENT_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI(TestEndpoint.class));

            messageLatch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals(SENT_MESSAGE, receivedMessage);

            Map<String, List<String>> headers = myClientEndpointConfigurator.hr.getHeaders();

            String supportedSubprotocol = headers.get(HandshakeRequest.SEC_WEBSOCKET_PROTOCOL).get(0);
            Assert.assertEquals("asd", supportedSubprotocol);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
