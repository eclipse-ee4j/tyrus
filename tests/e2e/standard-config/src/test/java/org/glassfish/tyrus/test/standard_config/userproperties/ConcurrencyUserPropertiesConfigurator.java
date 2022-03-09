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

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

public class ConcurrencyUserPropertiesConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        final String value = request.getHeaders().get(ConcurrencyUserPropertiesServer.KEY).get(0);
        switch (value) {
            case "1":
                sec.getUserProperties().put(ConcurrencyUserPropertiesServer.KEY, value);
                if (ConcurrencyUserPropertiesServer.session1Latch.getCount() == 1) {
                    ConcurrencyUserPropertiesServer.session1Latch.countDown();
                } else {
                    throw new IllegalStateException("session1Latch" + ConcurrencyUserPropertiesServer.session1Latch.getCount());
                }
                while (ConcurrencyUserPropertiesServer.session2Latch.getCount() == 1) {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case "2":
                while (ConcurrencyUserPropertiesServer.session1Latch.getCount() == 1) {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (ConcurrencyUserPropertiesServer.session2Latch.getCount() == 1) {
                    sec.getUserProperties().put(ConcurrencyUserPropertiesServer.KEY, value);
                    ConcurrencyUserPropertiesServer.session2Latch.countDown();
                } else {
                    throw new IllegalStateException("session2Latch" + ConcurrencyUserPropertiesServer.session2Latch.getCount());
                }
                break;
            default:
                throw new IllegalStateException("value" + value);
        }

        super.modifyHandshake(sec, request, response);
    }
}
