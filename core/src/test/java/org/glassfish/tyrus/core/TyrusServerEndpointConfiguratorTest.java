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

package org.glassfish.tyrus.core;

import org.junit.Assert;
import org.junit.Test;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class TyrusServerEndpointConfiguratorTest {
    @Test
    public void overridesGetEndpointInstanceTest() {
        ServerEndpointConfig.Configurator config = new TestServerEndpointConfigurator();
        Assert.assertFalse(TyrusServerEndpointConfigurator.overridesGetEndpointInstance(config));

        config = new TestServerEndpointConfigurator();
        Assert.assertFalse(TyrusServerEndpointConfigurator.overridesGetEndpointInstance(config));

        config = new TyrusServerEndpointConfigurator();
        Assert.assertFalse(TyrusServerEndpointConfigurator.overridesGetEndpointInstance(config));

        config = new TyrusServerEndpointConfigurator(){};
        Assert.assertFalse(TyrusServerEndpointConfigurator.overridesGetEndpointInstance(config));

        config = new TestServerEndpointConfigurator2();
        Assert.assertTrue(TyrusServerEndpointConfigurator.overridesGetEndpointInstance(config));
    }

    private static class TestServerEndpointConfigurator extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            super.modifyHandshake(sec, request, response);
        }
    }

    private static class TestServerEndpointConfigurator2 extends ServerEndpointConfig.Configurator {
        @Override
        public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
            return super.getEndpointInstance(endpointClass);
        }
    }
}
