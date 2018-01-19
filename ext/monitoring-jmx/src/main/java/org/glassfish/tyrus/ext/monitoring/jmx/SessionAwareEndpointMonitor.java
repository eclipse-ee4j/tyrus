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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.glassfish.tyrus.core.monitoring.MessageEventListener;

/**
 * This {@link org.glassfish.tyrus.ext.monitoring.jmx.EndpointMonitor} implementation creates and holds
 * {@link org.glassfish.tyrus.ext.monitoring.jmx.SessionMonitor}, which collects message statistics for open sessions.
 *
 * @author Petr Janouch
 */
class SessionAwareEndpointMonitor extends EndpointMonitor {

    private final Map<String, SessionMonitor> sessions = new ConcurrentHashMap<String, SessionMonitor>();

    SessionAwareEndpointMonitor(ApplicationMonitor applicationJmx, ApplicationMXBeanImpl applicationMXBean,
                                String applicationName, String endpointPath, String endpointClassName) {
        super(applicationJmx, applicationMXBean, applicationName, endpointPath, endpointClassName);
    }

    @Override
    public MessageEventListener onSessionOpened(String sessionId) {
        SessionMonitor sessionMonitor =
                new SessionMonitor(applicationName, endpointClassNamePathPair.getEndpointPath(), sessionId, this,
                                   endpointMXBean);
        sessions.put(sessionId, sessionMonitor);

        if (sessions.size() > maxOpenSessionsCount) {
            synchronized (maxOpenSessionsCountLock) {
                if (sessions.size() > maxOpenSessionsCount) {
                    maxOpenSessionsCount = sessions.size();
                }
            }
        }

        applicationMonitor.onSessionOpened();

        return new MessageEventListenerImpl(sessionMonitor);
    }

    @Override
    public void onSessionClosed(String sessionId) {
        SessionMonitor session = sessions.remove(sessionId);
        session.unregister();
        applicationMonitor.onSessionClosed();
    }

    @Override
    protected Callable<Integer> getOpenSessionsCount() {
        return new Callable<Integer>() {
            @Override
            public Integer call() {
                return sessions.size();
            }
        };
    }

    @Override
    public void onError(String sessionId, Throwable t) {
        SessionMonitor sessionMonitor = sessions.get(sessionId);
        sessionMonitor.onError(t);

        applicationMonitor.onError(t);
    }
}
