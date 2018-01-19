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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;
import javax.websocket.DeploymentException;
import javax.websocket.OnMessage;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.standard_config.bean.EchoEndpoint;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class GenericClientEndpointTest extends TestContainer {

    static final CountDownLatch messageLatch = new CountDownLatch(1);

    public abstract static class GenericEndpoint<T> {
        public abstract void onMessage(T message);
    }

    @ClientEndpoint
    public static class ConcreteEndpoint extends GenericEndpoint<String> {
        @Override
        @OnMessage
        public void onMessage(String message) {
            messageLatch.countDown();
        }
    }

    @Test
    public void testHello() throws DeploymentException {
        final Server server = startServer(EchoEndpoint.class);
        try {
            ClientManager client = createClient();
            final Session session = client.connectToServer(ConcreteEndpoint.class, getURI(EchoEndpoint.class));

            session.getBasicRemote().sendText("This is Red 5, I’m going in.");

            assertTrue(messageLatch.await(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            stopServer(server);
        }
    }
}
