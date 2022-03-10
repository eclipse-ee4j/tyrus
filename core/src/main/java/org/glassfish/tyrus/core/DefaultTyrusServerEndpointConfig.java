/*
 * Copyright (c) 2014, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.core;

import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Default tyrus-specific implementation of {@code TyrusServerEndpointConfig}.
 *
 * @author Ondrej Kosatka
 */
final class DefaultTyrusServerEndpointConfig extends ServerEndpointConfigWrapper implements TyrusServerEndpointConfig {

    /* maximal number of open sessions */
    private int maxSessions;


    // The builder ensures nothing except configurator can be {@code null}.
    DefaultTyrusServerEndpointConfig(ServerEndpointConfig config, int maxSessions) {
        super(config);
        this.maxSessions = maxSessions;
    }

    @Override
    public int getMaxSessions() {
        return maxSessions;
    }
}
