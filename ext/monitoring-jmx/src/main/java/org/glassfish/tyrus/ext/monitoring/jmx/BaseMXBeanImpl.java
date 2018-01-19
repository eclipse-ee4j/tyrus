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
class BaseMXBeanImpl extends MessageStatisticsMXBeanImpl implements BaseMXBean, Serializable {

    private static final long serialVersionUID = -7676554745289677749L;

    private final Callable<List<ErrorCount>> errorCounts;
    private final MessageStatisticsMXBean textMessageStatisticsMXBean;
    private final MessageStatisticsMXBean binaryMessageStatisticsMXBean;
    private final MessageStatisticsMXBean controlMessageStatisticsMXBean;

    BaseMXBeanImpl(MessageStatisticsSource sentMessageStatistics, MessageStatisticsSource receivedMessageStatistics,
                   Callable<List<ErrorCount>> errorCounts, MessageStatisticsMXBean textMessageStatisticsMXBean,
                   MessageStatisticsMXBean binaryMessageStatisticsMXBean,
                   MessageStatisticsMXBean controlMessageStatisticsMXBean) {
        super(sentMessageStatistics, receivedMessageStatistics);
        this.errorCounts = errorCounts;
        this.textMessageStatisticsMXBean = textMessageStatisticsMXBean;
        this.binaryMessageStatisticsMXBean = binaryMessageStatisticsMXBean;
        this.controlMessageStatisticsMXBean = controlMessageStatisticsMXBean;
    }

    @Override
    public List<ErrorCount> getErrorCounts() {
        return errorCounts.call();
    }

    @Override
    public MessageStatisticsMXBean getTextMessageStatisticsMXBean() {
        return textMessageStatisticsMXBean;
    }

    @Override
    public MessageStatisticsMXBean getBinaryMessageStatisticsMXBean() {
        return binaryMessageStatisticsMXBean;
    }

    @Override
    public MessageStatisticsMXBean getControlMessageStatisticsMXBean() {
        return controlMessageStatisticsMXBean;
    }
}
