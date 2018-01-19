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

import java.io.IOException;

import javax.websocket.DeploymentException;


/**
 * HTTP server abstraction.
 * <p>
 * TODO rename the class to avoid findbugs
 *
 * @author Martin Matula (martin.matula at oracle.com)
 */
public interface ServerContainer extends javax.websocket.server.ServerContainer {

    /**
     * Start the server.
     * <p>
     * Creates a new embedded HTTP server (if supported) listening to incoming connections at a given root path
     * and port.
     *
     * @param rootPath context root
     * @param port     TCP port
     * @throws IOException                         if something goes wrong.
     * @throws javax.websocket.DeploymentException when there is any issue with endpoints or other, non-specific
     *                                             issues.
     */
    void start(String rootPath, int port) throws IOException, DeploymentException;

    /**
     * Stop the server.
     */
    void stop();

    /**
     * Return WebSocketEngine to upgrade requests and setting up the connection.
     *
     * @return websocket engine
     */
    WebSocketEngine getWebSocketEngine();
}
