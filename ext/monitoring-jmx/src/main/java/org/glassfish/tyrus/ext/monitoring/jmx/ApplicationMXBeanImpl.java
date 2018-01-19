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
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Petr Janouch
 */
class ApplicationMXBeanImpl extends BaseMXBeanImpl implements ApplicationMXBean, Serializable {

    private static final long serialVersionUID = 5417841460238333390L;

    private final Callable<List<EndpointClassNamePathPair>> endpoints;
    private final Callable<List<String>> endpointPaths;
    private final ConcurrentHashMap<String, EndpointMXBean> endpointMXBeans =
            new ConcurrentHashMap<String, EndpointMXBean>();
    private final Callable<Integer> openSessionsCount;
    private final Callable<Integer> maxOpenSessionsCount;

    public ApplicationMXBeanImpl(MessageStatisticsSource sentMessageStatistics,
                                 MessageStatisticsSource receivedMessageStatistics,
                                 Callable<List<EndpointClassNamePathPair>> endpoints,
                                 Callable<List<String>> endpointPaths, Callable<Integer> openSessionsCount,
                                 Callable<Integer> maxOpenSessionsCount, Callable<List<ErrorCount>> errorCounts,
                                 MessageStatisticsMXBean textMessageStatisticsMXBean,
                                 MessageStatisticsMXBean binaryMessageStatisticsMXBean,
                                 MessageStatisticsMXBean controlMessageStatisticsMXBean) {
        super(sentMessageStatistics, receivedMessageStatistics, errorCounts, textMessageStatisticsMXBean,
              binaryMessageStatisticsMXBean, controlMessageStatisticsMXBean);
        this.endpoints = endpoints;
        this.endpointPaths = endpointPaths;
        this.openSessionsCount = openSessionsCount;
        this.maxOpenSessionsCount = maxOpenSessionsCount;
    }

    @Override
    public List<EndpointClassNamePathPair> getEndpoints() {
        return endpoints.call();
    }

    @Override
    public List<String> getEndpointPaths() {
        return endpointPaths.call();
    }

    @Override
    public List<EndpointMXBean> getEndpointMXBeans() {
        return new ArrayList<EndpointMXBean>(endpointMXBeans.values());
    }

    @Override
    public int getOpenSessionsCount() {
        return openSessionsCount.call();
    }

    @Override
    public int getMaximalOpenSessionsCount() {
        return maxOpenSessionsCount.call();
    }

    void putEndpointMXBean(String endpointPath, EndpointMXBean endpointMXBean) {
        endpointMXBeans.put(endpointPath, endpointMXBean);
    }

    void removeEndpointMXBean(String path) {
        endpointMXBeans.remove(path);
    }
}
