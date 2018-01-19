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

package org.glassfish.tyrus.container.grizzly.client;

/**
 * Grizzly client properties.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public final class GrizzlyClientProperties {

    /**
     * Client-side property to set custom worker {@link org.glassfish.grizzly.threadpool.ThreadPoolConfig}.
     * <p>
     * Value is expected to be instance of {@link org.glassfish.grizzly.threadpool.ThreadPoolConfig}, can be {@code
     * null} (it won't be used).
     * <p>
     * If both {@link #WORKER_THREAD_POOL_CONFIG} nad  {@link org.glassfish.tyrus.client
     * .ClientProperties#WORKER_THREAD_POOL_CONFIG} are set, configuration set in {@link #WORKER_THREAD_POOL_CONFIG}
     * will be used.
     */
    public static final String WORKER_THREAD_POOL_CONFIG = "org.glassfish.tyrus.client.grizzly.workerThreadPoolConfig";

    /**
     * Client-side property to set custom selector {@link org.glassfish.grizzly.threadpool.ThreadPoolConfig}.
     * <p>
     * Value is expected to be instance of {@link org.glassfish.grizzly.threadpool.ThreadPoolConfig}, can be {@code
     * null} (it won't be used).
     */
    public static final String SELECTOR_THREAD_POOL_CONFIG =
            "org.glassfish.tyrus.client.grizzly.selectorThreadPoolConfig";
}
