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

import java.util.List;

import org.glassfish.tyrus.core.Beta;

/**
 * MXBean used for accessing monitored application properties - registered endpoints, number of currently open sessions,
 * maximal number of open sessions since the start of the monitoring and message statistics.
 *
 * @author Petr Janouch
 */
@Beta
public interface ApplicationMXBean extends BaseMXBean {
    /**
     * Get endpoint paths and class names for currently registered endpoints.
     *
     * @return endpoint paths and class names for currently registered endpoints.
     */
    public List<EndpointClassNamePathPair> getEndpoints();

    /**
     * Get endpoint paths for currently registered endpoints.
     *
     * @return paths of registered endpoints.
     */
    public List<String> getEndpointPaths();

    /**
     * Get the number of sessions currently open on the endpoint.
     *
     * @return the number of sessions currently open on the endpoint.
     */
    public int getOpenSessionsCount();

    /**
     * Get the maximal number of open sessions on the endpoint since the start of monitoring.
     *
     * @return the maximal number of open sessions on the endpoint since the start of monitoring.
     */
    public int getMaximalOpenSessionsCount();

    /**
     * Get list of MXBeans representing registered endpoints.
     *
     * @return list of MXBeans representing registered endpoints.
     */
    public List<EndpointMXBean> getEndpointMXBeans();
}
