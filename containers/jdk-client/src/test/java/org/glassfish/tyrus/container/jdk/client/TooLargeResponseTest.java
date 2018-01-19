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

package org.glassfish.tyrus.container.jdk.client;

import java.util.Collections;

import javax.websocket.ClientEndpoint;
import javax.websocket.DeploymentException;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * JDK client has a hard limit for the maximal upgrade response size
 * ({@link org.glassfish.tyrus.container.jdk.client.HttpResponseParser#BUFFER_MAX_SIZE}). This test tests that
 * the situation when the limit is exceeded is correctly handled.
 *
 * @author Petr Janouch
 */
public class TooLargeResponseTest extends TestContainer {

    @Test
    public void testUpgradeResponse() {
        Server server = null;
        try {
            server = startServer(AnnotatedServerEndpoint.class);
            ClientManager clientManager = ClientManager.createClient(JdkClientContainer.class.getName());
            try {
                clientManager.connectToServer(AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class));
                fail();
            } catch (DeploymentException e) {
                // the DeploymentException should wrap a ParseException, this just checks that the test tests the
                // right thing
                assertTrue(ParseException.class.equals(e.getCause().getClass()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    @ClientEndpoint
    public static class AnnotatedClientEndpoint {

    }

    @ServerEndpoint(value = "/tooLargeResponseEndpoint", configurator = ServerConfig.class)
    public static class AnnotatedServerEndpoint {

    }

    public static class ServerConfig extends ServerEndpointConfig.Configurator {

        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            int addedHeadersSize = 0;
            int headerCounter = 0;
            String headerKey = "header";
            StringBuilder sb = new StringBuilder();
            // create 1k header value (Grizzly has a 100 headers limit, so there can't be many small headers)
            for (int i = 0; i < 1000; i++) {
                sb.append("A");
            }
            String headerValue = sb.toString();

            /* add at least as much headers to exceed JDK client limit
            (the standard parts of upgrade response are not counted into this for convenience) */
            while (addedHeadersSize < HttpResponseParser.BUFFER_MAX_SIZE) {
                response.getHeaders().put(headerKey + headerCounter, Collections.singletonList(headerValue));
                addedHeadersSize += headerKey.length() + headerValue.length() + 4; // 4 -> :, \r, \n, headerCounter
                headerCounter++;
            }
        }
    }
}
