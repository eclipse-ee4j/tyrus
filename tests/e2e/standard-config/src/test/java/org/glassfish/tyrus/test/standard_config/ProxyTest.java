/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;
import javax.websocket.DeploymentException;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.GrizzlyModProxy;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Petr Janouch
 */
public class ProxyTest extends TestContainer {

    private static final String PROXY_IP = "localhost";
    private static final int PROXY_PORT = 8090;
    private static final String PROXY_URL = "http://" + PROXY_IP + ":" + PROXY_PORT;

    /**
     * A basic positive test.
     * <p/>
     * A client manages to connect from behind a proxy and send and receive a message.
     */
    @Test
    public void testBasic() throws DeploymentException, IOException, InterruptedException {
        Server server = startServer(AnnotatedServerEndpoint.class);

        GrizzlyModProxy proxy = new GrizzlyModProxy(PROXY_IP, PROXY_PORT);
        proxy.start();

        try {
            ClientManager client = createClient();
            Map<String, Object> properties = client.getProperties();
            properties.put(ClientProperties.PROXY_URI, PROXY_URL);

            CountDownLatch latch = new CountDownLatch(1);
            client.connectToServer(new AnnotatedClientEndpoint(latch), getURI(AnnotatedServerEndpoint.class));

            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } finally {
            proxy.stop();
            stopServer(server);
        }
    }

    /**
     * Test a situation when the client receives a response to CONNECT with a status code other than 200.
     * This can happen for instance if the server is down or does not exist.
     */
    @Test
    public void testNonExistentServer() throws DeploymentException, InterruptedException, IOException {
        GrizzlyModProxy proxy = new GrizzlyModProxy(PROXY_IP, PROXY_PORT);
        proxy.start();

        try {
            ClientManager client = createClient();
            Map<String, Object> properties = client.getProperties();
            properties.put(ClientProperties.PROXY_URI, PROXY_URL);

            try {
                client.connectToServer(new AnnotatedClientEndpoint(new CountDownLatch(1)),
                        URI.create("ws://nonExistentServer.com"));
                fail();
            } catch (DeploymentException e) {
                // At least check it is an IOException and that there is a [P|p]roxy problem
                assertTrue(e.getCause() instanceof IOException);
                assertTrue(e.getCause().getMessage().contains("roxy"));
            }

        } finally {
            proxy.stop();
        }
    }

    /**
     * Tests a situation when a client sends CONNECT to a proxy, but does not receive any reply.
     */
    @Ignore // JDK connector is stuck forever
    @Test
    public void testConnectStuck() throws IOException {
        GrizzlyModProxy proxy = new GrizzlyModProxy(PROXY_IP, PROXY_PORT) {
            @Override
            protected NextAction handleConnect(final FilterChainContext ctx, final HttpContent content) {

                // simulate a situation when we receive CONNECT, but don't reply for some reason
                return ctx.getStopAction();
            }
        };

        proxy.start();

        try {
            ClientManager client = createClient();
            Map<String, Object> properties = client.getProperties();
            properties.put(ClientProperties.PROXY_URI, PROXY_URL);
            properties.put(ClientProperties.HANDSHAKE_TIMEOUT, 200);

            try {
                client.connectToServer(new AnnotatedClientEndpoint(new CountDownLatch(1)), getURI(AnnotatedServerEndpoint.class));
                fail();
            } catch (DeploymentException e) {
                assertTrue(e.getMessage().contains("Handshake response not received"));
            }

        } finally {
            proxy.stop();
        }
    }

    @ServerEndpoint("/destinationEndpoint")
    public static class AnnotatedServerEndpoint {

        @OnMessage
        public String onMessage(String message) {
            return message;
        }

    }

    @ClientEndpoint
    public static class AnnotatedClientEndpoint {

        private final CountDownLatch latch;

        public AnnotatedClientEndpoint(final CountDownLatch latch) {
            this.latch = latch;
        }

        @OnOpen
        public void onOpen(Session session) throws IOException {
            session.getBasicRemote().sendText("Hello");
        }

        @OnMessage
        public void onMessage(String message) {
            latch.countDown();
        }
    }
}
