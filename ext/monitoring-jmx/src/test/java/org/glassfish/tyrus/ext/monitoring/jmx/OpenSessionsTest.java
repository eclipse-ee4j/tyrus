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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;
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
 * Tests that open sessions statistics (number of currently open sessions and maximal number of open sessions since
 * the beginning of monitoring) are collected correctly.
 *
 * @author Petr Janouch (petr.janouch at oracle.com
 */
public class OpenSessionsTest extends TestContainer {

    @ServerEndpoint("/jmxSessionStatisticsEndpoint")
    public static class AnnotatedServerEndpoint {

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
        ClientManager client = createClient();
        setContextPath("/jmxSessionTestApp");
        Server server = null;
        try {
            CountDownLatch sessionClosedLatch = new CountDownLatch(1);
            CountDownLatch sessionOpenedLatch = new CountDownLatch(3);

            ApplicationMonitor applicationMonitor;
            if (monitorOnSessionLevel) {
                applicationMonitor = new SessionAwareApplicationMonitor();
            } else {
                applicationMonitor = new SessionlessApplicationMonitor();
            }
            ApplicationEventListener applicationEventListener =
                    new TestApplicationEventListener(applicationMonitor, sessionOpenedLatch, sessionClosedLatch, null,
                                                     null, null);
            getServerProperties().put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, applicationEventListener);
            server = startServer(AnnotatedServerEndpoint.class);

            client.connectToServer(AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class));
            Session session2 =
                    client.connectToServer(AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class));
            client.connectToServer(AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class));
            assertTrue(sessionOpenedLatch.await(1, TimeUnit.SECONDS));
            session2.close();
            assertTrue(sessionClosedLatch.await(1, TimeUnit.SECONDS));

            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            String fullApplicationMXBeanName = "org.glassfish.tyrus:type=/jmxSessionTestApp";
            ApplicationMXBean applicationBean =
                    JMX.newMXBeanProxy(mBeanServer, new ObjectName(fullApplicationMXBeanName), ApplicationMXBean.class);
            assertEquals(2, applicationBean.getOpenSessionsCount());
            assertEquals(3, applicationBean.getMaximalOpenSessionsCount());

            String fullEndpointMXBeanName =
                    "org.glassfish.tyrus:type=/jmxSessionTestApp,endpoints=endpoints,"
                            + "endpoint=/jmxSessionStatisticsEndpoint";
            EndpointMXBean endpointBean =
                    JMX.newMXBeanProxy(mBeanServer, new ObjectName(fullEndpointMXBeanName), EndpointMXBean.class);
            assertEquals(2, endpointBean.getOpenSessionsCount());
            assertEquals(3, endpointBean.getMaximalOpenSessionsCount());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }
}
