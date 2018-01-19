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
import java.util.HashSet;
import java.util.Set;
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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Tests that Session related MXBeans are cleaned after session is closed.
 *
 * @author Petr Janouch
 */
public class SessionCleanUpTest extends TestContainer {

    @ServerEndpoint("/serverEndpoint")
    public static class AnnotatedServerEndpoint {

    }

    @ClientEndpoint
    public static class AnnotatedClientEndpoint {

    }

    @Test
    public void test() {
        Server server = null;
        try {
            setContextPath("/serializationTestApp");

            CountDownLatch sessionOpenedLatch = new CountDownLatch(1);
            CountDownLatch sessionClosedLatch = new CountDownLatch(1);
            ApplicationEventListener applicationEventListener =
                    new TestApplicationEventListener(new SessionAwareApplicationMonitor(), sessionOpenedLatch,
                                                     sessionClosedLatch, null, null, null);
            getServerProperties().put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, applicationEventListener);
            server = startServer(AnnotatedServerEndpoint.class);

            ClientManager client = createClient();
            Session session =
                    client.connectToServer(AnnotatedClientEndpoint.class, getURI(AnnotatedServerEndpoint.class));
            assertTrue(sessionOpenedLatch.await(1, TimeUnit.SECONDS));

            String applicationMxBeanName = "org.glassfish.tyrus:type=/serializationTestApp";
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ApplicationMXBean applicationMXBean =
                    JMX.newMXBeanProxy(mBeanServer, new ObjectName(applicationMxBeanName), ApplicationMXBean.class);

            assertEquals(1, applicationMXBean.getEndpointMXBeans().size());
            EndpointMXBean endpointMXBean = applicationMXBean.getEndpointMXBeans().get(0);

            assertEquals(1, endpointMXBean.getSessionMXBeans().size());
            SessionMXBean sessionMXBean = endpointMXBean.getSessionMXBeans().get(0);

            Set<String> registeredMXBeanNames = getRegisteredMXBeanNames();
            String sessionMXBeanName =
                    "org.glassfish.tyrus:type=/serializationTestApp,endpoints=endpoints,endpoint=/serverEndpoint,"
                            + "sessions=sessions,session=" + sessionMXBean.getSessionId();
            String sessionMessageTypesNameBase =
                    sessionMXBeanName + ",message_statistics=message_statistics,message_type=";
            assertTrue(registeredMXBeanNames.contains(sessionMXBeanName));
            assertTrue(registeredMXBeanNames.contains(sessionMessageTypesNameBase + "text"));
            assertTrue(registeredMXBeanNames.contains(sessionMessageTypesNameBase + "binary"));
            assertTrue(registeredMXBeanNames.contains(sessionMessageTypesNameBase + "control"));

            session.close();
            assertTrue(sessionClosedLatch.await(1, TimeUnit.SECONDS));

            assertTrue(endpointMXBean.getSessionMXBeans().isEmpty());
            registeredMXBeanNames = getRegisteredMXBeanNames();
            assertFalse(registeredMXBeanNames.contains(sessionMXBeanName));
            assertFalse(registeredMXBeanNames.contains(sessionMessageTypesNameBase + "text"));
            assertFalse(registeredMXBeanNames.contains(sessionMessageTypesNameBase + "binary"));
            assertFalse(registeredMXBeanNames.contains(sessionMessageTypesNameBase + "control"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    private Set<String> getRegisteredMXBeanNames() {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        Set<String> result = new HashSet<String>();
        for (ObjectName name : mBeanServer.queryNames(null, null)) {
            result.add(name.toString());
        }
        return result;
    }
}
