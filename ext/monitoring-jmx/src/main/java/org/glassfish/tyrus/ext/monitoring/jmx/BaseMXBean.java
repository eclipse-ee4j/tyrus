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
 * A parent MXBean for {@link org.glassfish.tyrus.ext.monitoring.jmx.ApplicationMXBean}, {@link
 * org.glassfish.tyrus.ext.monitoring.jmx.EndpointMXBean} and {@link org.glassfish.tyrus.ext.monitoring.jmx
 * .SessionMXBean}.
 *
 * @author Petr Janouch
 */
@Beta
public interface BaseMXBean extends MessageStatisticsMXBean {

    /**
     * Get list of Throwable class name - count pairs, which represent errors and number of times they have occurred.
     *
     * @return list of Throwable class name - count pairs, which represent errors and number of times they have
     * occurred.
     */
    public List<ErrorCount> getErrorCounts();

    /**
     * Get an MXBean containing statistics about text messages.
     *
     * @return an MXBean containing statistics about text messages.
     */
    public MessageStatisticsMXBean getTextMessageStatisticsMXBean();

    /**
     * Get an MXBean containing statistics about binary messages.
     *
     * @return an MXBean containing statistics about binary messages.
     */
    public MessageStatisticsMXBean getBinaryMessageStatisticsMXBean();

    /**
     * Get an MXBean containing statistics about control messages.
     *
     * @return an MXBean containing statistics about control messages.
     */
    public MessageStatisticsMXBean getControlMessageStatisticsMXBean();
}
