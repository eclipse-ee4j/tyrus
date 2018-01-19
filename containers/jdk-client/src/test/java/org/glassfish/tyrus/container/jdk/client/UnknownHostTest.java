/*
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Test;

import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class UnknownHostTest {

    private static final Logger LOG = Logger.getLogger(UnknownHostTest.class.getName());


    @Test
    public void testIncreaseFileDescriptorsOnTyrusImplementationInCaseOfUnresolvedAddressException() throws Exception {
        LOG.log(Level.INFO, "BEGIN COUNT: {0}", getOpenFileDescriptorCount());
        String unreachedHostURL = "ws://unreachedhost:8025/e2e-test/echo1";
        URI uri = new URI(unreachedHostURL);
        WebSocketClientEndpoint webSocketClientEndpoint = new WebSocketClientEndpoint();

        //Warmup
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(webSocketClientEndpoint, uri);
        } catch (Exception e) {
            LOG.log(Level.FINE, e.getMessage());
            assertTrue(e.getMessage().contains("Connection failed"));
        }

        LOG.log(Level.INFO, "AFTER WARMUP COUNT: {0}", getOpenFileDescriptorCount());

        long fileDescriptorsBefore = getOpenFileDescriptorCount();

        //When
        int reconnectCount = 10;
        Session session = null;
        for (int i = 0; i < reconnectCount; i++) {
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                session = container.connectToServer(webSocketClientEndpoint, uri);
            } catch (Exception e) {
                LOG.log(Level.FINE, e.getMessage());
                assertTrue(e.getMessage().contains("Connection failed"));
                assertNull(session);

            }
        }

        long fileDescriptorsAfter = getOpenFileDescriptorCount();

        //Then
        LOG.log(Level.INFO, "END COUNT: {0}", getOpenFileDescriptorCount());
        assertEquals(fileDescriptorsBefore, fileDescriptorsAfter);

    }

    private long getOpenFileDescriptorCount() {
        return (((com.sun.management.UnixOperatingSystemMXBean) java.lang.management.ManagementFactory
                .getOperatingSystemMXBean()).getOpenFileDescriptorCount());
    }

    private static class WebSocketClientEndpoint extends Endpoint {

        @Override
        public void onOpen(Session session, EndpointConfig config) {

        }
    }
}
