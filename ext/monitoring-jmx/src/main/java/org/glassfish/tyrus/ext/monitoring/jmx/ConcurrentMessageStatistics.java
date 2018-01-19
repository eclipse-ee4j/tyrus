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
import java.util.concurrent.atomic.LongAdder;

/**
 * An implementation of {@link org.glassfish.tyrus.ext.monitoring.jmx.MessageStatisticsSource} that allows concurrent
 * updates by many threads.
 *
 * @author Petr Janouch
 */
class ConcurrentMessageStatistics implements MessageStatisticsSource, Serializable {

    private static final long serialVersionUID = -54010795753428605L;

    private final LongAdder messagesCount = new LongAdder();
    private final LongAdder messagesSize = new LongAdder();
    private final Object minimalMessageSizeLock = new Object();
    private final Object maximalMessageSizeLock = new Object();

    private volatile long minimalMessageSize = Long.MAX_VALUE;
    private volatile long maximalMessageSize = 0;

    void onMessage(long size) {
        messagesCount.increment();
        messagesSize.add(size);
        if (minimalMessageSize > size) {
            synchronized (minimalMessageSizeLock) {
                if (minimalMessageSize > size) {
                    minimalMessageSize = size;
                }
            }
        }
        if (maximalMessageSize < size) {
            synchronized (maximalMessageSizeLock) {
                if (maximalMessageSize < size) {
                    maximalMessageSize = size;
                }
            }
        }
    }

    @Override
    public long getMessagesCount() {
        return messagesCount.longValue();
    }

    @Override
    public long getMessagesSize() {
        return messagesSize.longValue();
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
