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

package org.glassfish.tyrus.core;

import org.glassfish.tyrus.core.virtual.LoomishExecutors;
import org.glassfish.tyrus.core.virtual.VirtualThreadSupport;

import java.util.Map;
import java.util.concurrent.ThreadFactory;

/**
 * Factory class to provide JDK specific implementation of bits related to the virtual thread support.
 */
final class VirtualThreadUtil {

    private static final boolean USE_VIRTUAL_THREADS_BY_DEFAULT = false;

    /**
     * Do not instantiate.
     */
    private VirtualThreadUtil() {
        throw new IllegalStateException();
    }

    /**
     * Return an instance of {@link LoomishExecutors} based on a configuration property.
     * @param properties the property {@link Map}
     * @param defaultFactory the default factory used if {@link TyrusWebSocketEngine#THREAD_FACTORY} is not used.
     * @param useVirtualByDefault the default use if not said otherwise by property
     * @return the {@link LoomishExecutors} instance.
     */
    public static LoomishExecutors withConfig(
            Map<String, Object> properties,
            ThreadFactory defaultFactory,
            Boolean useVirtualByDefault) {
        ThreadFactory tfThreadFactory = null;
        boolean useVirtualThreads = useVirtualThreads(properties, useVirtualByDefault);

        if (properties != null) {
            Object threadFactory = properties.get(TyrusWebSocketEngine.THREAD_FACTORY);
            if (ThreadFactory.class.isInstance(threadFactory)) {
                tfThreadFactory = (ThreadFactory) threadFactory;
            }
        }

        return tfThreadFactory == null
                ? VirtualThreadSupport.allowVirtual(useVirtualThreads, defaultFactory)
                : VirtualThreadSupport.allowVirtual(useVirtualThreads, tfThreadFactory);
    }

    /**
     * Check configuration if the use of the virtual threads is expected or return the default value if not.
     * @param properties the property {@link Map}
     * @param useByDefault the default expectation
     * @return the expected
     */
    private static boolean useVirtualThreads(Map<String, Object> properties, Boolean useByDefault) {
        boolean bUseVirtualThreads = useByDefault == null ? USE_VIRTUAL_THREADS_BY_DEFAULT : useByDefault;
        if (properties != null) {
            Object useVirtualThread = properties.get(TyrusWebSocketEngine.USE_VIRTUAL_THREADS);
            if (Boolean.class.isInstance(useVirtualThread)) {
                bUseVirtualThreads = (boolean) useVirtualThread;
            }
            if (String.class.isInstance(useVirtualThread)) {
                bUseVirtualThreads = Boolean.parseBoolean(useVirtualThread.toString());
            }
        }
        return bUseVirtualThreads;
    }
}
