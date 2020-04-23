/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerEndpointConfig;

import static org.glassfish.tyrus.spi.Connection.CloseListener;

/**
 * WebSocket engine is used for upgrading HTTP requests into websocket connections. A transport gets hold of the engine
 * from the {@link ServerContainer} and upgrades HTTP handshake requests.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public interface WebSocketEngine {

    /**
     * A transport calls this method to upgrade a HTTP request.
     *
     * @param request  request to be upgraded.
     * @param response response to the upgrade request.
     * @return info about upgrade status and connection details.
     */
    UpgradeInfo upgrade(UpgradeRequest request, UpgradeResponse response);

    // TODO : constructor? / List<Class<?> / List<ServerEndpointConfig>\
    // (one call instead of iteration).
    // TODO remove ??

    /**
     * Register endpoint class.
     *
     * @param endpointClass endpoint class to be registered.
     * @param contextPath   context path of the registered endpoint.
     * @throws DeploymentException when the endpoint is invalid.
     */
    void register(Class<?> endpointClass, String contextPath) throws DeploymentException;

    /**
     * Register {@link jakarta.websocket.server.ServerEndpointConfig}.
     *
     * @param serverConfig server endpoint to be registered.
     * @param contextPath  context path of the registered endpoint.
     * @throws DeploymentException when the endpoint is invalid.
     */
    void register(ServerEndpointConfig serverConfig, String contextPath) throws DeploymentException;

    /**
     * Upgrade info that includes status for HTTP request upgrading and connection creation details.
     */
    interface UpgradeInfo {

        /**
         * Returns the status of HTTP request upgrade.
         *
         * @return status of the upgrade.
         */
        UpgradeStatus getStatus();

        /**
         * Creates a connection if the upgrade is successful. Tyrus would call onConnect lifecycle method on the
         * endpoint during the invocation of this method.
         *
         * @param writer        transport writer that actually writes tyrus websocket data to underlying connection.
         * @param closeListener transport listener for receiving tyrus close notifications.
         * @return upgraded connection if the upgrade is successful otherwise null.
         */
        Connection createConnection(Writer writer, CloseListener closeListener);
    }

    /**
     * Upgrade Status for HTTP request upgrading.
     */
    enum UpgradeStatus {
        /**
         * Not a WebSocketRequest or no mapping in the application. This may mean that HTTP request processing should
         * continue (in servlet container, the next filter may be called).
         */
        NOT_APPLICABLE,

        /**
         * Upgrade failed due to version, extensions, origin check etc. Tyrus would set an appropriate HTTP error status
         * code in {@link UpgradeResponse}.
         */
        HANDSHAKE_FAILED,

        /**
         * Upgrade is successful.
         */
        SUCCESS
    }
}
