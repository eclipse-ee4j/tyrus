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

package org.glassfish.tyrus.test.e2e.non_deployable;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
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
 * Tests Grizzly context path with special cases.
 *
 * @author Petr Janouch
 */
public class GrizzlyContextPathTest extends TestContainer {

    /**
     * Sets context path to "" and tests that a request with URI "/a" will be passed to an endpoint registered on "/a".
     */
    @Test
    public void testEmptyContextPath() {
        testEcho("", "");
    }

    /**
     * Sets context path to "/" and tests that a request with URI "/a" will be passed to an endpoint registered on
     * "/a".
     */
    @Test
    public void testSlashContextPath() {
        testEcho("/", "");
    }

    private void testEcho(String serverContextPath, String requestContextPath) {
        Server server = null;
        try {
            setContextPath(serverContextPath);
            server = startServer(EchoEndpoint.class);
            final CountDownLatch messageLatch = new CountDownLatch(1);
            final ClientManager client = createClient();

            setContextPath(requestContextPath);
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {

                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                messageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText("Hello");
                    } catch (IOException e) {
                        // do nothing
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(), getURI(EchoEndpoint.class));

            assertTrue(messageLatch.await(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint("/echo")
    public static class EchoEndpoint {

        @OnMessage
        public String echo(Session session, String message) throws IOException {
            return message;
        }
    }
}
