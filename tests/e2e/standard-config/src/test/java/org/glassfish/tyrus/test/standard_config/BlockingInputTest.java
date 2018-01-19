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
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;
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
 * Tests that threads blocked in {@link java.io.Reader} and {@link java.io.InputStream} are unblocked after the session
 * has been closed. Also tests that attempt to read from {@link java.io.Reader} and {@link java.io.InputStream}
 * will fail when the session has been closed.
 *
 * @author Petr Janouch
 */
public class BlockingInputTest extends TestContainer {

    /**
     * Test that a thread blocked in {@link java.io.Reader} on the client side gets released if the session is closed by
     * the client.
     */
    @Test
    public void testReaderCloseByClient() {
        Server server = null;
        try {
            CountDownLatch threadReleasedLatch = new CountDownLatch(1);
            CountDownLatch messageLatch = new CountDownLatch(1);

            server = startServer(AnnotatedServerTextEndpoint.class);
            ClientManager client = createClient();
            Session session = client.connectToServer(new CloseByClientEndpoint(threadReleasedLatch, messageLatch),
                                                     getURI(AnnotatedServerTextEndpoint.class));

            assertTrue(messageLatch.await(1, TimeUnit.SECONDS));
            // give the client endpoint some time to get blocked
            Thread.sleep(100);
            session.close();
            assertTrue(threadReleasedLatch.await(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    /**
     * Test that a thread blocked in {@link java.io.InputStream} on the client side gets released if the session is
     * closed by the client.
     */
    @Test
    public void testInputStreamCloseByClient() {
        Server server = null;
        try {
            CountDownLatch threadReleasedLatch = new CountDownLatch(1);
            CountDownLatch messageLatch = new CountDownLatch(1);

            server = startServer(AnnotatedServerBinaryEndpoint.class);
            ClientManager client = createClient();
            Session session = client.connectToServer(new CloseByClientEndpoint(threadReleasedLatch, messageLatch),
                                                     getURI(AnnotatedServerBinaryEndpoint.class));

            assertTrue(messageLatch.await(1, TimeUnit.SECONDS));
            // give the client endpoint some time to get blocked
            Thread.sleep(100);
            session.close();
            assertTrue(threadReleasedLatch.await(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    /**
     * Test that a thread blocked in {@link java.io.Reader} on the client side gets released if the session is closed
     * by the server.
     */
    @Test
    public void testReaderCloseByServer() {
        Server server = null;
        try {
            CountDownLatch threadReleasedLatch = new CountDownLatch(1);

            server = startServer(AnnotatedServerTextEndpoint.class);
            ClientManager client = createClient();
            client.connectToServer(new CloseByServerEndpoint(threadReleasedLatch),
                                   getURI(AnnotatedServerTextEndpoint.class));

            assertTrue(threadReleasedLatch.await(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    /**
     * Test that a thread blocked in {@link java.io.InputStream} on the client side gets released if the session is
     * closed by the server.
     */
    @Test
    public void testInputStreamCloseByServer() {
        Server server = null;
        try {
            CountDownLatch threadReleasedLatch = new CountDownLatch(1);

            server = startServer(AnnotatedServerBinaryEndpoint.class);
            ClientManager client = createClient();
            client.connectToServer(new CloseByServerEndpoint(threadReleasedLatch),
                                   getURI(AnnotatedServerBinaryEndpoint.class));

            assertTrue(threadReleasedLatch.await(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    /**
     * Test that an attempt to read from {@link java.io.Reader} will throw {@link java.io.IOException} if the session
     * has been closed.
     */
    @Test
    public void testReaderWithClosedSession() {
        Server server = null;
        try {
            CountDownLatch threadReleasedLatch = new CountDownLatch(1);

            server = startServer(AnnotatedServerTextEndpoint.class);
            ClientManager client = createClient();
            client.connectToServer(new ReadFromClosedSessionEndpoint(threadReleasedLatch),
                                   getURI(AnnotatedServerTextEndpoint.class));

            assertTrue(threadReleasedLatch.await(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    /**
     * Test that an attempt to read from {@link java.io.InputStream} will throw {@link java.io.IOException} if the
     * session has been closed.
     */
    @Test
    public void testInputStreamWithClosedSession() {
        Server server = null;
        try {
            CountDownLatch threadReleasedLatch = new CountDownLatch(1);

            server = startServer(AnnotatedServerBinaryEndpoint.class);
            ClientManager client = createClient();
            client.connectToServer(new ReadFromClosedSessionEndpoint(threadReleasedLatch),
                                   getURI(AnnotatedServerBinaryEndpoint.class));

            assertTrue(threadReleasedLatch.await(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint("/blockingTextInputEndpoint")
    public static class AnnotatedServerTextEndpoint {

        @OnOpen
        public void onOpen(Session session) throws IOException {
            session.getBasicRemote().sendText("A", false);
        }

        @OnMessage
        public void OnMessage(PongMessage message, Session session) throws IOException, InterruptedException {
            // give the client endpoint some time to get blocked
            Thread.sleep(100);
            session.close();
        }
    }

    @ServerEndpoint("/blockingBinaryInputEndpoint")
    public static class AnnotatedServerBinaryEndpoint {

        @OnOpen
        public void onOpen(Session session) throws IOException {
            session.getBasicRemote().sendBinary(ByteBuffer.wrap("A".getBytes()), false);
        }

        @OnMessage
        public void OnMessage(PongMessage message, Session session) throws IOException, InterruptedException {
            // give the client endpoint some time to get blocked
            Thread.sleep(100);
            session.close();
        }
    }

    @ClientEndpoint
    public static class CloseByServerEndpoint {

        /**
         * Latch waiting for the blocked thread to be released.
         */
        private final CountDownLatch threadReleasedLatch;

        public CloseByServerEndpoint(CountDownLatch threadReleasedLatch) {
            this.threadReleasedLatch = threadReleasedLatch;
        }

        @OnMessage
        public void onMessage(Session session, Reader reader) throws IOException {
            reader.read();
            //server will close the session upon receiving a pong
            session.getAsyncRemote().sendPong(null);
            try {
                reader.read();
            } catch (IOException e) {
                threadReleasedLatch.countDown();
            }
        }

        @OnMessage
        public void onMessage(Session session, InputStream stream) throws IOException {
            stream.read();
            //server will close the session upon receiving a pong
            session.getAsyncRemote().sendPong(null);
            try {
                stream.read();
            } catch (Exception e) {
                threadReleasedLatch.countDown();
            }
        }
    }

    @ClientEndpoint
    public static class CloseByClientEndpoint {

        /**
         * Latch waiting for the blocked thread to be released.
         */
        private final CountDownLatch threadReleasedLatch;
        /**
         * Latch waiting for a message from the server.
         */
        private final CountDownLatch messageLatch;

        public CloseByClientEndpoint(CountDownLatch threadReleasedLatch, CountDownLatch messageLatch) {
            this.threadReleasedLatch = threadReleasedLatch;
            this.messageLatch = messageLatch;
        }

        @OnMessage
        public void onMessage(Session session, Reader reader) throws IOException {
            reader.read();
            messageLatch.countDown();
            try {
                reader.read();
            } catch (IOException e) {
                threadReleasedLatch.countDown();
            }
        }

        @OnMessage
        public void onMessage(Session session, InputStream stream) throws IOException {
            stream.read();
            messageLatch.countDown();
            try {
                stream.read();
            } catch (IOException e) {
                threadReleasedLatch.countDown();
            }
        }
    }

    @ClientEndpoint
    public static class ReadFromClosedSessionEndpoint {

        /**
         * Latch waiting for the blocked thread to be released.
         */
        private final CountDownLatch threadReleasedLatch;

        public ReadFromClosedSessionEndpoint(CountDownLatch threadReleasedLatch) {
            this.threadReleasedLatch = threadReleasedLatch;
        }

        @OnMessage
        public void onMessage(Session session, Reader reader) throws IOException {
            reader.read();
            session.close();
            try {
                reader.read();
            } catch (IOException e) {
                threadReleasedLatch.countDown();
            }
        }

        @OnMessage
        public void onMessage(Session session, InputStream stream) throws IOException {
            stream.read();
            session.close();
            try {
                stream.read();
            } catch (IOException e) {
                threadReleasedLatch.countDown();
            }
        }
    }
}
