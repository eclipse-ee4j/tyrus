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

package org.glassfish.tyrus.tests.e2e.auth.basic;

import java.io.IOException;
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
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.auth.AuthConfig;
import org.glassfish.tyrus.client.auth.AuthenticationException;
import org.glassfish.tyrus.client.auth.Credentials;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Authorization tests.
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
public class DigestAuthTest extends TestContainer {

    public static final String SCHEME = "ws";
    private static final String CONTEXT_PATH = "/e2e-digest-auth-test";

    public DigestAuthTest() {
        setDefaultPort(8025);
        setContextPath(CONTEXT_PATH);
    }

    @Test
    public void testAuthorizationSuccessAuthConfig() throws DeploymentException, InterruptedException, IOException,
            AuthenticationException {
        final AuthConfig authConfig = AuthConfig.Builder.create().build();

        final CountDownLatch messageLatch = new CountDownLatch(1);

        final ClientManager client = createClient();

        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

        client.getProperties().put(ClientProperties.CREDENTIALS, new Credentials("ws_user", "password"));
        client.getProperties().put(ClientProperties.AUTH_CONFIG, authConfig);

        client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig EndpointConfig) {
                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            System.out.println("received message: " + message);
                            assertEquals(message, "Do or do not, there is no try.");
                            messageLatch.countDown();
                        }
                    });

                    session.getBasicRemote().sendText("Do or do not, there is no try.");
                } catch (IOException e) {
                    // do nothing
                }
            }
        }, cec, getURI(DigestAuthEchoEndpoint.class.getAnnotation(ServerEndpoint.class).value(), SCHEME));

        messageLatch.await(1, TimeUnit.SECONDS);
        assertEquals(0, messageLatch.getCount());
    }

    @Test
    public void testAuthorizationSuccessCredentials() throws DeploymentException, InterruptedException, IOException,
            AuthenticationException {

        final CountDownLatch messageLatch = new CountDownLatch(1);

        final ClientManager client = createClient();

        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

        client.getProperties().put(ClientProperties.CREDENTIALS, new Credentials("ws_user", "password"));

        client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig EndpointConfig) {
                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            System.out.println("received message: " + message);
                            assertEquals(message, "Do or do not, there is no try.");
                            messageLatch.countDown();
                        }
                    });

                    session.getBasicRemote().sendText("Do or do not, there is no try.");
                } catch (IOException e) {
                    // do nothing
                }
            }
        }, cec, getURI(DigestAuthEchoEndpoint.class.getAnnotation(ServerEndpoint.class).value(), SCHEME));

        messageLatch.await(1, TimeUnit.SECONDS);
        assertEquals(0, messageLatch.getCount());
    }
}
