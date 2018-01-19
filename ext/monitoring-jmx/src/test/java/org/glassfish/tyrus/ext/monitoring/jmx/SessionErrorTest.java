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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
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

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Tests that error statistics collected at session level are accessible. Complementary test to {@link
 * org.glassfish.tyrus.ext.monitoring.jmx.ErrorStatisticsTest}
 *
 * @author Petr Janouch
 */
public class SessionErrorTest extends TestContainer {

    @ServerEndpoint("/annotatedServerEndpoint")
    public static class AnnotatedServerEndpoint {

        @OnMessage
        public void onTextMessage(String message, Session session) throws Exception {
            throw new Exception();
        }

        @OnError
        public void onError(Throwable t) {
        }
    }

    @ClientEndpoint
    public static class AnnotatedClientEndpoint {
    }

    @Test
    public void test() {
        Server server = null;
        try {
            setContextPath("/errorInSessionTestApp");

            CountDownLatch errorCountDownLatch = new CountDownLatch(1);

            ApplicationEventListener applicationEventListener =
                    new TestApplicationEventListener(new SessionAwareApplicationMonitor(), null, null, null, null,
                                                     errorCountDownLatch);
            getServerProperties().put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, applicationEventListener);
            server = startServer(AnnotatedServerEndpoint.class);

            ClientManager client = createClient();
            Session session =
                    client.connectToServer(AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class));
            session.getBasicRemote().sendText("Hello");

            assertTrue(errorCountDownLatch.await(1, TimeUnit.SECONDS));

            String applicationMXBeanName = "org.glassfish.tyrus:type=/errorInSessionTestApp";
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ApplicationMXBean applicationMXBean =
                    JMX.newMXBeanProxy(mBeanServer, new ObjectName(applicationMXBeanName), ApplicationMXBean.class);

            List<EndpointMXBean> endpointMXBeans = applicationMXBean.getEndpointMXBeans();
            assertEquals(1, endpointMXBeans.size());
            List<SessionMXBean> sessionMXBeans = endpointMXBeans.get(0).getSessionMXBeans();
            assertEquals(1, sessionMXBeans.size());

            SessionMXBean sessionMXBean = sessionMXBeans.get(0);
            assertEquals(1, sessionMXBean.getErrorCounts().size());

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }
}
