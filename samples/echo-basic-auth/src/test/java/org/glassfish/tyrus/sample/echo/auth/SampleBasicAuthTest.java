/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.echo.auth;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.auth.AuthConfig;
import org.glassfish.tyrus.client.auth.AuthenticationException;
import org.glassfish.tyrus.client.auth.Credentials;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
public class SampleBasicAuthTest extends TestContainer {

    public SampleBasicAuthTest() {
        setContextPath("/sample-echo-basic-auth");
    }

    @Test
    public void testDisabledBasicAuth() throws InterruptedException, IOException, AuthenticationException {
        AuthConfig authConfig = AuthConfig.Builder.create()
                                                  .disableProvidedBasicAuth()
                                                  .build();
        Credentials credentials = new Credentials("ws_user", "password");
        try {
            testEcho(authConfig, credentials);
        } catch (DeploymentException e) {
            assertTrue("Invalid auth config should throw an AuthenticationException",
                       e.getCause() instanceof AuthenticationException);
        }
    }

    @Test
    public void testSimplestBasicAuth() throws DeploymentException, InterruptedException, IOException,
            AuthenticationException {
        Credentials credentials = new Credentials("ws_user", "password");
        testEcho(null, credentials);
    }

    @Test
    public void testNullCredentials() throws InterruptedException, IOException, AuthenticationException {
        AuthConfig authConfig = AuthConfig.Builder.create().build();
        Credentials credentials = null;
        try {
            testEcho(authConfig, credentials);
        } catch (DeploymentException e) {
            assertTrue("Invalid auth config should throw an AuthenticationException",
                       e.getCause() instanceof AuthenticationException);
        }
    }

    @Test
    public void testNulls() throws InterruptedException, IOException, AuthenticationException {
        AuthConfig authConfig = null;
        Credentials credentials = null;
        try {
            testEcho(authConfig, credentials);
        } catch (DeploymentException e) {
            assertTrue("Invalid auth config should throw an AuthenticationException",
                       e.getCause() instanceof AuthenticationException);
        }
    }

    public void testEcho(AuthConfig authConfig, Credentials credentials) throws DeploymentException,
            InterruptedException, IOException {
        if (System.getProperty("tyrus.test.host") == null) {
            return;
        }

        final CountDownLatch messageLatch = new CountDownLatch(1);
        final CountDownLatch onOpenLatch = new CountDownLatch(1);

        final ClientManager client = createClient();

        client.getProperties().put(ClientProperties.AUTH_CONFIG, authConfig);
        client.getProperties().put(ClientProperties.CREDENTIALS, credentials);

        client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig EndpointConfig) {
                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            System.out.println("### Received: " + message);

                            if (message.equals("Do or do not, there is no try. (from your server)")) {
                                messageLatch.countDown();
                            } else if (message.equals("onOpen")) {
                                onOpenLatch.countDown();
                            }
                        }
                    });

                    session.getBasicRemote().sendText("Do or do not, there is no try.");
                } catch (IOException e) {
                    // do nothing
                }
            }
        }, ClientEndpointConfig.Builder.create().build(), getURI(BasicAuthEchoEndpoint.class, "wss"));

        messageLatch.await(1, TimeUnit.SECONDS);
        if (messageLatch.getCount() != 0 || onOpenLatch.getCount() != 0) {
            fail();
        }
    }

}
