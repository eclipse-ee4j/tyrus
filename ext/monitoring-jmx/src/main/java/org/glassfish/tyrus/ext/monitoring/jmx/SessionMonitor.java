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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Listens to message events and collects session-level statistics for sent and received messages. Creates and registers
 * {@link org.glassfish.tyrus.ext.monitoring.jmx.MessageStatisticsMXBean} MXBeans for text, binary control and all
 * messages which expose these statistics.
 *
 * @author Petr Janouch
 * @see org.glassfish.tyrus.core.monitoring.MessageEventListener
 */
class SessionMonitor extends BaseMonitor implements MessageListener {

    private final MessageStatistics sentTextMessageStatistics = new MessageStatistics();
    private final MessageStatistics sentBinaryMessageStatistics = new MessageStatistics();
    private final MessageStatistics sentControlMessageStatistics = new MessageStatistics();

    private final MessageStatistics receivedTextMessageStatistics = new MessageStatistics();
    private final MessageStatistics receivedBinaryMessageStatistics = new MessageStatistics();
    private final MessageStatistics receivedControlMessageStatistics = new MessageStatistics();

    private final String applicationName;
    private final String endpointPath;
    private final String sessionId;
    private final MessageListener messageListener;
    private final EndpointMXBeanImpl endpointMXBean;

    SessionMonitor(String applicationName, String endpointPath, String sessionId, MessageListener messageListener,
                   EndpointMXBeanImpl endpointMXBean) {
        this.applicationName = applicationName;
        this.endpointPath = endpointPath;
        this.sessionId = sessionId;
        this.messageListener = messageListener;
        this.endpointMXBean = endpointMXBean;

        MessageStatisticsMXBean textMessagesMXBean =
                new MessageStatisticsMXBeanImpl(sentTextMessageStatistics, receivedTextMessageStatistics);
        MessageStatisticsMXBean binaryMessagesMXBean =
                new MessageStatisticsMXBeanImpl(sentBinaryMessageStatistics, receivedBinaryMessageStatistics);
        MessageStatisticsMXBean controlMessagesMXBean =
                new MessageStatisticsMXBeanImpl(sentControlMessageStatistics, receivedControlMessageStatistics);

        MessageStatisticsAggregator sentMessagesTotal =
                new MessageStatisticsAggregator(sentTextMessageStatistics, sentBinaryMessageStatistics,
                                                sentControlMessageStatistics);
        MessageStatisticsAggregator receivedMessagesTotal =
                new MessageStatisticsAggregator(receivedTextMessageStatistics, receivedBinaryMessageStatistics,
                                                receivedControlMessageStatistics);
        SessionMXBeanImpl sessionMXBean =
                new SessionMXBeanImpl(sentMessagesTotal, receivedMessagesTotal, getErrorCounts(), textMessagesMXBean,
                                      binaryMessagesMXBean, controlMessagesMXBean, sessionId);

        endpointMXBean.putSessionMXBean(sessionId, sessionMXBean);
        MBeanPublisher
                .registerSessionMXBeans(applicationName, endpointPath, sessionId, sessionMXBean, textMessagesMXBean,
                                        binaryMessagesMXBean, controlMessagesMXBean);
    }

    void unregister() {
        MBeanPublisher.unregisterSessionMXBeans(applicationName, endpointPath, sessionId);
        endpointMXBean.removeSessionMXBean(sessionId);
    }

    @Override
    public void onTextMessageSent(long length) {
        sentTextMessageStatistics.onMessage(length);
        messageListener.onTextMessageSent(length);
    }

    @Override
    public void onBinaryMessageSent(long length) {
        sentBinaryMessageStatistics.onMessage(length);
        messageListener.onBinaryMessageSent(length);
    }

    @Override
    public void onControlMessageSent(long length) {
        sentControlMessageStatistics.onMessage(length);
        messageListener.onControlMessageSent(length);
    }

    @Override
    public void onTextMessageReceived(long length) {
        receivedTextMessageStatistics.onMessage(length);
        messageListener.onTextMessageReceived(length);
    }

    @Override
    public void onBinaryMessageReceived(long length) {
        receivedBinaryMessageStatistics.onMessage(length);
        messageListener.onBinaryMessageReceived(length);
    }

    @Override
    public void onControlMessageReceived(long length) {
        receivedControlMessageStatistics.onMessage(length);
        messageListener.onControlMessageReceived(length);
    }

    private static class MessageStatistics implements MessageStatisticsSource {

        /*
        volatile is enough in this case, because only one thread can sent or receive a message in a session
         */
        private final AtomicLong messagesCount = new AtomicLong(0);
        private volatile long messagesSize = 0;
        private volatile long minimalMessageSize = Long.MAX_VALUE;
        private volatile long maximalMessageSize = 0;

        void onMessage(long size) {
            messagesCount.incrementAndGet();
            messagesSize += size;
            if (minimalMessageSize > size) {
                minimalMessageSize = size;
            }
            if (maximalMessageSize < size) {
                maximalMessageSize = size;
            }
        }

        @Override
        public long getMessagesCount() {
            return messagesCount.get();
        }

        @Override
        public long getMessagesSize() {
            return messagesSize;
        }

        @Override
        public long getMinMessageSize() {
            if (minimalMessageSize == Long.MAX_VALUE) {
                return 0;
            }
            return minimalMessageSize;
        }

        @Override
        public long getMaxMessageSize() {
            return maximalMessageSize;
        }
    }
}
