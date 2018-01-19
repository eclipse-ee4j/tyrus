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
import java.util.List;

/**
 * @author Petr Janouch
 */
class SessionMXBeanImpl extends BaseMXBeanImpl implements SessionMXBean, Serializable {

    private static final long serialVersionUID = -5996261566352502699L;

    private final String sessionId;

    SessionMXBeanImpl(MessageStatisticsSource sentMessageStatistics, MessageStatisticsSource receivedMessageStatistics,
                      Callable<List<ErrorCount>> errorCounts, MessageStatisticsMXBean textMessageStatisticsMXBean,
                      MessageStatisticsMXBean binaryMessageStatisticsMXBean,
                      MessageStatisticsMXBean controlMessageStatisticsMXBean, String sessionId) {
        super(sentMessageStatistics, receivedMessageStatistics, errorCounts, textMessageStatisticsMXBean,
              binaryMessageStatisticsMXBean, controlMessageStatisticsMXBean);
        this.sessionId = sessionId;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }
}
