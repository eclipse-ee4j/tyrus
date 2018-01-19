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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests that statistics about different types of messages are collected correctly.
 *
 * @author Petr Janouch
 */
public class MessageTypesTest extends TestContainer {

    @ServerEndpoint("/jmxServerEndpoint")
    public static class AnnotatedServerEndpoint {

        @OnMessage
        public void messageReceived(Session session, String text) throws IOException {
            session.getBasicRemote().sendText(text);
            session.getBasicRemote().sendText(text);
        }

        @OnMessage
        public void messageReceived(Session session, ByteBuffer data) throws IOException {
            session.getBasicRemote().sendBinary(data);
            session.getBasicRemote().sendBinary(data);
        }

        @OnMessage
        public void messageReceived(Session session, PongMessage pong) throws IOException {
            session.getBasicRemote().sendPong(pong.getApplicationData());
            session.getBasicRemote().sendPong(pong.getApplicationData());
        }
    }

    @ClientEndpoint
    public static class AnnotatedClientEndpoint {

        @OnMessage
        public void messageReceived(Session session, String text) {
        }

        @OnMessage
        public void messageReceived(Session session, ByteBuffer data) {
        }

        @OnMessage
        public void messageReceived(Session session, PongMessage pong) {
        }
    }

    @Test
    public void monitoringOnSessionLevelTest() {
        test(true);
    }

    @Test
    public void monitoringOnEndpointLevelTest() {
        test(false);
    }

    public void test(boolean monitorOnSessionLevel) {
        Server server = null;
        try {
            /*
             Latches used to ensure all messages were sent or received by the server, before the statistics are checked.
             For every pong, text and binary message sent, 2 are received. For every ping message sent, 1 pong is
             received.
             */
            CountDownLatch messageSentLatch = new CountDownLatch(17);
            CountDownLatch messageReceivedLatch = new CountDownLatch(11);

            setContextPath("/jmxTestApp");

            ApplicationMonitor applicationMonitor;
            if (monitorOnSessionLevel) {
                applicationMonitor = new SessionAwareApplicationMonitor();
            } else {
                applicationMonitor = new SessionlessApplicationMonitor();
            }
            ApplicationEventListener applicationEventListener =
                    new TestApplicationEventListener(applicationMonitor, null, null, messageSentLatch,
                                                     messageReceivedLatch, null);
            getServerProperties().put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, applicationEventListener);
            server = startServer(AnnotatedServerEndpoint.class);

            ClientManager client = createClient();
            Session session =
                    client.connectToServer(AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class));

            session.getBasicRemote().sendText("some text");
            session.getBasicRemote().sendText("some text", false);
            session.getBasicRemote().sendText("some text", true);
            session.getAsyncRemote().sendText("some text").get();

            session.getBasicRemote().sendBinary(ByteBuffer.wrap("some text".getBytes()));
            session.getBasicRemote().sendBinary(ByteBuffer.wrap("some text".getBytes()));
            session.getBasicRemote().sendBinary(ByteBuffer.wrap("some text".getBytes()), false);
            session.getBasicRemote().sendBinary(ByteBuffer.wrap("some text".getBytes()), true);
            session.getAsyncRemote().sendBinary(ByteBuffer.wrap("some text".getBytes())).get();

            session.getBasicRemote().sendPing(null);
            session.getBasicRemote().sendPong(null);

            assertTrue(messageSentLatch.await(1, TimeUnit.SECONDS));
            assertTrue(messageReceivedLatch.await(1, TimeUnit.SECONDS));

            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            String endpointMxBeanNameBase =
                    "org.glassfish.tyrus:type=/jmxTestApp,endpoints=endpoints,endpoint=/jmxServerEndpoint";
            String messageStatisticsNameBase = endpointMxBeanNameBase + ",message_statistics=message_statistics";

            EndpointMXBean endpointBean =
                    JMX.newMXBeanProxy(mBeanServer, new ObjectName(endpointMxBeanNameBase), EndpointMXBean.class);
            MessageStatisticsMXBean textBean =
                    JMX.newMXBeanProxy(mBeanServer, new ObjectName(messageStatisticsNameBase + ",message_type=text"),
                                       MessageStatisticsMXBean.class);
            MessageStatisticsMXBean binaryBean =
                    JMX.newMXBeanProxy(mBeanServer, new ObjectName(messageStatisticsNameBase + ",message_type=binary"),
                                       MessageStatisticsMXBean.class);
            MessageStatisticsMXBean controlBean =
                    JMX.newMXBeanProxy(mBeanServer, new ObjectName(messageStatisticsNameBase + ",message_type=control"),
                                       MessageStatisticsMXBean.class);

            assertEquals(11, endpointBean.getReceivedMessagesCount());
            assertEquals(17, endpointBean.getSentMessagesCount());
            assertEquals(4, textBean.getReceivedMessagesCount());
            assertEquals(6, textBean.getSentMessagesCount());
            assertEquals(5, binaryBean.getReceivedMessagesCount());
            assertEquals(8, binaryBean.getSentMessagesCount());
            assertEquals(2, controlBean.getReceivedMessagesCount());
            assertEquals(3, controlBean.getSentMessagesCount());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }
}
