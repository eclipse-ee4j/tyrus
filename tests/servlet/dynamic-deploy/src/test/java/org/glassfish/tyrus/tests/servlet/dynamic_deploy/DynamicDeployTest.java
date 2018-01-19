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

package org.glassfish.tyrus.tests.servlet.dynamic_deploy;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class DynamicDeployTest extends TestContainer {

    private static final String CONTEXT_PATH = "/dynamic-deploy";

    public DynamicDeployTest() {
        setContextPath(CONTEXT_PATH);
    }

    @Test
    public void testEcho() throws DeploymentException, InterruptedException, IOException, URISyntaxException {
        if (System.getProperty("tyrus.test.host") == null) {
            return;
        }

        final CountDownLatch messageLatch = new CountDownLatch(1);

        URI uri = getURI(EchoEndpoint.class.getAnnotation(ServerEndpoint.class).value());
        // Test for TYRUS-187; works only in servlet case, thus test is here.
        uri = new URI(uri.toString() + "?myParam=myValue");


        final ClientManager client = createClient();
        client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig EndpointConfig) {

                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            assertEquals(message, "Do or do not, there is no try.");
                            messageLatch.countDown();
                        }
                    });

                    session.getBasicRemote().sendText("Do or do not, there is no try.");
                } catch (IOException e) {
                    // do nothing
                }
            }
        }, ClientEndpointConfig.Builder.create().build(), uri);

        messageLatch.await(1, TimeUnit.SECONDS);
        assertEquals(0, messageLatch.getCount());

    }

    @Test
    public void testAnnotated() throws DeploymentException, InterruptedException, IOException {
        if (System.getProperty("tyrus.test.host") == null) {
            return;
        }

        final CountDownLatch messageLatch = new CountDownLatch(1);

        final ClientManager client = createClient();
        client.connectToServer(
                new Endpoint() {
                    @Override
                    public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        try {
                            session.addMessageHandler(new MessageHandler.Whole<String>() {
                                @Override
                                public void onMessage(String message) {
                                    assertEquals(message, "Do or do not, there is no try.");
                                    messageLatch.countDown();
                                }
                            });

                            session.getBasicRemote().sendText("Do or do not, there is no try.");
                        } catch (IOException e) {
                            // do nothing
                        }
                    }
                }, ClientEndpointConfig.Builder.create().build(),
                getURI(MyServletContextListenerAnnotated.class.getAnnotation(ServerEndpoint.class).value()));

        messageLatch.await(1, TimeUnit.SECONDS);
        assertEquals(0, messageLatch.getCount());

    }

    @Test
    public void testProgrammatic() throws DeploymentException, InterruptedException, IOException {
        if (System.getProperty("tyrus.test.host") == null) {
            return;
        }

        final CountDownLatch messageLatch = new CountDownLatch(1);

        final ClientManager client = createClient();
        client.connectToServer(
                new Endpoint() {
                    @Override
                    public void onOpen(Session session, EndpointConfig EndpointConfig) {
                        try {
                            session.addMessageHandler(new MessageHandler.Whole<String>() {
                                @Override
                                public void onMessage(String message) {
                                    assertEquals(message, "Do or do not, there is no try.");
                                    messageLatch.countDown();
                                }
                            });

                            // TODO: remove when TYRUS-108 is resolved.
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            session.getBasicRemote().sendText("Do or do not, there is no try.");
                        } catch (IOException e) {
                            // do nothing
                        }
                    }
                }, ClientEndpointConfig.Builder.create().build(), getURI("/programmatic"));

        messageLatch.await(100, TimeUnit.SECONDS);
        assertEquals(0, messageLatch.getCount());

    }
}
