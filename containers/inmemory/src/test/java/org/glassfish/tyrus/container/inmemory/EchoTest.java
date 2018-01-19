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

package org.glassfish.tyrus.container.inmemory;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.server.TyrusServerConfiguration;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class EchoTest {

    @Test
    public void testEcho() throws IOException, DeploymentException, InterruptedException {

        final CountDownLatch messageLatch = new CountDownLatch(1);

        // InMemory client container is automatically discovered
        final WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
        final ServerApplicationConfig serverConfig =
                new TyrusServerConfiguration(new HashSet<Class<?>>(Arrays.<Class<?>>asList(EchoEndpoint.class)),
                                             Collections.<ServerEndpointConfig>emptySet());

        ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        cec.getUserProperties().put(InMemoryClientContainer.SERVER_CONFIG, serverConfig);

        webSocketContainer.connectToServer(
                new Endpoint() {
                    @Override
                    public void onOpen(Session session, EndpointConfig config) {
                        try {
                            session.addMessageHandler(new MessageHandler.Whole<String>() {
                                @Override
                                public void onMessage(String message) {
                                    System.out.println("# client received: " + message);
                                    messageLatch.countDown();
                                }
                            });

                            session.getBasicRemote().sendText("in-memory echo!");
                            System.out.println("# client sent: in-memory echo!");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // "inmemory" acts here as a hostname, will be removed in InMemoryClientContainer.
                    }
                }, cec, URI.create("ws://inmemory/echo"));

        assertTrue(messageLatch.await(1, TimeUnit.SECONDS));
    }

    @ServerEndpoint("/echo")
    public static class EchoEndpoint {
        @OnMessage
        public String onMessage(String message) {
            System.out.println("# server echoed: " + message);
            return message;
        }
    }
}
