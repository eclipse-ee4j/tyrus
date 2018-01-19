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
 * Message statistics aggregator.
 *
 * @author Petr Janouch
 */
class MessageStatisticsAggregator implements MessageStatisticsSource, Serializable {

    private static final long serialVersionUID = -8122720652279530246L;

    private final MessageStatisticsSource[] messageStatisticsSources;

    MessageStatisticsAggregator(MessageStatisticsSource... messageStatisticsSources) {
        this.messageStatisticsSources = messageStatisticsSources;
    }

    @Override
    public long getMessagesCount() {
        long result = 0;
        for (MessageStatisticsSource statisticsSource : messageStatisticsSources) {
            result += statisticsSource.getMessagesCount();
        }
        return result;
    }

    @Override
    public long getMessagesSize() {
        long result = 0;
        for (MessageStatisticsSource statisticsSource : messageStatisticsSources) {
            result += statisticsSource.getMessagesSize();
        }
        return result;
    }

    @Override
    public long getMinMessageSize() {
        long result = Long.MAX_VALUE;
        for (MessageStatisticsSource statisticsSource : messageStatisticsSources) {
            result = Math.min(result, statisticsSource.getMinMessageSize());
        }
        return result;
    }

    @Override
    public long getMaxMessageSize() {
        long result = 0;
        for (MessageStatisticsSource statisticsSource : messageStatisticsSources) {
            result = Math.max(result, statisticsSource.getMaxMessageSize());
        }
        return result;
    }
}
