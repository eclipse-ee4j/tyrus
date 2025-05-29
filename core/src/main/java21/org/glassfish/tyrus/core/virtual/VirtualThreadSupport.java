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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
     *
     * @return true when the current thread is virtual.
     */
    public static boolean isVirtualThread() {
        return Thread.currentThread().isVirtual();
    }

    /**
     * Return an instance of {@link LoomishExecutors} based on a permission to use virtual threads.
     *
     * @param allow         whether to allow virtual threads.
     * @param threadFactory the thread factory to be used by a the {@link ExecutorService}.
     * @return the {@link LoomishExecutors} instance.
     */
    public static LoomishExecutors allowVirtual(boolean allow, ThreadFactory threadFactory) {
        return allow ? new Java21LoomishExecutors(threadFactory) : new NonLoomishExecutors(threadFactory);
    }

    private static class NonLoomishExecutors implements LoomishExecutors {
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

    private static class Java21LoomishExecutors implements LoomishExecutors {
        private final ThreadFactory threadFactory;

        private Java21LoomishExecutors(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
        }

        @Override
        public ExecutorService newCachedThreadPool() {
            return Executors.newThreadPerTaskExecutor(threadFactory);
        }

        @Override
        public ExecutorService newFixedThreadPool(int nThreads) {
            return Executors.newFixedThreadPool(nThreads, threadFactory);
        }

        @Override
        public ScheduledExecutorService getScheduledExecutorService(int nThreads) {
            return new ScheduledExecutorService() {
                final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                final ExecutorService executorService = Executors.newThreadPerTaskExecutor(threadFactory);

                @Override
                public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                    return scheduler.schedule(() -> executorService.submit(command).get(), delay, unit);
                }

                @Override
                public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
                    return scheduler.schedule(() -> executorService.submit(callable).get(), delay, unit);
                }

                @Override
                public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
                    return scheduler.scheduleAtFixedRate(() -> executorService.execute(command), initialDelay, period, unit);
                }

                @Override
                public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
                    return scheduler.scheduleWithFixedDelay(() -> executorService.execute(command), initialDelay, delay, unit);
                }

                @Override
                public void shutdown() {
                    executorService.shutdown();
                    scheduler.shutdown();
                }

                @Override
                public List<Runnable> shutdownNow() {
                    executorService.shutdown();
                    return scheduler.shutdownNow();
                }

                @Override
                public boolean isShutdown() {
                    return scheduler.isShutdown();
                }

                @Override
                public boolean isTerminated() {
                    return scheduler.isTerminated();
                }

                @Override
                public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                    return executorService.awaitTermination(timeout, unit) && scheduler.awaitTermination(timeout, unit);
                }

                @Override
                public <T> Future<T> submit(Callable<T> task) {
                    return executorService.submit(task);
                }

                @Override
                public <T> Future<T> submit(Runnable task, T result) {
                    return executorService.submit(task, result);
                }

                @Override
                public Future<?> submit(Runnable task) {
                    return executorService.submit(task);
                }

                @Override
                public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
                    return executorService.invokeAll(tasks);
                }

                @Override
                public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                        throws InterruptedException {
                    return executorService.invokeAll(tasks, timeout, unit);
                }

                @Override
                public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
                    return executorService.invokeAny(tasks);
                }

                @Override
                public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                        throws InterruptedException, ExecutionException, TimeoutException {
                    return executorService.invokeAny(tasks, timeout, unit);
                }

                @Override
                public void execute(Runnable command) {
                    executorService.execute(command);
                }

            };
        }

        @Override
        public ThreadFactory getThreadFactory() {
            return threadFactory;
        }

        @Override
        public Thread newThread(String name, Runnable runnable) {
            return Thread.ofVirtual().name(name).unstarted(runnable);
        }

        @Override
        public boolean isVirtual() {
            return true;
        }

    }


}
