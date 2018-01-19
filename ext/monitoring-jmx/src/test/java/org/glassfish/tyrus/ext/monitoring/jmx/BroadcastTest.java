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
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.TyrusSession;
import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Test that broadcasted messages are included in monitoring statistics.
 * <p/>
 * 3 clients connect to a server and one of the clients sends a message that will cause the server to broadcast 2 text
 * messages and one binary message. Then it is checked that sending of those 3 x (2 + 1) messages has been included in
 * server monitoring statistics.
 *
 * @author Petr Janouch
 */
public class BroadcastTest extends TestContainer {

    public static final String TEXT_MESSAGE_1 = "Hello";
    public static final String TEXT_MESSAGE_2 = "Hello again";
    public static final ByteBuffer BINARY_MESSAGE = ByteBuffer.wrap("Hello".getBytes());

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
            setContextPath("/monitoringBroadcastApp");

            ApplicationMonitor applicationMonitor;
            if (monitorOnSessionLevel) {
                applicationMonitor = new SessionAwareApplicationMonitor();
            } else {
                applicationMonitor = new SessionlessApplicationMonitor();
            }

            int sessionsCount = 3;

            // each sessions gets 3 messages - 2 text messages and 1 binary
            CountDownLatch sentMessagesLatch = new CountDownLatch(sessionsCount * 3);

            ApplicationEventListener applicationEventListener =
                    new TestApplicationEventListener(applicationMonitor, null, null, sentMessagesLatch, null, null);
            getServerProperties().put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, applicationEventListener);
            server = startServer(AnnotatedServerEndpoint.class);

            ClientManager client = createClient();
            Session session = null;
            for (int i = 0; i < sessionsCount; i++) {
                session = client.connectToServer(AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class));
            }

            // send different number of messages of each type so that it can be verified that a correct MXBean is
            // accessed
            session.getBasicRemote().sendText("Broadcast request");

            assertTrue(sentMessagesLatch.await(5, TimeUnit.SECONDS));

            String applicationMxBeanName = "org.glassfish.tyrus:type=/monitoringBroadcastApp";
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ApplicationMXBean applicationMXBean =
                    JMX.newMXBeanProxy(mBeanServer, new ObjectName(applicationMxBeanName), ApplicationMXBean.class);

            assertEquals(9, applicationMXBean.getSentMessagesCount());
            assertEquals(3, applicationMXBean.getBinaryMessageStatisticsMXBean().getSentMessagesCount());
            assertEquals(6, applicationMXBean.getTextMessageStatisticsMXBean().getSentMessagesCount());

            // check message sizes get monitored properly
            assertEquals((TEXT_MESSAGE_1.length() + TEXT_MESSAGE_2.length()) / 2,
                         applicationMXBean.getTextMessageStatisticsMXBean().getAverageSentMessageSize());
            assertEquals(BINARY_MESSAGE.limit(),
                         applicationMXBean.getBinaryMessageStatisticsMXBean().getAverageSentMessageSize());

            List<EndpointMXBean> endpointMXBeans = applicationMXBean.getEndpointMXBeans();
            assertEquals(1, endpointMXBeans.size());

            EndpointMXBean endpointMXBean = endpointMXBeans.get(0);

            assertEquals(9, endpointMXBean.getSentMessagesCount());
            assertEquals(3, endpointMXBean.getBinaryMessageStatisticsMXBean().getSentMessagesCount());
            assertEquals(6, endpointMXBean.getTextMessageStatisticsMXBean().getSentMessagesCount());

            assertEquals((TEXT_MESSAGE_1.length() + TEXT_MESSAGE_2.length()) / 2,
                         endpointMXBean.getTextMessageStatisticsMXBean().getAverageSentMessageSize());
            assertEquals(BINARY_MESSAGE.limit(),
                         endpointMXBean.getBinaryMessageStatisticsMXBean().getAverageSentMessageSize());

            List<SessionMXBean> sessionMXBeans = endpointMXBean.getSessionMXBeans();
            if (!monitorOnSessionLevel) {
                assertTrue(sessionMXBeans.isEmpty());
                return;
            }

            assertEquals(3, sessionMXBeans.size());
            for (BaseMXBean sessionMXBean : sessionMXBeans) {
                assertEquals(3, sessionMXBean.getSentMessagesCount());
                assertEquals(1, sessionMXBean.getBinaryMessageStatisticsMXBean().getSentMessagesCount());
                assertEquals(2, sessionMXBean.getTextMessageStatisticsMXBean().getSentMessagesCount());

                assertEquals((TEXT_MESSAGE_1.length() + TEXT_MESSAGE_2.length()) / 2,
                             sessionMXBean.getTextMessageStatisticsMXBean().getAverageSentMessageSize());
                assertEquals(BINARY_MESSAGE.limit(),
                             sessionMXBean.getBinaryMessageStatisticsMXBean().getAverageSentMessageSize());
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    @ServerEndpoint("/broadcastEndpoint")
    public static class AnnotatedServerEndpoint {

        @OnMessage
        public void onMessage(Session session, String text) {
            TyrusSession tyrusSession = (TyrusSession) session;
            tyrusSession.broadcast(TEXT_MESSAGE_1);
            tyrusSession.broadcast(TEXT_MESSAGE_2);
            tyrusSession.broadcast(BINARY_MESSAGE);
        }
    }

    @ClientEndpoint
    public static class AnnotatedClientEndpoint {

        @OnMessage
        public void onMessage(String message, Session session) {
        }

        @OnMessage
        public void onMessage(ByteBuffer data, Session session) {
        }
    }
}
