/*
 * Copyright (c) 2026 Oracle and/or its affiliates. All rights reserved.
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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.DeploymentException;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Test for {@link ClientProperties#SOCKET_CONNECT_TIMEOUT}.
 * <p>
 * Without this property, a connection attempt whose underlying socket connect never completes nor fails -
 * for example when a peer silently drops an incoming connection attempt rather than actively refusing it -
 * blocks {@link JdkClientContainer#openClientSocket} indefinitely, since {@link ClientProperties#HANDSHAKE_TIMEOUT}
 * only starts counting down once the socket has already connected.
 * <p>
 * This is reproduced locally, without any special network configuration, by exhausting the accept backlog of a
 * {@link ServerSocket} that never calls {@code accept()}: once the backlog is full, further incoming connection
 * attempts have their SYN silently dropped by the OS rather than rejected (on platforms where
 * {@code tcp_abort_on_overflow} - or the platform equivalent - is not set to send a RST), which is the same
 * "connect never completes" situation a silently filtering firewall or a black-holed route would cause.
 */
public class SocketConnectTimeoutTest {

    private static final int BACKLOG_FILL_ATTEMPTS = 8;
    private static final int FILL_CONNECT_TIMEOUT = 500;

    @Test
    public void connectTimesOutInsteadOfHangingForever() throws Exception {
        try (ServerSocket server = new ServerSocket()) {
            server.bind(new InetSocketAddress("127.0.0.1", 0), 1);
            int port = server.getLocalPort();

            // Fill the accept backlog: open connections but never call server.accept(), so the kernel has no
            // room left for further incoming connection attempts and silently drops their SYN instead of
            // completing or actively refusing them. Whether this actually happens, and after how many
            // attempts, is platform/kernel-dependent (backlog size rounding, overflow handling, etc.), so we
            // track whether we actually observed the precondition instead of assuming it from the attempt count.
            boolean backlogExhausted = false;
            for (int i = 0; i < BACKLOG_FILL_ATTEMPTS; i++) {
                try (Socket fillSocket = new Socket()) {
                    fillSocket.connect(new InetSocketAddress("127.0.0.1", port), FILL_CONNECT_TIMEOUT);
                } catch (SocketTimeoutException timedOut) {
                    // the SYN was silently dropped rather than accepted or actively refused - this is the
                    // precondition the rest of the test relies on.
                    backlogExhausted = true;
                    break;
                } catch (IOException otherFailure) {
                    // e.g. "Connection refused": this platform actively rejects once the backlog is full
                    // instead of silently dropping, so it cannot be used to reproduce this scenario.
                    break;
                }
            }

            assumeTrue("Could not reproduce a silently-dropped connection attempt by exhausting the accept "
                    + "backlog on this platform (saw neither a timed-out fill connection, nor backlog exhaustion "
                    + "after " + BACKLOG_FILL_ATTEMPTS + " attempts) - skipping, since the rest of this test "
                    + "would otherwise pass vacuously without exercising SOCKET_CONNECT_TIMEOUT at all",
                    backlogExhausted);

            ClientManager client = ClientManager.createClient(JdkClientContainer.class.getName());
            client.getProperties().put(ClientProperties.SOCKET_CONNECT_TIMEOUT, 2000);

            URI uri = URI.create("ws://127.0.0.1:" + port + "/");

            long start = System.currentTimeMillis();
            final AtomicReference<Throwable> connectFailure = new AtomicReference<>();
            Thread connectThread = new Thread(() -> {
                try {
                    client.connectToServer(AnnotatedClientEndpoint.class, uri);
                } catch (Throwable t) {
                    connectFailure.set(t);
                }
            });
            connectThread.setDaemon(true);
            connectThread.start();

            // Generous bound: SOCKET_CONNECT_TIMEOUT (2000ms) plus slack. Before the fix this join() would time
            // out here with connectThread still alive, since the underlying connect attempt never completed.
            connectThread.join(10_000);

            assertTrue("connectToServer() did not return within 10s of a 2s SOCKET_CONNECT_TIMEOUT - "
                    + "the connect attempt is hanging instead of timing out", !connectThread.isAlive());

            long elapsed = System.currentTimeMillis() - start;
            assertTrue("connectToServer() returned in " + elapsed + "ms, expected close to the "
                    + "configured 2000ms SOCKET_CONNECT_TIMEOUT", elapsed < 9_000);

            Throwable failure = connectFailure.get();
            if (!(failure instanceof DeploymentException)) {
                fail("Expected connectToServer() to fail with DeploymentException due to the connect timing out, "
                        + "but got: " + failure);
            }
        }
    }

    @ClientEndpoint
    public static class AnnotatedClientEndpoint {
    }
}
