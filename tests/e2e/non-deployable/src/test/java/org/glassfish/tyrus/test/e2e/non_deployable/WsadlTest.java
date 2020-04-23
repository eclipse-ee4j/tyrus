/*
 * Copyright (c) 2014, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.test.e2e.non_deployable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;

import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class WsadlTest extends TestContainer {

    public WsadlTest() {
        getServerProperties().put(TyrusWebSocketEngine.WSADL_SUPPORT, "true");
    }

    @Test
    public void testWsadl() throws DeploymentException, IOException {
        Server server = startServer(NoopEndpoint.class);

        try {
            boolean found = false;

            URLConnection urlConnection = getURI("/application.wsadl", "http").toURL().openConnection();
            urlConnection.connect();
            System.out.println(urlConnection.getContentLength());
            InputStream inputStream = urlConnection.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains(NoopEndpoint.class.getAnnotation(ServerEndpoint.class).value())) {
                    found = true;
                }
                System.out.println("### " + line);
            }

            assertTrue(found);
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint(value = "/noopEndpoint")
    public static class NoopEndpoint {
    }

}
