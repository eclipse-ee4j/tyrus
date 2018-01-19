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

package org.glassfish.tyrus.tests.servlet.maxsessions;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

/**
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
public class MaxSessionPerAppApplicationConfig implements ServerApplicationConfig {

    static final String[] PATHS = new String[]{"/echo1", "/echo2", "/echo3"};
    // session limit - as defined in web.xml
    static final int MAX_SESSIONS_PER_APP = 5;
    static CountDownLatch closeLatch = new CountDownLatch(MAX_SESSIONS_PER_APP);
    static CountDownLatch openLatch = new CountDownLatch(MAX_SESSIONS_PER_APP);

    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
        return new HashSet<ServerEndpointConfig>() {
            {
                for (String PATH : PATHS) {
                    add(ServerEndpointConfig.Builder.create(EchoEndpoint.class, PATH).build());
                }
            }
        };
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
        return new HashSet<Class<?>>() {
            {
                add(ServiceEndpoint.class);
            }
        };
    }
}
