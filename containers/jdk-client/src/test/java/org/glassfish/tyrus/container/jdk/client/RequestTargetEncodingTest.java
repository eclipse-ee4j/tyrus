/*
 * Copyright (c) 2026 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.container.jdk.client;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Percent-encoded characters in the path and query of the endpoint URI must stay encoded in the upgrade request line.
 */
public class RequestTargetEncodingTest extends TestContainer {

    private static final String VALUE = "a%41 b\r\nX-Injected: yes";

    @ServerEndpoint(value = "/requestTarget/{param}")
    public static class RequestTargetEndpoint {

        @OnOpen
        public void onOpen(Session session, @PathParam("param") String param) throws IOException {
            session.getBasicRemote().sendText(param + "|" + session.getRequestParameterMap().get("q").get(0));
        }
    }

    @Test
    public void testEncodedRequestTarget() throws Exception {
        Server server = startServer(RequestTargetEndpoint.class);
        try {
            final CountDownLatch messageLatch = new CountDownLatch(1);
            final AtomicReference<String> response = new AtomicReference<String>();

            URI base = getURI("/requestTarget/" + VALUE);
            URI uri = new URI(base.getScheme(), null, base.getHost(), base.getPort(), base.getPath(), "q=" + VALUE,
                              null);

            ClientManager client = ClientManager.createClient(JdkClientContainer.class.getName());
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            response.set(message);
                            messageLatch.countDown();
                        }
                    });
                }
            }, ClientEndpointConfig.Builder.create().build(), uri);

            assertTrue(messageLatch.await(3, TimeUnit.SECONDS));
            assertEquals(VALUE + "|" + VALUE, response.get());
        } finally {
            stopServer(server);
        }
    }
}
