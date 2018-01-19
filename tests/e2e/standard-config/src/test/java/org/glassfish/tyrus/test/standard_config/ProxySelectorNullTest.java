/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved.
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
import java.net.ProxySelector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.standard_config.bean.EchoEndpoint;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ProxySelectorNullTest extends TestContainer {

    private CountDownLatch messageLatch;
    private volatile String receivedMessage;
    private static final String SENT_MESSAGE = "Hello World";

    @Test
    public void testNullProxySelector() throws DeploymentException {
        final Server server = startServer(EchoEndpoint.class);
        final ProxySelector defaultProxySelector = ProxySelector.getDefault();

        try {
            messageLatch = new CountDownLatch(1);

            ProxySelector.setDefault(null);

            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = createClient();
            client.connectToServer(new TestEndpointAdapter() {

                private Session session;

                @Override
                public EndpointConfig getEndpointConfig() {
                    return cec;
                }

                @Override
                public void onOpen(Session session) {

                    this.session = session;

                    try {
                        session.addMessageHandler(new TestTextMessageHandler(this));
                        session.getBasicRemote().sendText(SENT_MESSAGE);
                        System.out.println("Hello message sent.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(String message) {
                    receivedMessage = message;

                    // TYRUS-141
                    if (session.getNegotiatedSubprotocol() != null) {
                        messageLatch.countDown();
                    }
                }
            }, cec, getURI(EchoEndpoint.class));
            messageLatch.await(1, TimeUnit.SECONDS);
            Assert.assertEquals(0L, messageLatch.getCount());
            Assert.assertEquals(SENT_MESSAGE, receivedMessage);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            ProxySelector.setDefault(defaultProxySelector);

            stopServer(server);
        }
    }
}
