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

package org.glassfish.tyrus.ext.monitoring.jmx;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Tests that MXBean tree can be traversed - all MXBeans can be accessed from {@link
 * org.glassfish.tyrus.ext.monitoring.jmx.ApplicationMXBean}
 *
 * @author Petr Janouch
 */
public class MBeanTreeTest extends TestContainer {

    @ServerEndpoint("/serverEndpoint")
    public static class AnnotatedServerEndpoint {

        @OnMessage
        public void onMessage(String message, Session session) {
        }

        @OnMessage
        public void onMessage(ByteBuffer data, Session session) {
        }

        @OnMessage
        public void onMessage(PongMessage pong, Session session) {
        }
    }

    @ClientEndpoint
    public static class AnnotatedClientEndpoint {
    }

    @Test
    public void monitoringOnSessionLevelTest() {
        test(true);
    }

    @Test
    public void monitoringOnEndpointLevelTest() {
        test(false);
    }

    private void test(boolean monitorOnSessionLevel) {
        Server server = null;
        try {
            setContextPath("/mBeanTreeTestApp");

            ApplicationMonitor applicationMonitor;
            if (monitorOnSessionLevel) {
                applicationMonitor = new SessionAwareApplicationMonitor();
            } else {
                applicationMonitor = new SessionlessApplicationMonitor();
            }

            CountDownLatch receivedMessagesLatch = new CountDownLatch(6);

            ApplicationEventListener applicationEventListener =
                    new TestApplicationEventListener(applicationMonitor, null, null, null, receivedMessagesLatch, null);
            getServerProperties().put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, applicationEventListener);
            server = startServer(AnnotatedServerEndpoint.class);

            ClientManager client = createClient();
            Session session =
                    client.connectToServer(AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class));

            // send different number of messages of each type so that it can be verified that a correct MXBean is
            // accessed
            session.getBasicRemote().sendBinary(ByteBuffer.wrap("Hello".getBytes()));
            session.getBasicRemote().sendText("Hello 1");
            session.getBasicRemote().sendText("Hello 2");
            session.getBasicRemote().sendPong(null);
            session.getBasicRemote().sendPong(null);
            session.getBasicRemote().sendPong(null);

            assertTrue(receivedMessagesLatch.await(1, TimeUnit.SECONDS));

            String applicationMxBeanName = "org.glassfish.tyrus:type=/mBeanTreeTestApp";
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ApplicationMXBean applicationMXBean =
                    JMX.newMXBeanProxy(mBeanServer, new ObjectName(applicationMxBeanName), ApplicationMXBean.class);

            assertEquals(1, applicationMXBean.getBinaryMessageStatisticsMXBean().getReceivedMessagesCount());
            assertEquals(2, applicationMXBean.getTextMessageStatisticsMXBean().getReceivedMessagesCount());
            assertEquals(3, applicationMXBean.getControlMessageStatisticsMXBean().getReceivedMessagesCount());

            List<EndpointMXBean> endpointMXBeans = applicationMXBean.getEndpointMXBeans();
            assertEquals(1, endpointMXBeans.size());

            EndpointMXBean endpointMXBean = endpointMXBeans.get(0);

            assertEquals(1, endpointMXBean.getBinaryMessageStatisticsMXBean().getReceivedMessagesCount());
            assertEquals(2, endpointMXBean.getTextMessageStatisticsMXBean().getReceivedMessagesCount());
            assertEquals(3, endpointMXBean.getControlMessageStatisticsMXBean().getReceivedMessagesCount());

            List<SessionMXBean> sessionMXBeans = endpointMXBean.getSessionMXBeans();
            if (!monitorOnSessionLevel) {
                assertTrue(sessionMXBeans.isEmpty());
                return;
            }

            assertEquals(1, sessionMXBeans.size());
            BaseMXBean sessionMXBean = sessionMXBeans.get(0);

            assertEquals(1, sessionMXBean.getBinaryMessageStatisticsMXBean().getReceivedMessagesCount());
            assertEquals(2, sessionMXBean.getTextMessageStatisticsMXBean().getReceivedMessagesCount());
            assertEquals(3, sessionMXBean.getControlMessageStatisticsMXBean().getReceivedMessagesCount());

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }
}
