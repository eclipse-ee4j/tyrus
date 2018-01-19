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

import java.io.Serializable;

/**
 * @author Petr Janouch
 */
class MessageStatisticsMXBeanImpl implements MessageStatisticsMXBean, Serializable {

    private static final long serialVersionUID = -2156995729320425363L;

    private final MessageStatisticsSource sentMessageStatistics;
    private final MessageStatisticsSource receivedMessageStatistics;
    private final long monitoringStart;

    public MessageStatisticsMXBeanImpl(MessageStatisticsSource sentMessageStatistics,
                                       MessageStatisticsSource receivedMessageStatistics) {
        this.sentMessageStatistics = sentMessageStatistics;
        this.receivedMessageStatistics = receivedMessageStatistics;
        this.monitoringStart = System.currentTimeMillis();
    }

    @Override
    public long getSentMessagesCount() {
        return sentMessageStatistics.getMessagesCount();
    }

    @Override
    public long getMinimalSentMessageSize() {
        return sentMessageStatistics.getMinMessageSize();
    }

    @Override
    public long getMaximalSentMessageSize() {
        return sentMessageStatistics.getMaxMessageSize();
    }

    @Override
    public long getAverageSentMessageSize() {
        if (sentMessageStatistics.getMessagesCount() == 0) {
            return 0;
        }
        return sentMessageStatistics.getMessagesSize() / sentMessageStatistics.getMessagesCount();
    }

    @Override
    public long getSentMessagesCountPerSecond() {
        long time = getTimeSinceBeginningInSeconds();
        if (time == 0) {
            return 0;
        }
        return getSentMessagesCount() / time;
    }

    @Override
    public long getReceivedMessagesCount() {
        return receivedMessageStatistics.getMessagesCount();
    }

    @Override
    public long getMinimalReceivedMessageSize() {
        return receivedMessageStatistics.getMinMessageSize();
    }

    @Override
    public long getMaximalReceivedMessageSize() {
        return receivedMessageStatistics.getMaxMessageSize();
    }

    @Override
    public long getAverageReceivedMessageSize() {
        if (receivedMessageStatistics.getMessagesCount() == 0) {
            return 0;
        }
        return receivedMessageStatistics.getMessagesSize() / receivedMessageStatistics.getMessagesCount();
    }

    @Override
    public long getReceivedMessagesCountPerSecond() {
        long time = getTimeSinceBeginningInSeconds();
        if (time == 0) {
            return 0;
        }
        return getReceivedMessagesCount() / time;
    }

    private long getTimeSinceBeginningInSeconds() {
        long time = System.currentTimeMillis() - monitoringStart;
        return time / 1000;
    }
}
