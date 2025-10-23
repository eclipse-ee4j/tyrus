/*
 * Copyright (c) 2025 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.core.virtual;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Utility class for the virtual thread support.
 */
public final class VirtualThreadSupport {

    /**
     * Do not instantiate.
     */
    private VirtualThreadSupport() {
        throw new IllegalStateException();
    }

    /**
     * Informs whether the given {@link Thread} is virtual.
     * @return true when the current thread is virtual.
     */
    public static boolean isVirtualThread() {
        return false;
    }

    /**
     * Return an instance of {@link LoomishExecutors} based on a permission to use virtual threads.
     * @param allow whether to allow virtual threads.
     * @param threadFactory the thread factory to be used by a the {@link ExecutorService}.
     * @return the {@link LoomishExecutors} instance.
     */
    public static LoomishExecutors allowVirtual(boolean allow, ThreadFactory threadFactory) {
        return new NonLoomishExecutors(threadFactory);
    }

    private static final class NonLoomishExecutors implements LoomishExecutors {
        private final ThreadFactory threadFactory;

        private NonLoomishExecutors(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
        }

        @Override
        public ExecutorService newCachedThreadPool() {
            return Executors.newCachedThreadPool(threadFactory);
        }

        @Override
        public ExecutorService newFixedThreadPool(int nThreads) {
            return Executors.newFixedThreadPool(nThreads, threadFactory);
        }

        @Override
        public ScheduledExecutorService getScheduledExecutorService(int nThreads) {
            return Executors.newScheduledThreadPool(nThreads, threadFactory);
        }

        @Override
        public ThreadFactory getThreadFactory() {
            return threadFactory;
        }

        @Override
        public Thread newThread(String name, Runnable runnable) {
            return new Thread(null, runnable, name, 0);
        }

        @Override
        public boolean isVirtual() {
            return false;
        }
    }
}
