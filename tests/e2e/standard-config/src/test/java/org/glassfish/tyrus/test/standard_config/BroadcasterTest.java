/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.TyrusSession;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * Tests broadcasting to several clients.
 *
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class BroadcasterTest extends TestContainer {
    private static final String SENT_MESSAGE = "Hello World";

    private final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

    @Test
    public void testBroadcaster() throws DeploymentException {
        final CountDownLatch messageLatch = new CountDownLatch(2);
        Server server = startServer(BroadcasterTestEndpoint.class);

        try {
            final TEndpointAdapter ea1 = new TEndpointAdapter(messageLatch);
            final TEndpointAdapter ea2 = new TEndpointAdapter(messageLatch);

            final ClientManager client1 = createClient();
            client1.connectToServer(ea1, cec, getURI(BroadcasterTestEndpoint.class));
            final ClientManager client2 = createClient();
            client2.connectToServer(ea2, cec, getURI(BroadcasterTestEndpoint.class));

            synchronized (ea1) {
                if (ea1.peer == null) {
                    ea1.wait();
                }
            }

            synchronized (ea2) {
                if (ea2.peer == null) {
                    ea2.wait();
                }
            }

            ea1.peer.sendText(SENT_MESSAGE);

            assertTrue("Timeout reached. Message latch value: " + messageLatch.getCount(),
                       messageLatch.await(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void testTyrusBroadcaster() throws DeploymentException {
        final CountDownLatch messageLatch = new CountDownLatch(2);
        Server server = startServer(TyrusBroadcasterTestEndpoint.class);

        try {
            final TEndpointAdapter ea1 = new TEndpointAdapter(messageLatch);
            final TEndpointAdapter ea2 = new TEndpointAdapter(messageLatch);

            final ClientManager client1 = createClient();
            client1.connectToServer(ea1, cec, getURI(TyrusBroadcasterTestEndpoint.class));
            final ClientManager client2 = createClient();
            client2.connectToServer(ea2, cec, getURI(TyrusBroadcasterTestEndpoint.class));

            synchronized (ea1) {
                if (ea1.peer == null) {
                    ea1.wait();
                }
            }

            synchronized (ea2) {
                if (ea2.peer == null) {
                    ea2.wait();
                }
            }

            ea1.peer.sendText(SENT_MESSAGE);

            assertTrue("Timeout reached. Message latch value: " + messageLatch.getCount(),
                       messageLatch.await(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }

    private static class TEndpointAdapter extends TestEndpointAdapter {
        private final CountDownLatch messageLatch;
        public RemoteEndpoint.Basic peer;

        TEndpointAdapter(CountDownLatch messageLatch) {
            this.messageLatch = messageLatch;
        }

        @Override
        public EndpointConfig getEndpointConfig() {
            return null;
        }

        @Override
        public synchronized void onOpen(Session session) {
            this.peer = session.getBasicRemote();
            notifyAll();

            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    TEndpointAdapter.this.onMessage(message);
                }
            });
        }

        @Override
        public void onMessage(String message) {
            messageLatch.countDown();
        }
    }

    /**
     * @author Martin Matula (martin.matula at oracle.com)
     * @author Stepan Kopriva (stepan.kopriva at oracle.com)
     */
    @ServerEndpoint(value = "/broadcast")
    public static class BroadcasterTestEndpoint {

        @OnMessage
        public void message(String message, Session session) throws IOException, EncodeException {
            for (Session s : session.getOpenSessions()) {
                s.getBasicRemote().sendText(message);
            }
        }
    }

    @ServerEndpoint(value = "/tyrus-broadcast")
    public static class TyrusBroadcasterTestEndpoint {

        @OnMessage
        public void message(String message, Session session) throws IOException, EncodeException {
            ((TyrusSession) session).broadcast(message);
        }
    }
}
