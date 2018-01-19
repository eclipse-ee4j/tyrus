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

package org.glassfish.tyrus.client;

import java.io.IOException;
import java.util.Map;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;

import org.glassfish.tyrus.spi.ClientContainer;
import org.glassfish.tyrus.spi.ClientEngine;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ClientManagerTest {

    @Test
    public void setMaxSessionIdleTimeout() {
        final ClientManager clientManager = createClientManager();

        clientManager.setDefaultMaxSessionIdleTimeout(100);
        assertEquals(100, clientManager.getDefaultMaxSessionIdleTimeout());
    }

    @Test
    public void maxBinaryMessageBufferSize() {
        final ClientManager clientManager = createClientManager();

        clientManager.setDefaultMaxBinaryMessageBufferSize(100);
        assertEquals(100, clientManager.getDefaultMaxBinaryMessageBufferSize());
    }

    @Test
    public void maxTextMessageBufferSize() {
        final ClientManager clientManager = createClientManager();

        clientManager.setDefaultMaxTextMessageBufferSize(100);
        assertEquals(100, clientManager.getDefaultMaxTextMessageBufferSize());

    }

    private ClientManager createClientManager() {
        return ClientManager.createClient(NoopContainer.class.getName());
    }

    public static class NoopContainer implements ClientContainer {

        @Override
        public void openClientSocket(ClientEndpointConfig cec,
                                     Map<String, Object> properties,
                                     ClientEngine clientEngine) throws DeploymentException, IOException {
        }
    }
}
