/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
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

import jakarta.websocket.ClientEndpointConfig;
import java.util.Map;

/**
 * Extended Configurator that can be used for subclassing the user provided configurator.
 * If done so, the additional methods are invoked as described by the methods.
 */
public class TyrusClientEndpointConfigurator extends ClientEndpointConfig.Configurator {

    /**
     * This method is called by the implementation after it has formulated the handshake request that will be used
     * to initiate the connection to the server, but before {@link #beforeRequest(Map)} is invoked. This allows the
     * developer to inspect the handshake request itself prior to the start of the handshake interaction.
     * <p>
     *     For modifying the HandshakeRequestHeaders, use {@link #beforeRequest(Map)}.
     * </p>
     *
     * @param upgradeRequest the read-only handshake request the implementation is about to send to start the
     *                       handshake interaction.
     */
    public void beforeRequest(UpgradeRequest upgradeRequest) {

    }
}
