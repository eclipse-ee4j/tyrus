/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.test.standard_config.userproperties;

import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

@ServerEndpoint(value = "/onopenserver", configurator = OnOpenConfigurator.class)
public class OnOpenServer {
    ServerEndpointConfig config;

    @OnMessage
    public String onMessage(String msg) {
        boolean ret = false;
        if (msg.equals("config")) {
            ret = OnOpenConfigurator.getConfig().getClass().getName()
                    .equals(config.getClass().getName());
        }
        return String.valueOf(ret);
    }

    @OnOpen
    public void onOpen(EndpointConfig config) {
        this.config = (ServerEndpointConfig) config;
    }

    @OnError
    public void onError(Session session, Throwable thr) {
        thr.printStackTrace(); // Write to error log, too
    }
}
