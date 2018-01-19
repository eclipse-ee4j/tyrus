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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.DeploymentException;
import javax.websocket.server.ServerEndpoint;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests that MXBeans exposing information about registered endpoints get registered and contain information about
 * deployed endpoint classes and paths.
 *
 * @author Petr Janouch
 */
public class RegisteredEndpointsTest extends TestContainer {

    @ServerEndpoint("/jmxServerEndpoint1")
    public static class AnnotatedServerEndpoint1 {
    }

    @ServerEndpoint("/jmxServerEndpoint2")
    public static class AnnotatedServerEndpoint2 {
    }

    @ServerEndpoint("/jmxServerEndpoint3")
    public static class AnnotatedServerEndpoint3 {
    }

    @Test
    public void testJmx() {
        Server server1 = null;
        Server server2 = null;
        try {
            Map<String, Object> server1Properties = new HashMap<String, Object>();
            ApplicationEventListener application1EventListener = new SessionAwareApplicationMonitor();
            server1Properties.put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, application1EventListener);
            server1 = new Server("localhost", 8025, "/jmxTestApp", server1Properties, AnnotatedServerEndpoint1.class,
                                 AnnotatedServerEndpoint2.class);
            server1.start();

            Map<String, Object> server2Properties = new HashMap<String, Object>();
            server2Properties
                    .put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, new SessionAwareApplicationMonitor());
            server2 = new Server("localhost", 8026, "/jmxTestApp2", server2Properties, AnnotatedServerEndpoint2.class,
                                 AnnotatedServerEndpoint3.class);
            server2.start();

            // test all endpoints are registered
            assertTrue(isEndpointRegistered(
                    "/jmxTestApp",
                    new EndpointClassNamePathPair("/jmxServerEndpoint1", AnnotatedServerEndpoint1.class.getName())));
            assertTrue(isEndpointRegistered(
                    "/jmxTestApp",
                    new EndpointClassNamePathPair("/jmxServerEndpoint2", AnnotatedServerEndpoint2.class.getName())));
            assertTrue(isEndpointRegistered(
                    "/jmxTestApp2",
                    new EndpointClassNamePathPair("/jmxServerEndpoint2", AnnotatedServerEndpoint2.class.getName())));
            assertTrue(isEndpointRegistered(
                    "/jmxTestApp2",
                    new EndpointClassNamePathPair("/jmxServerEndpoint3", AnnotatedServerEndpoint3.class.getName())));

            // test endpoint gets unregistered
            application1EventListener.onEndpointUnregistered("/jmxServerEndpoint2");
            assertTrue(isEndpointRegistered(
                    "/jmxTestApp",
                    new EndpointClassNamePathPair("/jmxServerEndpoint1", AnnotatedServerEndpoint1.class.getName())));
            assertFalse(isEndpointRegistered(
                    "/jmxTestApp",
                    new EndpointClassNamePathPair("/jmxServerEndpoint2", AnnotatedServerEndpoint2.class.getName())));
            assertTrue(isEndpointRegistered(
                    "/jmxTestApp2",
                    new EndpointClassNamePathPair("/jmxServerEndpoint2", AnnotatedServerEndpoint2.class.getName())));
            assertTrue(isEndpointRegistered(
                    "/jmxTestApp2",
                    new EndpointClassNamePathPair("/jmxServerEndpoint3", AnnotatedServerEndpoint3.class.getName())));

            // test jmx of one applications is terminated
            server2.stop();
            assertTrue(isEndpointRegistered(
                    "/jmxTestApp",
                    new EndpointClassNamePathPair("/jmxServerEndpoint1", AnnotatedServerEndpoint1.class.getName())));
            assertFalse(isEndpointRegistered(
                    "/jmxTestApp",
                    new EndpointClassNamePathPair("/jmxServerEndpoint2", AnnotatedServerEndpoint2.class.getName())));
            assertFalse(isEndpointRegistered(
                    "/jmxTestApp2",
                    new EndpointClassNamePathPair("/jmxServerEndpoint2", AnnotatedServerEndpoint2.class.getName())));
            assertFalse(isEndpointRegistered(
                    "/jmxTestApp2",
                    new EndpointClassNamePathPair("/jmxServerEndpoint3", AnnotatedServerEndpoint3.class.getName())));

        } catch (DeploymentException e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server1);
            stopServer(server2);
        }
    }

    private boolean isEndpointRegistered(String applicationName, EndpointClassNamePathPair endpoint) {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        String fullMxBeanName = "org.glassfish.tyrus:type=" + applicationName;
        ApplicationMXBean proxy;
        boolean result = false;
        try {
            proxy = JMX.newMXBeanProxy(mBeanServer, new ObjectName(fullMxBeanName), ApplicationMXBean.class);
            List<EndpointClassNamePathPair> registeredEndpoints = proxy.getEndpoints();
            for (EndpointClassNamePathPair registeredEndpoint : registeredEndpoints) {
                if (registeredEndpoint.getEndpointPath().equals(endpoint.getEndpointPath())
                        && registeredEndpoint.getEndpointClassName().equals(endpoint.getEndpointClassName())) {
                    result = true;
                    break;
                }
            }

            if (!proxy.getEndpointPaths().contains(endpoint.getEndpointPath())) {
                result = false;
            }
        } catch (MalformedObjectNameException e) {
            System.out.print("Could not retrieve MXBean for application " + applicationName + ": " + e.getMessage());
        } catch (Exception e) {
            // do nothing false will be returned
        }
        return result;
    }
}
