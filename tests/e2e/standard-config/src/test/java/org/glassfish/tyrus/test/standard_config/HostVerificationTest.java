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

import javax.websocket.ClientEndpoint;
import javax.websocket.server.ServerEndpoint;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslContextConfigurator;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * The test assumes that {@code tyrus.test.host.ip} contains IP of the server, which is not in the server certificate.
 * <p/>
 * The test will be run only if systems properties {@code tyrus.test.host.ip} and {@code tyrus.test.port.ssl} are set.
 *
 * @author Petr Janouch
 */
public class HostVerificationTest extends TestContainer {

    /**
     * Test that the client will manage to connect to the server using server IP, when host name verification is
     * disabled.
     */
    @Test
    public void disabledHostVerificationTest() {
        try {
            ClientManager client = createClient();
            SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(new SslContextConfigurator());
            sslEngineConfigurator.setHostVerificationEnabled(false);
            client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);

            client.connectToServer(AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class, "wss"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Test that the client will fail to connect to the server using server IP, when host name verification is enabled.
     */
    @Test
    public void enabledHostVerificationTest() {
        try {
            ClientManager client = createClient();

            // Grizzly client logs the exception - giving a hint to the reader of the logs
            System.out.println("=== SSL error may follow in the log ===");
            client.connectToServer(AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class, "wss"));

            fail();
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof SSLException);
        }
    }

    /**
     * Test that the client will manage to connect to the server using a custom host verifier.
     */
    @Test
    public void customHostVerifierPassTest() {
        try {
            ClientManager client = createClient();
            SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(new SslContextConfigurator());
            sslEngineConfigurator.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
            client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);

            client.connectToServer(AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class, "wss"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Test that the client will not manage to connect to the server using a custom host verifier.
     */
    @Test
    public void customHostVerifierFailTest() {
        try {
            ClientManager client = createClient();
            SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(new SslContextConfigurator());
            sslEngineConfigurator.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return false;
                }
            });
            client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);

            // Grizzly client logs the exception - giving a hint to the reader of the logs
            System.out.println("=== SSL error may follow in the log ===");
            client.connectToServer(AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class, "wss"));

            fail();
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof SSLException);
        }
    }

    /**
     * The test will be run only if systems properties {@code tyrus.test.host.ip} and {@code tyrus.test.port.ssl} are
     * set.
     */
    @Before
    public void before() {
        assumeTrue(
                System.getProperty("tyrus.test.host.ip") != null && System.getProperty("tyrus.test.port.ssl") != null);
    }

    @Override
    protected String getHost() {
        return System.getProperty("tyrus.test.host.ip");
    }

    @Override
    protected int getPort() {
        String sslPort = System.getProperty("tyrus.test.port.ssl");

        try {
            return Integer.parseInt(sslPort);
        } catch (NumberFormatException nfe) {
            fail();
        }

        // just to make javac happy - won't be executed.
        return 0;
    }

    @ClientEndpoint
    public static class AnnotatedClientEndpoint {
    }

    @ServerEndpoint("/hostVerificationEndpoint")
    public static class AnnotatedServerEndpoint {
    }
}
