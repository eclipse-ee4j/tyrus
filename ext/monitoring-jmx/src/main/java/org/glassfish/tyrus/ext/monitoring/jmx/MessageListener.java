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

/**
 * Listener of message-level events like {@link org.glassfish.tyrus.core.monitoring.MessageEventListener}, with a
 * difference that it distinguishes between message types.
 *
 * @author Petr Janouch
 */
interface MessageListener {

    void onTextMessageSent(long length);

    void onBinaryMessageSent(long length);

    void onControlMessageSent(long length);

    void onTextMessageReceived(long length);

    void onBinaryMessageReceived(long length);

    void onControlMessageReceived(long length);
}
