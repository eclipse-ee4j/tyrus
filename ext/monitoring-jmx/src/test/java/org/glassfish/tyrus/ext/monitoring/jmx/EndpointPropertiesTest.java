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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.test.tools.TestContainer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests that endpoint path and class name are accessible through Endpoint MXBean for both programmatic and annotated
 * endpoint.
 *
 * @author Petr Janouch
 */
public class EndpointPropertiesTest extends TestContainer {

    @ServerEndpoint("/annotatedEndpoint")
    public static class AnnotatedServerEndpoint {

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
        setContextPath("/jmxSessionTestApp");
        Server server = null;
        try {
            if (monitorOnSessionLevel) {
                getServerProperties().put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, new
                        SessionAwareApplicationMonitor());
            } else {
                getServerProperties().put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, new
                        SessionlessApplicationMonitor());
            }
            server = startServer(ApplicationConfig.class);
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

            String fullAnnotatedEndpointBeanName = "org.glassfish.tyrus:type=/jmxSessionTestApp,endpoints=endpoints,"
                    + "endpoint=/annotatedEndpoint";
            EndpointMXBean annotatedEndpointBean = JMX.newMXBeanProxy(
                    mBeanServer, new ObjectName(fullAnnotatedEndpointBeanName), EndpointMXBean.class);
            assertEquals(AnnotatedServerEndpoint.class.getName(), annotatedEndpointBean.getEndpointClassName());
            assertEquals("/annotatedEndpoint", annotatedEndpointBean.getEndpointPath());

            String fullProgrammaticEndpointBeanName = "org.glassfish.tyrus:type=/jmxSessionTestApp,"
                    + "endpoints=endpoints,endpoint=/programmaticEndpoint";
            EndpointMXBean programmaticEndpointBean = JMX.newMXBeanProxy(
                    mBeanServer, new ObjectName(fullProgrammaticEndpointBeanName), EndpointMXBean.class);
            assertEquals(ApplicationConfig.ProgrammaticServerEndpoint.class.getName(), programmaticEndpointBean
                    .getEndpointClassName());
            assertEquals("/programmaticEndpoint", programmaticEndpointBean.getEndpointPath());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            stopServer(server);
        }
    }

    public static class ApplicationConfig implements ServerApplicationConfig {

        @Override
        public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
            return new HashSet<ServerEndpointConfig>() {
                {
                    add(ServerEndpointConfig.Builder.create(ProgrammaticServerEndpoint.class,
                                                            "/programmaticEndpoint").build());
                }
            };
        }

        @Override
        public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {

            return new HashSet<Class<?>>(Collections.singleton(AnnotatedServerEndpoint.class));
        }

        public static class ProgrammaticServerEndpoint extends Endpoint {
            @Override
            public void onOpen(final Session session, final EndpointConfig EndpointConfig) {

            }
        }
    }
}
