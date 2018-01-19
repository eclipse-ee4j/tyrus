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

package org.glassfish.tyrus.tests.servlet.oneappconfig;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests correct deployment of one {@link javax.websocket.server.ServerApplicationConfig}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class OneServerApplicationConfigDeployTest extends TestContainer {

    private static final String CONTEXT_PATH = "/oneappconfig-test";

    public OneServerApplicationConfigDeployTest() {
        setContextPath(CONTEXT_PATH);
    }

    @Test
    public void oneServerAppConfigEcho() throws DeploymentException, InterruptedException, IOException {
        final Server server =
                startServer(PlainEcho.class, PlainOne.class, PlainTwo.class, TestServerApplicationConfig.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                Assert.assertEquals(message, "Do or do not, there is no try.");
                                messageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText("Do or do not, there is no try.");
                    } catch (IOException e) {
                        // do nothing
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(),
                                   getURI(PlainEcho.class.getAnnotation(ServerEndpoint.class).value()));

            messageLatch.await(1, TimeUnit.SECONDS);
            Assert.assertEquals(0, messageLatch.getCount());
        } finally {
            stopServer(server);
        }
    }

    @Test
    public void oneServerAppConfigOne() throws DeploymentException, InterruptedException, IOException {
        final Server server =
                startServer(PlainEcho.class, PlainOne.class, PlainTwo.class, TestServerApplicationConfig.class);

        final CountDownLatch messageLatch = new CountDownLatch(1);

        try {
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {
                            @Override
                            public void onMessage(String message) {
                                Assert.assertEquals(message, "1");
                                messageLatch.countDown();
                            }
                        });

                        session.getBasicRemote().sendText("Do or do not, there is no try.");
                    } catch (IOException e) {
                        // do nothing
                    }
                }
            }, ClientEndpointConfig.Builder.create().build(),
                                   getURI(PlainOne.class.getAnnotation(ServerEndpoint.class).value()));

            messageLatch.await(1, TimeUnit.SECONDS);
            Assert.assertEquals(0, messageLatch.getCount());
        } finally {
            stopServer(server);
        }
    }
}
