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
import java.net.URI;
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

import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class IPv6EchoTest extends TestContainer {

    @ServerEndpoint(value = "/ipv6-echo")
    public static class EchoEndpoint {
        @OnMessage
        public String onMessage(String message) {
            return message;
        }
    }

    @Test
    @Ignore("jdk@yosemite bug - does not correctly apply ::1 as non-proxiable host.")
    public void testIPv6() throws DeploymentException {
        Server server = startServer(EchoEndpoint.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);

        final URI uri = URI.create("ws://[::1]:8025/e2e-test/ipv6-echo");

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            createClient().connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            messageLatch.countDown();
                        }
                    });

                    try {
                        session.getBasicRemote().sendText("ipv6-echo");
                    } catch (IOException e) {
                        // do nothing.
                    }
                }
            }, cec, uri);

            assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
