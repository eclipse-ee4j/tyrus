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
 * Application events listener and statistics collector.
 * The statistics are collected by aggregating statistics from application endpoints and endpoint sessions.
 * The only difference form {@link org.glassfish.tyrus.ext.monitoring.jmx.SessionlessApplicationMonitor} is that
 * {@link org.glassfish.tyrus.ext.monitoring.jmx.SessionlessApplicationMonitor} does not collect statistics on session
 * level.
 * <p>
 * It also creates and registers MXBeans that can be used to access these collected statistics. The created MXBeans
 * will allow accessing monitored properties and statistics on application, endpoint and session level.
 * <p>
 * For monitoring in Grizzly server an instance should be passed to the server in server properties.
 * <pre>
 *     serverProperties.put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, new SessionAwareApplicationMonitor());
 * </pre>
 * For use in servlet container the class name should be passed as a context parameter in web.xml.
 * <pre>{@code
 *     <context-param>
 *         <param-name>org.glassfish.tyrus.core.monitoring.ApplicationEventListener</param-name>
 *         <param-value>org.glassfish.tyrus.ext.monitoring.jmx.SessionAwareApplicationMonitor</param-value>
 *     </context-param>}</pre>
 *
 * @author Petr Janouch
 * @see org.glassfish.tyrus.core.monitoring.ApplicationEventListener
 */
public final class SessionAwareApplicationMonitor extends ApplicationMonitor {

    /**
     * Constructor.
     */
    public SessionAwareApplicationMonitor() {
        super(true);
    }
}
