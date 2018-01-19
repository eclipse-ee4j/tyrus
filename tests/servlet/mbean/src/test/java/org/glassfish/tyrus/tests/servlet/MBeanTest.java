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

package org.glassfish.tyrus.tests.servlet;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.ext.monitoring.jmx.SessionlessApplicationMonitor;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;
import org.glassfish.tyrus.tests.servlet.mbean.MonitoredEndpoint1;
import org.glassfish.tyrus.tests.servlet.mbean.MonitoredEndpoint2;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests Application MBean was exposed.
 *
 * @author Petr Janouch
 */
public class MBeanTest extends TestContainer {

    @Test
    public void mBeanTests() {
        Server server = null;
        try {
            setContextPath("/mbean-test");
            getServerProperties()
                    .put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, new SessionlessApplicationMonitor());
            server = startServer(MonitoredEndpoint1.class, MonitoredEndpoint2.class);
            final CountDownLatch messageLatch = new CountDownLatch(1);
            final ClientManager client = createClient();
            client.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig EndpointConfig) {

                    session.addMessageHandler(new MessageHandler.Whole<String>() {
                        @Override
                        public void onMessage(String message) {
                            if (message.equals("OK")) {
                                messageLatch.countDown();
                            }
                        }
                    });

                }
            }, ClientEndpointConfig.Builder.create().build(), getURI(MonitoredEndpoint1.class));

            assertTrue(messageLatch.await(2, TimeUnit.SECONDS));

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }
}
