/*
 * Copyright (c) 2013, 2025 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.core;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.websocket.WebSocketContainer;
import org.glassfish.tyrus.core.collection.LazyValue;
import org.glassfish.tyrus.core.collection.Values;
import org.glassfish.tyrus.core.virtual.LoomishExecutors;

/**
 * Base WebSocket container.
 * <p>
 * Client and Server containers extend this to provide additional functionality.
 *
 * @author Jitendra Kotamraju
 */
public abstract class BaseContainer extends ExecutorServiceProvider implements WebSocketContainer {

    private static final Logger LOGGER = Logger.getLogger(BaseContainer.class.getName());

    private final ExecutorService managedExecutorService;
    private final ScheduledExecutorService managedScheduledExecutorService;
    private final ThreadFactory threadFactory;
    /**
     * This lock ensures that only one instance of each type of executors will be created and it also prevents a
     * situation
     * when a client is given an executor that is just about to be shut down.
     */
    private final Object EXECUTORS_CLEAN_UP_LOCK = new Object();

    private volatile ExecutorService executorService = null;
    private volatile ScheduledExecutorService scheduledExecutorService = null;

    public BaseContainer() {
        this.managedExecutorService = lookupManagedExecutorService();
        this.managedScheduledExecutorService = lookupManagedScheduledExecutorService();

        if (managedExecutorService == null || managedScheduledExecutorService == null) {
            // at least one of the managed executor services is null, a local one will be created instead
            threadFactory = new DaemonThreadFactory(this::getProperties);
        } else {
            // only managed executor services will be used, the thread factory won't be needed.
            threadFactory = null;
        }
    }

    /**
     * Returns a container-managed {@link java.util.concurrent.ExecutorService} registered under
     * {@code java:comp/DefaultManagedExecutorService} or if the lookup has failed, it returns a
     * {@link java.util.concurrent.ExecutorService} created and managed by this instance of
     * {@link org.glassfish.tyrus.core.BaseContainer}.
     *
     * @return executor service.
     */
    @Override
    public ExecutorService getExecutorService() {
        if (managedExecutorService != null) {
            return managedExecutorService;
        }

        if (executorService == null) {
            synchronized (EXECUTORS_CLEAN_UP_LOCK) {
                if (executorService == null) {
                    executorService = VirtualThreadUtil.withConfig(getProperties(), threadFactory, null).newCachedThreadPool();
                }
            }
        }

        return executorService;
    }

    /**
     * Returns a container-managed {@link java.util.concurrent.ScheduledExecutorService} registered under
     * {@code java:comp/DefaultManagedScheduledExecutorService} or if the lookup has failed it returns a
     * {@link java.util.concurrent.ScheduledExecutorService} created and managed by this instance of
     * {@link org.glassfish.tyrus.core.BaseContainer}.
     *
     * @return scheduled executor service.
     */
    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        if (managedScheduledExecutorService != null) {
            return managedScheduledExecutorService;
        }

        if (scheduledExecutorService == null) {
            synchronized (EXECUTORS_CLEAN_UP_LOCK) {
                if (scheduledExecutorService == null) {
                    scheduledExecutorService =
                            VirtualThreadUtil.withConfig(getProperties(), threadFactory, null).getScheduledExecutorService(10);
                }
            }
        }

        return scheduledExecutorService;
    }

    /**
     * Release executor services managed by this instance. Executor services obtained via JNDI lookup won't be
     * shut down.
     */
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }

        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
            scheduledExecutorService = null;
        }
    }

    /**
     * Release executor services managed by this instance if the condition passed in the parameter is fulfilled.
     * Executor services obtained via JNDI lookup won't be shut down.
     *
     * @param shutDownCondition condition that will be evaluated before executor services are released and they will be
     *                          released only if the condition is evaluated to {@code true}. The condition will be
     *                          evaluated in a synchronized block in order to make the process of its evaluation
     *                          and executor services release an atomic operation.
     */
    protected void shutdown(ShutDownCondition shutDownCondition) {
        synchronized (EXECUTORS_CLEAN_UP_LOCK) {
            if (shutDownCondition.evaluate()) {
                shutdown();
            }
        }
    }

    /**
     * Container properties.
     * <p>
     * Used to set container specific configuration.
     *
     * @return map containing container properties.
     */
    public abstract Map<String, Object> getProperties();

    private static ExecutorService lookupManagedExecutorService() {
        // Get the default ManagedExecutorService, if available
        try {
            // TYRUS-256: Tyrus client on Android
            final Class<?> aClass = Class.forName("javax.naming.InitialContext");
            final Object o = aClass.newInstance();

            final Method lookupMethod = aClass.getMethod("lookup", String.class);
            return (ExecutorService) lookupMethod.invoke(o, "java:comp/DefaultManagedExecutorService");
        } catch (Exception e) {
            // ignore
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, e.getMessage(), e);
            }
        } catch (LinkageError error) {
            // ignore - JDK8 compact2 profile - http://openjdk.java.net/jeps/161
        }

        return null;
    }

    private static ScheduledExecutorService lookupManagedScheduledExecutorService() {
        try {
            // TYRUS-256: Tyrus client on Android
            final Class<?> aClass = Class.forName("javax.naming.InitialContext");
            final Object o = aClass.newInstance();

            final Method lookupMethod = aClass.getMethod("lookup", String.class);
            return (ScheduledExecutorService) lookupMethod
                    .invoke(o, "java:comp/DefaultManagedScheduledExecutorService");
        } catch (Exception e) {
            // ignore
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, e.getMessage(), e);
            }
        } catch (LinkageError error) {
            // ignore - JDK8 compact2 profile - http://openjdk.java.net/jeps/161
        }

        return null;
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        static final AtomicInteger poolNumber = new AtomicInteger(1);
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;
        final LazyValue<LoomishExecutors> lazyExecutors;

        DaemonThreadFactory(Supplier<Map<String, Object>> mapSupplier) {
            namePrefix = "tyrus-" + poolNumber.getAndIncrement() + "-thread-";
            lazyExecutors = Values.lazy(() -> VirtualThreadUtil.withConfig(mapSupplier.get(), null, null));
        }

        @Override
        public Thread newThread(@SuppressWarnings("NullableProblems") Runnable r) {
            Thread t = lazyExecutors.get().newThread(namePrefix + threadNumber.getAndIncrement(), r);
            if (!t.isDaemon()) {
                t.setDaemon(true);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    protected static interface ShutDownCondition {

        boolean evaluate();
    }
}
