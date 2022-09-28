/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;
import org.junit.Test;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class QueryParamTest extends TestContainer {

    public static final String QUERY_NAME = "NAME";
    public static final String QUERY_VALUE = "value10%&value2%";

    @ServerEndpoint(value = "/query")
    public static class QueryEndpoint {

        @OnMessage
        public void onMessage(Session session, String message) throws IOException {
            session.getBasicRemote().sendText(session.getRequestParameterMap().get(QUERY_NAME).get(0));
        }
    }

    @Test
    public void testQueryClient() throws DeploymentException {
        Server server = startServer(QueryEndpoint.class);
        try {
            testQueryClient(false);
            testQueryClient(true);
        } finally {
            stopServer(server);
        }
    }

    public void testQueryClient(boolean fromString) throws DeploymentException {
        AtomicReference<String> response = new AtomicReference<>();

        try {
            final CountDownLatch messageLatch = new CountDownLatch(1);

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                response.set(message);
                                messageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText("Hello");

                    } catch (IOException e) {
                        // do nothing.
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(), getURI(QueryEndpoint.class, "ws", fromString));

            messageLatch.await(1, TimeUnit.SECONDS);
            assertEquals(0, messageLatch.getCount());
            assertEquals(QUERY_VALUE, response.get());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected URI getURI(Class<?> serverClass, String scheme, boolean fromString) {
        URI uri = super.getURI(serverClass, scheme);
        URI withQuery = null;
        try {
            if (fromString) {
                withQuery = new URI(uri.toASCIIString() + "?" + QUERY_NAME + "=" + URLEncoder.encode(QUERY_VALUE, "UTF-8"));
            } else {
                withQuery = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(),
                        QUERY_NAME + "=" + URLEncoder.encode(QUERY_VALUE, "UTF-8"), uri.getFragment());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return withQuery;
    }
}
