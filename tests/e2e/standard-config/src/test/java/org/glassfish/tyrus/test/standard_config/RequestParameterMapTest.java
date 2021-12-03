/*
 * Copyright (c) 2014, 2021 Oracle and/or its affiliates. All rights reserved.
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
import java.net.URI;
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

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek
 */
public class RequestParameterMapTest extends TestContainer {

    @Test
    public void testSingleQueryParam() throws DeploymentException {
        Server server = startServer(RequestParameterMapEndpoint.class);
        final CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            if (message.equals("paramValue")) {
                                messageLatch.countDown();
                            }
                        }
                    });
                }

            }, cec, URI.create(getURI(RequestParameterMapEndpoint.class).toString() + "?test=paramValue"));

            assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint(value = "/requestParameterMapEndpoint")
    public static class RequestParameterMapEndpoint {

        @OnOpen
        public void onOpen(Session session) {
            String test = session.getRequestParameterMap().get("test").get(0);
            try {
                session.getBasicRemote().sendText(test);
            } catch (IOException e) {
                // do nothing.
            }
        }
    }
}
