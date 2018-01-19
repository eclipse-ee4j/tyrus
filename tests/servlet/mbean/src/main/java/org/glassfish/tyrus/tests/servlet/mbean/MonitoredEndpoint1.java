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

package org.glassfish.tyrus.tests.servlet.mbean;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;

import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.glassfish.tyrus.ext.monitoring.jmx.ApplicationMXBean;
import org.glassfish.tyrus.ext.monitoring.jmx.EndpointClassNamePathPair;

/**
 * Endpoint that returns OK in @#onOpen if @MonitoredEndpoint1 and @ MonitoredEndpoint2 are registered in application
 * MBean.
 *
 * @author Petr Janouch
 */
@ServerEndpoint("/monitoredEndpoint1")
public class MonitoredEndpoint1 {

    @OnOpen
    public void onOpen(Session session) throws IOException {
        if (isEndpointRegistered(
                "/mbean-test", new EndpointClassNamePathPair("/monitoredEndpoint1", MonitoredEndpoint1.class.getName()))
                && isEndpointRegistered("/mbean-test",
                                        new EndpointClassNamePathPair("/monitoredEndpoint2",
                                                                      MonitoredEndpoint2.class.getName()))) {
            session.getBasicRemote().sendText("OK");
            return;
        }
        session.getBasicRemote().sendText("NOK");
    }

    private boolean isEndpointRegistered(String applicationName, EndpointClassNamePathPair endpoint) {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        String fullMxBeanName = "org.glassfish.tyrus:type=" + applicationName;
        ApplicationMXBean proxy;
        try {
            proxy = JMX.newMXBeanProxy(mBeanServer, new ObjectName(fullMxBeanName), ApplicationMXBean.class);
            List<EndpointClassNamePathPair> registeredEndpoints = proxy.getEndpoints();
            for (EndpointClassNamePathPair registeredEndpoint : registeredEndpoints) {
                if (registeredEndpoint.getEndpointPath().equals(endpoint.getEndpointPath())
                        && registeredEndpoint.getEndpointClassName().equals(endpoint.getEndpointClassName())) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
