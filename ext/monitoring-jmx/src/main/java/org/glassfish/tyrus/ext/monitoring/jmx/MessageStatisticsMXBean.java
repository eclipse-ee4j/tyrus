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

import org.glassfish.tyrus.core.Beta;

/**
 * MXBean used for exposing message-level statistics.
 *
 * @author Petr Janouch
 */
@Beta
public interface MessageStatisticsMXBean extends Serializable {

    /**
     * Get the total number of messages sent since the start of monitoring.
     *
     * @return the total number of messages sent since the start of monitoring.
     */
    public long getSentMessagesCount();

    /**
     * Get the size of the smallest message sent since the start of monitoring.
     *
     * @return the size of the smallest message sent since the start of monitoring.
     */
    public long getMinimalSentMessageSize();

    /**
     * Get the size of the largest message sent since the start of monitoring.
     *
     * @return the size of the largest message sent since the start of monitoring.
     */
    public long getMaximalSentMessageSize();

    /**
     * Get the average size of all the messages sent since the start of monitoring.
     *
     * @return the average size of all the message sent since the start of monitoring.
     */
    public long getAverageSentMessageSize();

    /**
     * Get the average number of sent messages per second.
     *
     * @return the average number of sent messages per second.
     */
    public long getSentMessagesCountPerSecond();

    /**
     * Get the total number of messages received since the start of monitoring.
     *
     * @return the total number of messages received since the start of monitoring.
     */
    public long getReceivedMessagesCount();

    /**
     * Get the size of the smallest message received since the start of monitoring.
     *
     * @return the size of the smallest message received since the start of monitoring.
     */
    public long getMinimalReceivedMessageSize();

    /**
     * Get the size of the largest message received since the start of monitoring.
     *
     * @return the size of the largest message received since the start of monitoring.
     */
    public long getMaximalReceivedMessageSize();

    /**
     * Get the average size of all the messages received since the start of monitoring.
     *
     * @return the average size of all the message received since the start of monitoring.
     */
    public long getAverageReceivedMessageSize();

    /**
     * Get the average number of received messages per second.
     *
     * @return the average number of received messages per second.
     */
    public long getReceivedMessagesCountPerSecond();
}
