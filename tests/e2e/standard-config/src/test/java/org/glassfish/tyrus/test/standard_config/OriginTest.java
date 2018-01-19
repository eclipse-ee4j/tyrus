/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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
import java.net.URISyntaxException;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class OriginTest extends TestContainer {

    private static final String SENT_MESSAGE = "Always pass on what you have learned.";

    @ServerEndpoint(value = "/echo7", configurator = MyServerConfigurator.class)
    public static class TestEndpointOriginTest1 {

        @OnMessage
        public String onMessage(String message) {
            return message;
        }
    }

    public static class MyServerConfigurator extends ServerEndpointConfig.Configurator {

        @Override
        public boolean checkOrigin(String originHeaderValue) {
            return false;
        }
    }

    @ServerEndpoint(value = "/testEndpointOriginTest2", configurator = AnotherServerConfigurator.class)
    public static class TestEndpointOriginTest2 {

        @OnMessage
        public String onMessage(String message) {
            return message;
        }
    }

    public static class AnotherServerConfigurator extends ServerEndpointConfig.Configurator {

        @Override
        public boolean checkOrigin(String originHeaderValue) {

            if (!originHeaderValue.startsWith("http://")) {
                return false;
            } else {
                return true;
            }
        }
    }

    @Test
    public void testInvalidOrigin() throws URISyntaxException, IOException, DeploymentException {
        Server server = startServer(TestEndpointOriginTest1.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(final Session session, EndpointConfig EndpointConfig) {
                }
            }, cec, getURI(TestEndpointOriginTest1.class));

            fail("DeploymentException expected.");
        } catch (DeploymentException e) {
            e.printStackTrace();
            assertTrue(e.getCause().getMessage().contains("403"));
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testOriginStartsWithHttp() throws URISyntaxException, IOException, DeploymentException {
        Server server = startServer(TestEndpointOriginTest2.class);

        try {
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();
            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(final Session session, EndpointConfig EndpointConfig) {
                }
            }, cec, getURI(TestEndpointOriginTest2.class));

        } catch (DeploymentException e) {
            e.printStackTrace();
            throw e;
        } finally {
            stopServer(server);
        }
    }
}
