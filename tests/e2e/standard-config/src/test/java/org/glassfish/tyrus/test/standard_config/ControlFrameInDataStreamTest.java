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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests that a control frame (in the test represented by pong and close) sent within a stream of partial text or
 * binary messages gets delivered.
 *
 * @author Petr Janouch
 */
public class ControlFrameInDataStreamTest extends TestContainer {

    private static final String CLOSE_PHRASE = "Just checking this gets delivered";

    /**
     * Test that a pong message inside a stream of text partial messages gets delivered.
     */
    @Test
    public void testPongMessageInTextStream() {
        Server server = null;
        try {
            final CountDownLatch pongLatch = new CountDownLatch(1);

            server = startServer(PongInTextServerEndpoint.class);
            ClientManager client = createClient();
            client.connectToServer(new AnnotatedClientEndpoint(pongLatch, null),
                                   getURI(PongInTextServerEndpoint.class));

            assertTrue(pongLatch.await(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    /**
     * Test that a close message inside a stream of text partial messages gets delivered.
     */
    @Test
    public void testCloseMessageInTextStream() {
        Server server = null;
        try {
            final CountDownLatch closeLatch = new CountDownLatch(1);

            server = startServer(CloseInTextServerEndpoint.class);
            ClientManager client = createClient();
            client.connectToServer(new AnnotatedClientEndpoint(null, closeLatch),
                                   getURI(CloseInTextServerEndpoint.class));

            assertTrue(closeLatch.await(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    /**
     * Test that a pong message inside a stream of binary partial messages gets delivered.
     */
    @Test
    public void testPongMessageInBinaryStream() {
        Server server = null;
        try {
            final CountDownLatch pongLatch = new CountDownLatch(1);

            server = startServer(PongInBinaryServerEndpoint.class);
            ClientManager client = createClient();
            client.connectToServer(new AnnotatedClientEndpoint(pongLatch, null),
                                   getURI(PongInBinaryServerEndpoint.class));

            assertTrue(pongLatch.await(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    /**
     * Test that a close message inside a stream of binary partial messages gets delivered.
     */
    @Test
    public void testCloseMessageInBinaryStream() {
        Server server = null;
        try {
            final CountDownLatch closeLatch = new CountDownLatch(1);

            server = startServer(CloseInBinaryServerEndpoint.class);
            ClientManager client = createClient();
            client.connectToServer(new AnnotatedClientEndpoint(null, closeLatch),
                                   getURI(CloseInBinaryServerEndpoint.class));

            assertTrue(closeLatch.await(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint("/closeInTextEndpoint")
    public static class CloseInTextServerEndpoint {

        @OnOpen
        public void onOpen(Session session) throws IOException {
            session.getBasicRemote().sendText("Hi, wait for more ...", false);
            session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, CLOSE_PHRASE));
        }
    }

    @ServerEndpoint("/pongInTextEndpoint")
    public static class PongInTextServerEndpoint {

        @OnOpen
        public void onOpen(Session session) throws IOException {
            session.getBasicRemote().sendText("Hi, wait for more ...", false);
            session.getBasicRemote().sendPong(null);
        }
    }

    @ServerEndpoint("/pongInBinaryEndpoint")
    public static class PongInBinaryServerEndpoint {

        @OnOpen
        public void onOpen(Session session) throws IOException {
            session.getBasicRemote().sendBinary(ByteBuffer.wrap("Hi, wait for more ...".getBytes()), false);
            session.getBasicRemote().sendPong(null);
        }
    }

    @ServerEndpoint("/closeInBinaryEndpoint")
    public static class CloseInBinaryServerEndpoint {

        @OnOpen
        public void onOpen(Session session) throws IOException {
            session.getBasicRemote().sendBinary(ByteBuffer.wrap("Hi, wait for more ...".getBytes()), false);
            session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, CLOSE_PHRASE));
        }
    }

    @ClientEndpoint
    public static class AnnotatedClientEndpoint {

        private final CountDownLatch pongLatch;
        private final CountDownLatch closeLatch;

        public AnnotatedClientEndpoint(CountDownLatch pongLatch, CountDownLatch closeLatch) {
            this.pongLatch = pongLatch;
            this.closeLatch = closeLatch;
        }

        @OnMessage
        public void onMessage(Session session, ByteBuffer message, boolean last) {
        }

        @OnMessage
        public void onMessage(Session session, String message, boolean last) {
        }

        @OnMessage
        public void onMessage(PongMessage message, Session session) {
            pongLatch.countDown();
        }

        @OnClose
        public void onClose(CloseReason closeReason, Session session) {
            if (closeLatch != null && closeReason.getReasonPhrase().equals(CLOSE_PHRASE)) {
                closeLatch.countDown();
            }
        }
    }
}
