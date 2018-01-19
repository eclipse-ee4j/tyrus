/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.HandshakeException;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class TestHello404 extends TestContainer {

    private static final String SENT_MESSAGE = "Hello World";

    @Test
    public void testHello404() throws DeploymentException {
        Server server = startServer(EchoEndpoint.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                            }
                        });
                        session.getBasicRemote().sendText(SENT_MESSAGE);
                        System.out.println("Hello message sent.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, cec, getURI("invalid-endpoint-path", "ws"));
            Assert.fail();
        } catch (Exception e) {
            assertNotNull(e);
            assertTrue(e instanceof DeploymentException);
            assertTrue(e.getCause() instanceof HandshakeException);
            assertEquals(404, ((HandshakeException) e.getCause()).getHttpStatusCode());
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint(value = "/test-hello-404-echo-endpoint")
    public static class EchoEndpoint {

        @OnMessage
        public String doThat(String message, Session session) {

            // TYRUS-141
            if (session.getNegotiatedSubprotocol() != null) {
                return message;
            }

            return null;
        }
    }
}
