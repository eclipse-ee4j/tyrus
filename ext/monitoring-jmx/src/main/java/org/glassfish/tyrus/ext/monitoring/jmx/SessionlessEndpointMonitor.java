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

import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.tyrus.core.monitoring.MessageEventListener;

/**
 * This {@link org.glassfish.tyrus.ext.monitoring.jmx.EndpointMonitor} implementation represents the lowest level of
 * monitoring hierarchy and does not create and hold {@link org.glassfish.tyrus.ext.monitoring.jmx.SessionMonitor}
 * for opened sessions. It is used when monitoring on session level is turned off.
 *
 * @author Petr Janouch
 */
class SessionlessEndpointMonitor extends EndpointMonitor {

    private final AtomicInteger openSessionsCount = new AtomicInteger();

    SessionlessEndpointMonitor(ApplicationMonitor applicationJmx, ApplicationMXBeanImpl applicationMXBean,
                               String applicationName, String endpointPath, String endpointClassName) {
        super(applicationJmx, applicationMXBean, applicationName, endpointPath, endpointClassName);
    }

    @Override
    protected Callable<Integer> getOpenSessionsCount() {
        return new Callable<Integer>() {
            @Override
            public Integer call() {
                return openSessionsCount.get();
            }
        };
    }

    @Override
    public MessageEventListener onSessionOpened(String sessionId) {
        applicationMonitor.onSessionOpened();
        openSessionsCount.incrementAndGet();

        if (openSessionsCount.get() > maxOpenSessionsCount) {
            synchronized (maxOpenSessionsCountLock) {
                if (openSessionsCount.get() > maxOpenSessionsCount) {
                    maxOpenSessionsCount = openSessionsCount.get();
                }
            }
        }

        return new MessageEventListenerImpl(this);
    }

    @Override
    public void onSessionClosed(String sessionId) {
        applicationMonitor.onSessionClosed();
        openSessionsCount.decrementAndGet();
    }

    @Override
    public void onError(String sessionId, Throwable t) {
        applicationMonitor.onError(t);
    }
}
