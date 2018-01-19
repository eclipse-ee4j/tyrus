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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.standard_config.bean.EchoEndpoint;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class SharedContainerTimeoutTest extends TestContainer {

    private static final String SENT_MESSAGE = "That’s no moon, it’s a space station.";

    @Test
    @Ignore("won't work in all testing environments :/")
    public void testHello() throws InterruptedException, DeploymentException {
        _testHello();
        _testHello();

        // server cleanup.
        Thread.sleep(1000);
        int activeThreads = Thread.activeCount();
        Thread.sleep(10000);

        // shared client transport should be stopped now.
        assertTrue(activeThreads > Thread.activeCount());

        // test shared container restart.
        _testHello();
        _testHello();
        _testHello();

        // server cleanup.
        Thread.sleep(1000);

        activeThreads = Thread.activeCount();
        Thread.sleep(10000);

        // shared client transport should be stopped now.
        assertTrue(activeThreads > Thread.activeCount());

        // test shared container restart.
        _testHello();

    }

    void _testHello() throws DeploymentException {
        final CountDownLatch messageLatch;

        final Server server = startServer(EchoEndpoint.class);
        try {
            messageLatch = new CountDownLatch(1);

            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();

            // this is the interesting part.
            client.getProperties().put(ClientProperties.SHARED_CONTAINER, true);
            client.getProperties().put(ClientProperties.SHARED_CONTAINER_IDLE_TIMEOUT, 5);

            final Session session = client.connectToServer(new TestEndpointAdapter() {

                @Override
                public void onOpen(Session session) {

                    try {
                        session.addMessageHandler(new TestTextMessageHandler(this));
                        session.getBasicRemote().sendText(SENT_MESSAGE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(String message) {
                    if (message.equals(SENT_MESSAGE)) {
                        messageLatch.countDown();
                    }
                }
            }, cec, getURI(EchoEndpoint.class));
            assertTrue(messageLatch.await(1, TimeUnit.SECONDS));
            Assert.assertEquals(0L, messageLatch.getCount());

            session.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
