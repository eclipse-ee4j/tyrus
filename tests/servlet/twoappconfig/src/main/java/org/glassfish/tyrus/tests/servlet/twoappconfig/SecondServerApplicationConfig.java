/*
 * Copyright (c) 2013, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.servlet.twoappconfig;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * @author Stepan Kopriva
 * @author Pavel Bucek
 */
public class SecondServerApplicationConfig implements ServerApplicationConfig {

    private static AtomicBoolean annotatedGetterCalled = new AtomicBoolean(false);

    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
        return null;
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
        annotatedGetterCalled.set(true);
        HashSet<Class<?>> toReturn = new HashSet<Class<?>>();
        toReturn.add(ConfigurationChecker.class);

        return toReturn;
    }

    public static boolean isAnnotatedGetterCalled() {
        return annotatedGetterCalled.get();
    }
}
