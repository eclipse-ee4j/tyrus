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
 * MXBean used for accessing monitored endpoint properties - endpoint path and class name, number of currently open
 * sessions, maximal number of open sessions since the start of monitoring, and message statistics.
 *
 * @author Petr Janouch
 * @see MessageStatisticsMXBean
 */
@Beta
public interface EndpointMXBean extends BaseMXBean {

    /**
     * Get the path the endpoint is registered on.
     *
     * @return path of the endpoint.
     */
    public String getEndpointPath();

    /**
     * Get the class name of the endpoint.
     *
     * @return the class name of the endpoint.
     */
    public String getEndpointClassName();

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
     * Get list of MXBeans representing currently open sessions. Return an empty list if monitoring is conducted only on
     * endpoint level.
     *
     * @return list of MXBeans representing currently open sessions.
     */
    public List<SessionMXBean> getSessionMXBeans();
}
