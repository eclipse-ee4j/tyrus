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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Petr Janouch
 */
class EndpointMXBeanImpl extends BaseMXBeanImpl implements EndpointMXBean, Serializable {

    private static final long serialVersionUID = 1362028955470529660L;

    private final String endpointPath;
    private final String endpointClassName;
    private final Callable<Integer> openSessionsCount;
    private final Callable<Integer> maxOpenSessionsCount;
    private final Map<String, SessionMXBean> sessionMXBeans = new ConcurrentHashMap<String, SessionMXBean>();

    public EndpointMXBeanImpl(
            MessageStatisticsSource sentMessageStatistics, MessageStatisticsSource receivedMessageStatistics,
            String endpointPath, String endpointClassName, Callable<Integer> openSessionsCount,
            Callable<Integer> maxOpenSessionsCount, Callable<List<ErrorCount>> errorCounts,
            MessageStatisticsMXBean textMessageStatisticsMXBean, MessageStatisticsMXBean binaryMessageStatisticsMXBean,
            MessageStatisticsMXBean controlMessageStatisticsMXBean) {
        super(sentMessageStatistics, receivedMessageStatistics, errorCounts, textMessageStatisticsMXBean,
              binaryMessageStatisticsMXBean, controlMessageStatisticsMXBean);
        this.endpointPath = endpointPath;
        this.endpointClassName = endpointClassName;
        this.openSessionsCount = openSessionsCount;
        this.maxOpenSessionsCount = maxOpenSessionsCount;
    }

    @Override
    public String getEndpointPath() {
        return endpointPath;
    }

    @Override
    public String getEndpointClassName() {
        return endpointClassName;
    }

    @Override
    public int getOpenSessionsCount() {
        return openSessionsCount.call();
    }

    @Override
    public int getMaximalOpenSessionsCount() {
        return maxOpenSessionsCount.call();
    }

    @Override
    public List<SessionMXBean> getSessionMXBeans() {
        return new ArrayList<SessionMXBean>(sessionMXBeans.values());
    }

    void putSessionMXBean(String sessionId, SessionMXBean sessionMXBean) {
        sessionMXBeans.put(sessionId, sessionMXBean);
    }

    void removeSessionMXBean(String sessionId) {
        sessionMXBeans.remove(sessionId);
    }
}
