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

package org.glassfish.tyrus.spi;

import java.io.IOException;
import java.util.Map;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;

/**
 * Entry point for client implementation.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public interface ClientContainer {

    /**
     * Property name for maximal incoming buffer size.
     * <p>
     * Can be set in properties map (see {@link #openClientSocket(javax.websocket.ClientEndpointConfig, java.util.Map,
     * ClientEngine)}).
     *
     * @deprecated please use {@code org.glassfish.tyrus.client.ClientProperties#INCOMING_BUFFER_SIZE}.
     */
    String INCOMING_BUFFER_SIZE = "org.glassfish.tyrus.incomingBufferSize";

    /**
     * WLS version of {@link org.glassfish.tyrus.spi.ClientContainer#INCOMING_BUFFER_SIZE}.
     */
    String WLS_INCOMING_BUFFER_SIZE = "weblogic.websocket.tyrus.incoming-buffer-size";

    /**
     * Open client socket - connect to endpoint specified with {@code url} parameter.
     * <p>
     * Called from ClientManager when {@link javax.websocket.WebSocketContainer#connectToServer(Class,
     * javax.websocket.ClientEndpointConfig, java.net.URI)} is invoked.
     *
     * @param cec          endpoint configuration. SPI consumer can access user properties, {@link
     *                     javax.websocket.ClientEndpointConfig.Configurator}, extensions and subprotocol
     *                     configuration,
     *                     etc..
     * @param properties   properties passed from client container. Don't mix up this with {@link
     *                     javax.websocket.ClientEndpointConfig#getUserProperties()}, these are Tyrus proprietary.
     * @param clientEngine one instance equals to one connection, cannot be reused. Implementation is expected to call
     *                     {@link ClientEngine#createUpgradeRequest(ClientEngine.TimeoutHandler)} and {@link
     *                     ClientEngine#processResponse(UpgradeResponse, Writer,
     *                     org.glassfish.tyrus.spi.Connection.CloseListener)} (in that order).
     * @throws javax.websocket.DeploymentException when the client endpoint is invalid or when there is any other (not
     *                                             specified) connection problem.
     * @throws java.io.IOException                 when there is any I/O issue related to opening client socket or
     *                                             connecting to remote endpoint.
     */
    void openClientSocket(ClientEndpointConfig cec, Map<String, Object> properties, ClientEngine clientEngine) throws
            DeploymentException, IOException;
}
