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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.core.monitoring.EndpointEventListener;

/**
 * Application events listener and application-level statistics collector.
 * The statistics are collected by aggregating statistics from application endpoints.
 * <p>
 * Creates and registers {@link org.glassfish.tyrus.ext.monitoring.jmx.ApplicationMXBean} MXBean that exposes these
 * application statistics. Also creates and registers {@link org.glassfish.tyrus.ext.monitoring.jmx
 * .MessageStatisticsMXBean} MXBean for exposing text, binary and control messages statistics.
 * <p>
 * Allows defining the level monitoring will be conducted on by setting parameter in constructor that determines
 * if message statistics should be collected on session level or not. In the former case statistics will be collected
 * and exposed in MXBeans per session, endpoint and application. In the latter case statistics will be collected
 * and exposed in MXBeans only per endpoint and application, which is the default setting.
 *
 * @author Petr Janouch
 * @see ApplicationEventListener
 */
class ApplicationMonitor extends BaseMonitor implements ApplicationEventListener, MessageListener {

    private final Map<String, EndpointMonitor> endpoints = new ConcurrentHashMap<String, EndpointMonitor>();
    private final AtomicInteger openSessionsCount = new AtomicInteger(0);
    private final Object maxOpenSessionsCountLock = new Object();
    private final boolean monitorOnSessionLevel;

    private final ConcurrentMessageStatistics sentTextMessageStatistics = new ConcurrentMessageStatistics();
    private final ConcurrentMessageStatistics sentBinaryMessageStatistics = new ConcurrentMessageStatistics();
    private final ConcurrentMessageStatistics sentControlMessageStatistics = new ConcurrentMessageStatistics();

    private final ConcurrentMessageStatistics receivedTextMessageStatistics = new ConcurrentMessageStatistics();
    private final ConcurrentMessageStatistics receivedBinaryMessageStatistics = new ConcurrentMessageStatistics();
    private final ConcurrentMessageStatistics receivedControlMessageStatistics = new ConcurrentMessageStatistics();

    private volatile int maxOpenSessionCount = 0;
    private volatile String applicationName;
    private volatile ApplicationMXBeanImpl applicationMXBean;

    /**
     * Constructor.
     *
     * @param monitorOnSessionLevel defines the level monitoring will be conducted on. If set to true, statistics
     *                              will be collected and exposed in MXBeans per session, endpoint and application.
     *                              If false, statistics will be collected and exposed in MXBeans only per endpoint
     *                              and application.
     */
    ApplicationMonitor(boolean monitorOnSessionLevel) {
        this.monitorOnSessionLevel = monitorOnSessionLevel;
    }

    @Override
    public void onApplicationInitialized(String applicationName) {
        this.applicationName = applicationName;

        MessageStatisticsMXBeanImpl textMessagesMXBean =
                new MessageStatisticsMXBeanImpl(sentTextMessageStatistics, receivedTextMessageStatistics);
        MessageStatisticsMXBeanImpl controlMessagesMXBean =
                new MessageStatisticsMXBeanImpl(sentControlMessageStatistics, receivedControlMessageStatistics);
        MessageStatisticsMXBeanImpl binaryMessagesMXBean =
                new MessageStatisticsMXBeanImpl(sentBinaryMessageStatistics, receivedBinaryMessageStatistics);

        MessageStatisticsAggregator sentTotalStatistics =
                new MessageStatisticsAggregator(sentTextMessageStatistics, sentBinaryMessageStatistics,
                                                sentControlMessageStatistics);
        MessageStatisticsAggregator receivedTotalStatistics =
                new MessageStatisticsAggregator(receivedTextMessageStatistics, receivedBinaryMessageStatistics,
                                                receivedControlMessageStatistics);
        applicationMXBean =
                new ApplicationMXBeanImpl(sentTotalStatistics, receivedTotalStatistics, getEndpoints(),
                                          getEndpointPaths(), getOpenSessionsCount(), getMaxOpenSessionsCount(),
                                          getErrorCounts(), textMessagesMXBean, binaryMessagesMXBean,
                                          controlMessagesMXBean);

        MBeanPublisher.registerApplicationMXBeans(applicationName, applicationMXBean, textMessagesMXBean,
                                                  binaryMessagesMXBean, controlMessagesMXBean);
    }

    @Override
    public void onApplicationDestroyed() {
        MBeanPublisher.unregisterApplicationMXBeans(applicationName);
    }

    @Override
    public EndpointEventListener onEndpointRegistered(String endpointPath, Class<?> endpointClass) {
        EndpointMonitor endpointJmx;
        if (monitorOnSessionLevel) {
            endpointJmx = new SessionAwareEndpointMonitor(this, applicationMXBean, applicationName, endpointPath,
                                                          endpointClass.getName());
        } else {
            endpointJmx = new SessionlessEndpointMonitor(this, applicationMXBean, applicationName, endpointPath,
                                                         endpointClass.getName());

        }
        endpoints.put(endpointPath, endpointJmx);
        return endpointJmx;
    }

    @Override
    public void onEndpointUnregistered(String endpointPath) {
        EndpointMonitor endpoint = endpoints.remove(endpointPath);
        endpoint.unregister();
    }

    /**
     * Get a {@link Callable} that will provide list of endpoint paths and endpoint
     * class names for currently registered endpoints.
     *
     * @return {@link Callable} returning list of endpoint paths and class names.
     */
    private Callable<List<EndpointClassNamePathPair>> getEndpoints() {
        return new Callable<List<EndpointClassNamePathPair>>() {
            @Override
            public List<EndpointClassNamePathPair> call() {
                List<EndpointClassNamePathPair> result = new ArrayList<EndpointClassNamePathPair>();
                for (EndpointMonitor endpoint : endpoints.values()) {
                    result.add(endpoint.getEndpointClassNamePathPair());
                }
                return result;
            }
        };
    }

    /**
     * Get a {@link Callable} that will provide set of endpoint paths for currently registered endpoints.
     *
     * @return {@link Callable} returning set of endpoint paths.
     */
    private Callable<List<String>> getEndpointPaths() {
        return new Callable<List<String>>() {
            @Override
            public List<String> call() {
                return new ArrayList<String>(endpoints.keySet());
            }
        };
    }

    /**
     * Get a {@link Callable} that will provide number of currently open sessions.
     *
     * @return {@link Callable} returning a current number of open sessions.
     */
    private Callable<Integer> getOpenSessionsCount() {
        return new Callable<Integer>() {
            @Override
            public Integer call() {
                return openSessionsCount.get();
            }
        };
    }

    /**
     * Get a {@link Callable} that will provide a maximal number of open sessions since the start of monitoring.
     *
     * @return {@link Callable} returning a maximal number of open sessions since the start of monitoring.
     */
    private Callable<Integer> getMaxOpenSessionsCount() {
        return new Callable<Integer>() {
            @Override
            public Integer call() {
                return maxOpenSessionCount;
            }
        };
    }

    void onSessionOpened() {
        openSessionsCount.incrementAndGet();
        if (openSessionsCount.get() > maxOpenSessionCount) {
            synchronized (maxOpenSessionsCountLock) {
                if (openSessionsCount.get() > maxOpenSessionCount) {
                    maxOpenSessionCount = openSessionsCount.get();
                }
            }
        }
    }

    void onSessionClosed() {
        openSessionsCount.decrementAndGet();
    }

    @Override
    public void onTextMessageSent(long length) {
        sentTextMessageStatistics.onMessage(length);
    }

    @Override
    public void onBinaryMessageSent(long length) {
        sentBinaryMessageStatistics.onMessage(length);
    }

    @Override
    public void onControlMessageSent(long length) {
        sentControlMessageStatistics.onMessage(length);
    }

    @Override
    public void onTextMessageReceived(long length) {
        receivedTextMessageStatistics.onMessage(length);
    }

    @Override
    public void onBinaryMessageReceived(long length) {
        receivedBinaryMessageStatistics.onMessage(length);
    }

    @Override
    public void onControlMessageReceived(long length) {
        receivedControlMessageStatistics.onMessage(length);
    }
}
