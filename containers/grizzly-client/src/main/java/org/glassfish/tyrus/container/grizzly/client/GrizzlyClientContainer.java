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

package org.glassfish.tyrus.container.grizzly.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;

import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.spi.ClientContainer;
import org.glassfish.tyrus.spi.ClientEngine;

/**
 * @author Danny Coward (danny.coward at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class GrizzlyClientContainer implements ClientContainer {

    /**
     * @deprecated please use {@link org.glassfish.tyrus.client.ClientProperties#SSL_ENGINE_CONFIGURATOR}.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static final String SSL_ENGINE_CONFIGURATOR = ClientProperties.SSL_ENGINE_CONFIGURATOR;

    /**
     * When set to {@code true} (boolean value), client runtime preserves used container and reuses it for outgoing
     * connections.
     *
     * @see ClientProperties#SHARED_CONTAINER_IDLE_TIMEOUT
     * @deprecated please use {@link org.glassfish.tyrus.client.ClientProperties#SHARED_CONTAINER}.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static final String SHARED_CONTAINER = ClientProperties.SHARED_CONTAINER;

    /**
     * Container idle timeout in seconds (Integer value).
     *
     * @see ClientProperties#SHARED_CONTAINER
     * @deprecated please use {@link org.glassfish.tyrus.client.ClientProperties#SHARED_CONTAINER_IDLE_TIMEOUT}.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static final String SHARED_CONTAINER_IDLE_TIMEOUT = ClientProperties.SHARED_CONTAINER_IDLE_TIMEOUT;

    //The same value Grizzly is using for socket timeout.
    private static final long CLIENT_SOCKET_TIMEOUT = 30000;

    @Override
    public void openClientSocket(ClientEndpointConfig cec,
                                 Map<String, Object> properties,
                                 ClientEngine clientEngine
    ) throws DeploymentException, IOException {


        new GrizzlyClientSocket(CLIENT_SOCKET_TIMEOUT, clientEngine, properties).connect();
    }
}
