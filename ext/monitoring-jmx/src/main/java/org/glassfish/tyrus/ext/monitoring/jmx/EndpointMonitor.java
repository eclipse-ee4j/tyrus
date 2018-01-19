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

import org.glassfish.tyrus.core.monitoring.EndpointEventListener;

/**
 * Listens to endpoint events and collects endpoint-level statistics.
 * <p/>
 * Creates and registers {@link org.glassfish.tyrus.ext.monitoring.jmx.EndpointMXBean} MXBean that exposes these
 * statistics.
 *
 * @author Petr Janouch
 * @see EndpointEventListener
 */
abstract class EndpointMonitor extends BaseMonitor implements EndpointEventListener, MessageListener {

    final EndpointClassNamePathPair endpointClassNamePathPair;
    final String applicationName;
    final Object maxOpenSessionsCountLock = new Object();
    final ApplicationMonitor applicationMonitor;
    final EndpointMXBeanImpl endpointMXBean;

    private final ApplicationMXBeanImpl applicationMXBean;

    private final ConcurrentMessageStatistics sentTextMessageStatistics = new ConcurrentMessageStatistics();
    private final ConcurrentMessageStatistics sentBinaryMessageStatistics = new ConcurrentMessageStatistics();
    private final ConcurrentMessageStatistics sentControlMessageStatistics = new ConcurrentMessageStatistics();

    private final ConcurrentMessageStatistics receivedTextMessageStatistics = new ConcurrentMessageStatistics();
    private final ConcurrentMessageStatistics receivedBinaryMessageStatistics = new ConcurrentMessageStatistics();
    private final ConcurrentMessageStatistics receivedControlMessageStatistics = new ConcurrentMessageStatistics();

    protected volatile int maxOpenSessionsCount = 0;

    EndpointMonitor(ApplicationMonitor applicationMonitor, ApplicationMXBeanImpl applicationMXBean,
                    String applicationName, String endpointPath, String endpointClassName) {
        this.applicationName = applicationName;
        this.endpointClassNamePathPair = new EndpointClassNamePathPair(endpointPath, endpointClassName);
        this.applicationMonitor = applicationMonitor;
        this.applicationMXBean = applicationMXBean;

        MessageStatisticsMXBeanImpl textMessagesMXBean =
                new MessageStatisticsMXBeanImpl(sentTextMessageStatistics, receivedTextMessageStatistics);
        MessageStatisticsMXBeanImpl binaryMessagesMXBean =
                new MessageStatisticsMXBeanImpl(sentBinaryMessageStatistics, receivedBinaryMessageStatistics);
        MessageStatisticsMXBeanImpl controlMessagesMXBean =
                new MessageStatisticsMXBeanImpl(sentControlMessageStatistics, receivedControlMessageStatistics);

        MessageStatisticsAggregator sentTotalStatistics =
                new MessageStatisticsAggregator(sentTextMessageStatistics, sentBinaryMessageStatistics,
                                                sentControlMessageStatistics);
        MessageStatisticsAggregator receivedTotalStatistics =
                new MessageStatisticsAggregator(receivedTextMessageStatistics, receivedBinaryMessageStatistics,
                                                receivedControlMessageStatistics);
        endpointMXBean =
                new EndpointMXBeanImpl(sentTotalStatistics, receivedTotalStatistics, endpointPath, endpointClassName,
                                       getOpenSessionsCount(), getMaxOpenSessionsCount(), getErrorCounts(),
                                       textMessagesMXBean, binaryMessagesMXBean, controlMessagesMXBean);

        MBeanPublisher.registerEndpointMXBeans(applicationName, endpointPath, endpointMXBean, textMessagesMXBean,
                                               binaryMessagesMXBean, controlMessagesMXBean);
        applicationMXBean.putEndpointMXBean(endpointPath, endpointMXBean);
    }


    void unregister() {
        MBeanPublisher.unregisterEndpointMXBeans(applicationName, endpointClassNamePathPair.getEndpointPath());
        applicationMXBean.removeEndpointMXBean(endpointClassNamePathPair.getEndpointPath());
    }

    EndpointClassNamePathPair getEndpointClassNamePathPair() {
        return endpointClassNamePathPair;
    }

    /**
     * Get a {@link Callable} that will provide current number of open sessions for this endpoint.
     *
     * @return {@link Callable} returning number of currently open sessions.
     */
    protected abstract Callable<Integer> getOpenSessionsCount();

    /**
     * Get a {@link Callable} that will provide maximal number of open sessions for this endpoint since the start of
     * monitoring.
     *
     * @return {@link Callable} returning a maximal number of open sessions since the start of monitoring.
     */
    private Callable<Integer> getMaxOpenSessionsCount() {
        return new Callable<Integer>() {
            @Override
            public Integer call() {
                return maxOpenSessionsCount;
            }
        };
    }

    @Override
    public void onTextMessageSent(long length) {
        sentTextMessageStatistics.onMessage(length);
        applicationMonitor.onTextMessageSent(length);
    }

    @Override
    public void onBinaryMessageSent(long length) {
        sentBinaryMessageStatistics.onMessage(length);
        applicationMonitor.onBinaryMessageSent(length);
    }

    @Override
    public void onControlMessageSent(long length) {
        sentControlMessageStatistics.onMessage(length);
        applicationMonitor.onControlMessageSent(length);
    }

    @Override
    public void onTextMessageReceived(long length) {
        receivedTextMessageStatistics.onMessage(length);
        applicationMonitor.onTextMessageReceived(length);
    }

    @Override
    public void onBinaryMessageReceived(long length) {
        receivedBinaryMessageStatistics.onMessage(length);
        applicationMonitor.onBinaryMessageReceived(length);
    }

    @Override
    public void onControlMessageReceived(long length) {
        receivedControlMessageStatistics.onMessage(length);
        applicationMonitor.onControlMessageReceived(length);
    }
}
