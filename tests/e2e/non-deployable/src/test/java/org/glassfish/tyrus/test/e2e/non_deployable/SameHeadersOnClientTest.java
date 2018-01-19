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
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.spi.UpgradeRequest;
import org.glassfish.tyrus.spi.UpgradeResponse;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests a situation when a handshake response contains the same header twice with different values. Cannot be moved to
 * standard tests due the need to modify HTTP response.
 *
 * @author Petr Janouch
 */
public class SameHeadersOnClientTest extends TestContainer {

    private static final String HEADER_KEY = "my-header";
    private static final String HEADER_VALUE_1 = "my-header-value-1";
    private static final String HEADER_VALUE_2 = "my-header-value-2";

    @Test
    public void testSameHeaderNamesInHandshakeResponse() {
        HttpServer server = null;
        try {
            server = getHandshakeServer();

            final CountDownLatch responseLatch = new CountDownLatch(1);

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig endpointConfig) {
                    // do nothing
                }
            }, ClientEndpointConfig.Builder.create().configurator(new ClientEndpointConfig.Configurator() {
                @Override
                public void afterResponse(HandshakeResponse hr) {
                    List<String> headers = hr.getHeaders().get(HEADER_KEY);
                    System.out.println("Received headers: " + headers);

                    if (headers.contains(HEADER_VALUE_1) && headers.contains(HEADER_VALUE_2)) {
                        responseLatch.countDown();
                    }
                }
            }).build(), URI.create("ws://localhost:8025/testSameHeader"));

            assertTrue(responseLatch.await(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            if (server != null) {
                server.shutdown();
            }
        }
    }

    private HttpServer getHandshakeServer() throws IOException {
        HttpServer server = HttpServer.createSimpleServer("/testSameHeader", getHost(), getPort());
        server.getServerConfiguration().addHttpHandler(new HttpHandler() {
            public void service(Request request, Response response) throws Exception {
                response.setStatus(101);

                response.addHeader(UpgradeRequest.CONNECTION, UpgradeRequest.UPGRADE);
                response.addHeader(UpgradeRequest.UPGRADE, UpgradeRequest.WEBSOCKET);

                String secKey = request.getHeader(HandshakeRequest.SEC_WEBSOCKET_KEY);
                String key = secKey + UpgradeRequest.SERVER_KEY_HASH;

                MessageDigest instance;
                try {
                    instance = MessageDigest.getInstance("SHA-1");
                    instance.update(key.getBytes("UTF-8"));
                    final byte[] digest = instance.digest();
                    String responseKey = Base64.getEncoder().encodeToString(digest);

                    response.addHeader(UpgradeResponse.SEC_WEBSOCKET_ACCEPT, responseKey);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                response.addHeader(HEADER_KEY, HEADER_VALUE_1);
                response.addHeader(HEADER_KEY, HEADER_VALUE_2);
            }
        });

        server.start();
        return server;
    }
}
