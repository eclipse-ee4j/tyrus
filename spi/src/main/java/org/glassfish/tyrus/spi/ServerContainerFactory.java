/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.spi;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Factory used to get instances of {@link ServerContainer}.
 */
public abstract class ServerContainerFactory {

    private static final String CONTAINTER_CLASS =
            "org.glassfish.tyrus.container.grizzly.server.GrizzlyServerContainer";

    /**
     * Create new {@link org.glassfish.tyrus.spi.ServerContainer} with default configuration.
     *
     * @return new {@link org.glassfish.tyrus.spi.ServerContainer}.
     */
    public static ServerContainer createServerContainer() {
        return createServerContainer(Collections.<String, Object>emptyMap());
    }

    /**
     * Create new {@link org.glassfish.tyrus.spi.ServerContainer} with configuration.
     *
     * @param properties configuration passed to created server container.
     * @return new {@link org.glassfish.tyrus.spi.ServerContainer}.
     */
    public static ServerContainer createServerContainer(final Map<String, Object> properties) {
        ServerContainerFactory factory = null;

        Iterator<ServerContainerFactory> it = ServiceLoader.load(ServerContainerFactory.class).iterator();
        if (it.hasNext()) {
            factory = it.next();
        }
        if (factory == null) {
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                Class factoryClass = (classLoader == null)
                        ? Class.forName(CONTAINTER_CLASS)
                        : classLoader.loadClass(CONTAINTER_CLASS);
                factory = (ServerContainerFactory) factoryClass.newInstance();
            } catch (ClassNotFoundException ce) {
                throw new RuntimeException(ce);
            } catch (InstantiationException ie) {
                throw new RuntimeException(ie);
            } catch (IllegalAccessException ie) {
                throw new RuntimeException(ie);
            }
        }
        return factory.createContainer(properties);
    }

    /**
     * Create container delegate method.
     * <p>
     * Has to be implemented by {@link org.glassfish.tyrus.spi.ServerContainerFactory} implementations.
     *
     * @param properties configuration passed to created server container.
     * @return new {@link org.glassfish.tyrus.spi.ServerContainer}.
     */
    public abstract ServerContainer createContainer(Map<String, Object> properties);

}
