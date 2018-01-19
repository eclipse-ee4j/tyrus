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

package org.glassfish.tyrus.tests.servlet.autobahn;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.ext.extension.deflate.PerMessageDeflateExtension;
import org.glassfish.tyrus.ext.extension.deflate.XWebkitDeflateExtension;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class AutobahnApplicationConfig implements ServerApplicationConfig {
    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
        final ServerEndpointConfig serverEndpointConfig =
                ServerEndpointConfig.Builder
                        .create(EchoServer.class, "/echo")
                        .extensions(Arrays.<Extension>asList(new PerMessageDeflateExtension(),
                                                             new XWebkitDeflateExtension()))
                        .build();

        return new HashSet<ServerEndpointConfig>() {
            {
                add(serverEndpointConfig);
            }
        };
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
        return Collections.<Class<?>>emptySet();
    }
}
