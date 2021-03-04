/*
 * Copyright (c) 2014, 2021 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertFalse;

/**
 * @author Vladimir Golyakov
 */
public class ConnectSynchronouslyHangTest extends TestContainer {

    @Test
    public void testConnectSynchronouslyHang() throws IOException, InterruptedException {
        Thread clientThread = null;
        ServerSocket server = startBadServer();
        try {
            clientThread = new Thread() {
                @Override
                public void run() {
                    ClientManager client = ClientManager.createClient(JdkClientContainer.class.getName());
                    try {
                        client.connectToServer(AnnotatedClientEndpoint.class, getURI("/any-endpoint", "wss"));
                    } catch (Exception e) {
                        // ignore
                    } finally {
                        client.shutdown();
                    }
                }
            };
            clientThread.start();

            // Wait for client to hang or not
            clientThread.join(TimeUnit.SECONDS.toMillis(5));
            assertFalse("Client hangs", clientThread.isAlive());
        } finally {
            if (clientThread != null) {
                clientThread.interrupt();
            }
            server.close();
        }
    }

    private ServerSocket startBadServer() throws IOException {
        final ServerSocket serverSocket = new ServerSocket(getPort());
        new Thread() {
            @Override
            public void run() {
                // Server accepts client connection and disconnects for some reason
                try {
                    serverSocket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(5));
                    Socket clientSocket = serverSocket.accept();
                    try {
                        Thread.sleep(200);
                    } finally {
                        clientSocket.close();
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }.start();
        return serverSocket;
    }

    @ClientEndpoint
    public static class AnnotatedClientEndpoint {
    }
}
