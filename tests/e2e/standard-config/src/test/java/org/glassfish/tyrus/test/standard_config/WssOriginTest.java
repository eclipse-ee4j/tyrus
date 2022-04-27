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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.glassfish.tyrus.test.standard_config.springboot.WebSocketConfig;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import static org.glassfish.tyrus.client.ClientProperties.SSL_ENGINE_CONFIGURATOR;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * This tests that a correct Origin is sent when working with a wss:// connection.
 * A Spring Boot application is started to easily start a TLS-protected Websocket server that
 * verifies Origin header.
 *
 * @author Roman Puchkovskiy
 */
@SpringBootTest(classes = {WssApplication.class, WebSocketConfig.class}, webEnvironment = RANDOM_PORT)
@RunWith(SpringRunner.class)
public class WssOriginTest extends TestContainer {
    @LocalServerPort
    private int serverPort;

    @Test
    public void wssConnectionToServerValidatingOriginShouldWork() throws Exception {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create()
                .build();

        ClientManager client = createClient();
        client.getProperties().put(SSL_ENGINE_CONFIGURATOR, createSslEngineConfigurator());

        client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(final Session session, EndpointConfig EndpointConfig) {
            }
        }, cec, new URI("wss://localhost:" + serverPort + "/hello"));
    }

    private SslEngineConfigurator createSslEngineConfigurator() {
        return createTrustAllSslEngineConfigurator();
    }

    private SslEngineConfigurator createTrustAllSslEngineConfigurator() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");

            ctx.init(null, new TrustManager[]{new DisabledX509TrustManager()}, null);

            SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(ctx, true, false, false);

            sslEngineConfigurator.setHostVerificationEnabled(false);

            return sslEngineConfigurator;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class DisabledX509TrustManager implements X509TrustManager {
        private static final X509Certificate[] CERTS = new X509Certificate[0];

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {
            // No-op, all clients are trusted.
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {
            // No-op, all servers are trusted.
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return CERTS;
        }
    }
}
