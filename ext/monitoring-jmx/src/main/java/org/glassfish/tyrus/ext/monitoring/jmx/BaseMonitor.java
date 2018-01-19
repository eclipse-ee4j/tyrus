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
import java.util.concurrent.atomic.AtomicLong;

/**
 * A class containing logic common for {@link org.glassfish.tyrus.ext.monitoring.jmx.ApplicationMonitor},
 * {@link org.glassfish.tyrus.ext.monitoring.jmx.EndpointMonitor} and {@link org.glassfish.tyrus.ext.monitoring.jmx
 * .SessionMonitor}.
 *
 * @author Petr Janouch
 */
class BaseMonitor {

    private final Map<String, AtomicLong> errorStatistics = new ConcurrentHashMap<String, AtomicLong>();
    private final Object errorStatisticsLock = new Object();

    Callable<List<ErrorCount>> getErrorCounts() {
        return new Callable<List<ErrorCount>>() {
            @Override
            public List<ErrorCount> call() {
                final List<ErrorCount> errorCounts = new ArrayList<ErrorCount>();
                for (Map.Entry<String, AtomicLong> errorCount : errorStatistics.entrySet()) {
                    errorCounts.add(new ErrorCount(errorCount.getKey(), errorCount.getValue().get()));
                }
                return errorCounts;
            }
        };
    }

    void onError(Throwable t) {
        String throwableClassName;
        if (t.getCause() == null) {
            throwableClassName = t.getClass().getName();
        } else {
            throwableClassName = t.getCause().getClass().getName();
        }

        if (!errorStatistics.containsKey(throwableClassName)) {
            synchronized (errorStatisticsLock) {
                if (!errorStatistics.containsKey(throwableClassName)) {
                    errorStatistics.put(throwableClassName, new AtomicLong());
                }
            }
        }

        AtomicLong throwableCounter = errorStatistics.get(throwableClassName);
        throwableCounter.incrementAndGet();
    }
}
