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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Tyrus MXBeans publisher.
 *
 * @author Petr Janouch
 */
class MBeanPublisher {

    private static final Logger LOGGER = Logger.getLogger(MBeanPublisher.class.getName());
    private static final String DOMAIN = "org.glassfish.tyrus";
    private static final String APPLICATION_NAME_BASE = DOMAIN + ":type=";
    private static final String ENDPOINT_KEY = ",endpoint=";
    private static final String SESSION_KEY = ",session=";
    private static final String MESSAGE_TYPE_KEY = ",message_type=";
    private static final String TEXT = "text";
    private static final String BINARY = "binary";
    private static final String CONTROL = "control";
    private static final String SESSIONS_DIRECTORY = ",sessions=sessions";
    private static final String ENDPOINTS_DIRECTORY = ",endpoints=endpoints";
    private static final String MESSAGE_STATISTIC_DIRECTORY = ",message_statistics=message_statistics";

    /**
     * Register {@link org.glassfish.tyrus.ext.monitoring.jmx.ApplicationMXBean} and MXBeans exposing statistics
     * about text, binary and control messages.
     *
     * @param applicationName                name of the application.
     * @param applicationMXBean              MXBean exposing application-level statistics.
     * @param textMessageStatisticsMXBean    MXBean exposing statistics about text messages sent and received by the
     *                                       application.
     * @param binaryMessageStatisticsMXBean  MXBean exposing statistics about binary messages sent and received by the
     *                                       application.
     * @param controlMessageStatisticsMXBean MXBean exposing statistics about control messages sent and received by the
     *                                       application.
     */
    static void registerApplicationMXBeans(String applicationName, ApplicationMXBean applicationMXBean,
                                           MessageStatisticsMXBean textMessageStatisticsMXBean,
                                           MessageStatisticsMXBean binaryMessageStatisticsMXBean,
                                           MessageStatisticsMXBean controlMessageStatisticsMXBean) {
        String nameBase = getApplicationBeansBaseName(applicationName);
        registerStatisticsMXBeans(nameBase, applicationMXBean, textMessageStatisticsMXBean,
                                  binaryMessageStatisticsMXBean, controlMessageStatisticsMXBean);
    }

    /**
     * Register {@link org.glassfish.tyrus.ext.monitoring.jmx.EndpointMXBean} and MXBeans exposing statistics.
     * about text, binary and control messages sent and received by the endpoint.
     *
     * @param applicationName                application name.
     * @param endpointPath                   endpoint path.
     * @param endpointMXBean                 MXBean exposing endpoint-level statistics.
     * @param textMessageStatisticsMXBean    MXBean exposing statistics about text messages sent and received by the
     *                                       endpoint.
     * @param binaryMessageStatisticsMXBean  MXBean exposing statistics about binary messages sent and received by the
     *                                       endpoint.
     * @param controlMessageStatisticsMXBean MXBean exposing statistics about control messages sent and received by the
     *                                       endpoint
     */
    static void registerEndpointMXBeans(String applicationName, String endpointPath, EndpointMXBean endpointMXBean,
                                        MessageStatisticsMXBean textMessageStatisticsMXBean,
                                        MessageStatisticsMXBean binaryMessageStatisticsMXBean,
                                        MessageStatisticsMXBean controlMessageStatisticsMXBean) {
        String nameBase = getEndpointBeansBaseName(applicationName, endpointPath);
        registerStatisticsMXBeans(nameBase, endpointMXBean, textMessageStatisticsMXBean, binaryMessageStatisticsMXBean,
                                  controlMessageStatisticsMXBean);
    }

    /**
     * Register MXBeans exposing statistics about text, binary, control and total messages sent and received by the
     * session.
     *
     * @param applicationName                application name.
     * @param endpointPath                   endpoint path.
     * @param sessionId                      session ID.
     * @param sessionMXBean                  MXBean exposing statistics about total messages sent and received by the
     *                                       session.
     * @param textMessageStatisticsMXBean    MXBean exposing statistics about text messages sent and received by the
     *                                       session.
     * @param binaryMessageStatisticsMXBean  MXBean exposing statistics about binary messages sent and received by the
     *                                       session.
     * @param controlMessageStatisticsMXBean MXBean exposing statistics about control messages sent and received by the
     *                                       session.
     */
    static void registerSessionMXBeans(String applicationName, String endpointPath, String sessionId,
                                       MessageStatisticsMXBean sessionMXBean,
                                       MessageStatisticsMXBean textMessageStatisticsMXBean,
                                       MessageStatisticsMXBean binaryMessageStatisticsMXBean,
                                       MessageStatisticsMXBean controlMessageStatisticsMXBean) {
        String baseName = getSessionBeansBaseName(applicationName, endpointPath, sessionId);
        registerStatisticsMXBeans(baseName, sessionMXBean, textMessageStatisticsMXBean, binaryMessageStatisticsMXBean,
                                  controlMessageStatisticsMXBean);
    }

    /**
     * Unregister MXBeans of the given application including all the endpoint and sessions MXBeans.
     *
     * @param applicationName application name.
     */
    static void unregisterApplicationMXBeans(String applicationName) {
        String name = getApplicationBeansBaseName(applicationName);
        unregisterMXBean(name);
    }

    /**
     * Unregister MXBeans of the given endpoint including all the sessions MXBeans.
     *
     * @param applicationName application name.
     * @param endpointPath    endpoint path.
     */
    static void unregisterEndpointMXBeans(String applicationName, String endpointPath) {
        String name = getEndpointBeansBaseName(applicationName, endpointPath);
        unregisterMXBean(name);
    }

    /**
     * Unregister an MXBeans of the given session.
     *
     * @param applicationName application name.
     * @param endpointPath    endpoint path.
     * @param sessionId       session ID.
     */
    static void unregisterSessionMXBeans(String applicationName, String endpointPath, String sessionId) {
        String name = getSessionBeansBaseName(applicationName, endpointPath, sessionId);
        unregisterMXBean(name);
    }

    private static void registerStatisticsMXBeans(String nameBase, MessageStatisticsMXBean nodeMXBean,
                                                  MessageStatisticsMXBean textMessageStatisticsMXBean,
                                                  MessageStatisticsMXBean binaryMessageStatisticsMXBean,
                                                  MessageStatisticsMXBean controlMessageStatisticsMXBean) {
        registerMXBean(nameBase, nodeMXBean);
        String name = nameBase + MESSAGE_STATISTIC_DIRECTORY + MESSAGE_TYPE_KEY + TEXT;
        registerMXBean(name, textMessageStatisticsMXBean);
        name = nameBase + MESSAGE_STATISTIC_DIRECTORY + MESSAGE_TYPE_KEY + BINARY;
        registerMXBean(name, binaryMessageStatisticsMXBean);
        name = nameBase + MESSAGE_STATISTIC_DIRECTORY + MESSAGE_TYPE_KEY + CONTROL;
        registerMXBean(name, controlMessageStatisticsMXBean);
    }

    private static String getApplicationBeansBaseName(String applicationName) {
        return APPLICATION_NAME_BASE + applicationName;
    }

    private static String getEndpointBeansBaseName(String applicationName, String endpointPath) {
        return getApplicationBeansBaseName(applicationName) + ENDPOINTS_DIRECTORY + ENDPOINT_KEY + endpointPath;
    }

    private static String getSessionBeansBaseName(String applicationName, String endpointPath, String sessionId) {
        return getEndpointBeansBaseName(applicationName, endpointPath) + SESSIONS_DIRECTORY + SESSION_KEY + sessionId;
    }

    private static void registerMXBean(String name, Object mBean) {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            synchronized (MBeanPublisher.class) {
                final ObjectName objectName = new ObjectName(name);
                if (mBeanServer.isRegistered(objectName)) {
                    LOGGER.log(Level.WARNING, "MXBean with name " + " already registered");
                } else {
                    mBeanServer.registerMBean(mBean, objectName);
                }
            }
        } catch (JMException e) {
            LOGGER.log(Level.WARNING, "Could not register MXBean with name " + name, e);
        }
    }

    private static void unregisterMXBean(String name) {
        name = name + ",*";
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            synchronized (MBeanPublisher.class) {
                for (ObjectName objectName : mBeanServer.queryNames(new ObjectName(name), null)) {
                    mBeanServer.unregisterMBean(objectName);
                }
            }
        } catch (JMException e) {
            LOGGER.log(Level.WARNING, "Could not unregister MXBeans with name " + name, e);
        }
    }
}
