/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.container.grizzly.client;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

/**
 * Timeout filter used for shared container.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
class GrizzlyTransportTimeoutFilter extends BaseFilter {

    private static final Logger LOGGER = Logger.getLogger(GrizzlyTransportTimeoutFilter.class.getName());
    private static final AtomicInteger connectionCounter = new AtomicInteger(0);
    private static final ScheduledExecutorService executorService =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("tyrus-grizzly-container-idle-timeout");
                    thread.setDaemon(true);
                    return thread;
                }
            });

    /**
     * Should be updated whenever you don't want to the container to be stopped. (lastAccessed + timeout) is used for
     * evaluating timeout condition when there are no ongoing connections.
     */
    private static volatile long lastAccessed;
    private static volatile boolean closed;
    private static volatile ScheduledFuture<?> timeoutTask;

    private final int timeout;

    public GrizzlyTransportTimeoutFilter(int timeout) {
        this.timeout = timeout;
        closed = false;
    }

    /**
     * Update last accessed info.
     */
    public static void touch() {
        lastAccessed = System.currentTimeMillis();
    }

    @Override
    public NextAction handleConnect(FilterChainContext ctx) throws IOException {
        connectionCounter.incrementAndGet();
        touch();
        return super.handleConnect(ctx);
    }

    @Override
    public NextAction handleClose(FilterChainContext ctx) throws IOException {
        final int connectionCount = connectionCounter.decrementAndGet();
        touch();

        if (connectionCount == 0 && timeoutTask == null) {
            LOGGER.log(Level.FINER, "Scheduling IdleTimeoutTransportTask: " + timeout + " seconds.");
            timeoutTask = executorService
                    .schedule(new IdleTimeoutTransportTask(connectionCounter), timeout, TimeUnit.SECONDS);
        }

        return super.handleClose(ctx);
    }

    private class IdleTimeoutTransportTask implements Runnable {

        private final AtomicInteger connectionCounter;

        private IdleTimeoutTransportTask(AtomicInteger connectionCounter) {
            this.connectionCounter = connectionCounter;
        }

        @Override
        public void run() {
            if (connectionCounter.get() == 0 && !closed) {
                final long currentTime = System.currentTimeMillis();
                if ((lastAccessed + (timeout * 1000)) < currentTime) {
                    closed = true;
                    timeoutTask = null;
                    GrizzlyClientSocket.closeSharedTransport();
                } else {
                    final long delay = (lastAccessed + (timeout * 1000)) - currentTime;
                    LOGGER.log(Level.FINER, "Scheduling IdleTimeoutTransportTask: " + delay / 1000 + " seconds.");

                    timeoutTask = executorService
                            .schedule(new IdleTimeoutTransportTask(connectionCounter), delay, TimeUnit.MILLISECONDS);
                }
            }
        }
    }
}
