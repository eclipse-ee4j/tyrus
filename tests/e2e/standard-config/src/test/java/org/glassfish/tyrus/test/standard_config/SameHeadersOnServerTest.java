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

package org.glassfish.tyrus.test.standard_config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.spi.UpgradeRequest;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests a situation when a handshake request contains the same header twice with different values.
 *
 * @author Petr Janouch
 */
public class SameHeadersOnServerTest extends TestContainer {

    private static final String HEADER_KEY = "my-header";
    private static final String HEADER_VALUE_1 = "my-header-value-1";
    private static final String HEADER_VALUE_2 = "my-header-value-2";

    @Test
    public void testSameHeaderNamesInHandshakeRequest() {
        Server server = null;
        try {
            server = startServer(AnnotatedServerEndpoint.class);
            final CountDownLatch responseLatch = new CountDownLatch(1);

            StringBuilder handshakeRequest = new StringBuilder();
            URI serverEndpointUri = getURI(AnnotatedServerEndpoint.class);
            handshakeRequest.append("GET " + serverEndpointUri.getPath() + " HTTP/1.1\r\n");
            appendHeader(handshakeRequest, "Host", getHost() + ":" + getPort());
            appendHeader(handshakeRequest, UpgradeRequest.CONNECTION, UpgradeRequest.UPGRADE);
            appendHeader(handshakeRequest, UpgradeRequest.UPGRADE, UpgradeRequest.WEBSOCKET);
            appendHeader(handshakeRequest, HandshakeRequest.SEC_WEBSOCKET_KEY, "MX3DK3cbUu5DHEWW6dyzJQ==");
            appendHeader(handshakeRequest, HandshakeRequest.SEC_WEBSOCKET_VERSION, "13");

            appendHeader(handshakeRequest, HEADER_KEY, HEADER_VALUE_1);
            appendHeader(handshakeRequest, HEADER_KEY, HEADER_VALUE_2);

            handshakeRequest.append("\r\n");

            String requestStr = handshakeRequest.toString();
            byte[] requestBytes = requestStr.getBytes(Charset.forName("ISO-8859-1"));

            final Socket socket = new Socket(getHost(), getPort());
            OutputStream out = socket.getOutputStream();
            out.write(requestBytes);
            out.flush();

            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    BufferedReader in = null;
                    try {
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        while (true) {
                            String responseLine = in.readLine();
                            if (responseLine == null) {
                                break;
                            }

                            if (responseLine.contains("101")) {
                                responseLatch.countDown();
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail();
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });

            try {
                assertTrue(responseLatch.await(5, TimeUnit.SECONDS));
            } finally {
                out.close();
                socket.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    private void appendHeader(StringBuilder request, String key, String value) {
        request.append(key);
        request.append(":");
        request.append(value);
        request.append("\r\n");
    }

    @ServerEndpoint(value = "/sameHeadersEndpoint", configurator = HeaderCheckingConfigurator.class)
    public static class AnnotatedServerEndpoint {
    }

    public static class HeaderCheckingConfigurator extends ServerEndpointConfig.Configurator {

        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            List<String> headerValues = request.getHeaders().get(HEADER_KEY);
            System.out.println("RECEIVED HEADERS: " + headerValues);

            if (!headerValues.contains(HEADER_VALUE_1) || !headerValues.contains(HEADER_VALUE_2)) {
                throw new RuntimeException("Request does not contain both headers " + headerValues);
            }
        }
    }
}
