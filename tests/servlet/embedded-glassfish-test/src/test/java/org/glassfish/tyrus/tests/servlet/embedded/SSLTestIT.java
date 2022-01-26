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

package org.glassfish.tyrus.tests.servlet.embedded;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

@ExtendWith(ArquillianExtension.class)
public class SSLTestIT extends ServletTestBase {

    private static final String CONTEXT_PATH = "ssl-test";

    public SSLTestIT() {
        setContextPath(CONTEXT_PATH);
    }

    @Override
    protected String getScheme() {
        return "wss";
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() throws IOException {
        return ServletTestBase.createDeployment(
                "ssl-web.xml",
                CONTEXT_PATH,
                PlainEchoEndpoint.class
        );
    }

    @Override
    protected int getPort() {
        return 8181;
    }

    @Test
    public void plainEchoTest() throws DeploymentException, IOException, InterruptedException {
        super.testPlainEchoShort();
    }

    @Override
    protected ClientEndpointConfig createClientEndpointConfig() throws DeploymentException {
        final String password = "changeit";
        final String keyStore = System.getenv("GLASSFISH_HOME") + "/glassfish/domains/domain1/config/keystore.jks";

        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream in = new FileInputStream(keyStore)) {
                keystore.load(in, password.toCharArray());
            }
            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, password.toCharArray());

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                    keyManagerFactory.getKeyManagers(),
                    trustManagerFactory.getTrustManagers(),
                    new SecureRandom()
            );

            return ClientEndpointConfig.Builder.create().sslContext(sslContext).build();
        } catch (UnrecoverableKeyException | CertificateException | KeyStoreException | IOException
                | NoSuchAlgorithmException | KeyManagementException e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }
}
