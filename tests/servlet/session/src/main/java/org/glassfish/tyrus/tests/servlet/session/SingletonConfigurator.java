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

package org.glassfish.tyrus.tests.servlet.session;

import java.util.HashMap;
import java.util.Map;

import javax.websocket.server.ServerEndpointConfig;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class SingletonConfigurator extends ServerEndpointConfig.Configurator {

    private static final CloseClientEndpoint CLOSE_CLIENT_ENDPOINT = new CloseClientEndpoint();
    private static final CloseServerEndpoint CLOSE_SERVER_ENDPOINT = new CloseServerEndpoint();
    private static final IdleTimeoutReceivingEndpoint IDLE_TIMEOUT_RECEIVING_ENDPOINT = new
            IdleTimeoutReceivingEndpoint();
    private static final IdleTimeoutSendingEndpoint IDLE_TIMEOUT_SENDING_ENDPOINT = new IdleTimeoutSendingEndpoint();
    private static final IdleTimeoutSendingPingEndpoint IDLE_TIMEOUT_SENDING_PING_ENDPOINT = new
            IdleTimeoutSendingPingEndpoint();

    private static final Map<Class<?>, Object> instanceMap = new HashMap<Class<?>, Object>() {
        {
            put(CloseClientEndpoint.class, CLOSE_CLIENT_ENDPOINT);
            put(CloseServerEndpoint.class, CLOSE_SERVER_ENDPOINT);
            put(IdleTimeoutReceivingEndpoint.class, IDLE_TIMEOUT_RECEIVING_ENDPOINT);
            put(IdleTimeoutSendingEndpoint.class, IDLE_TIMEOUT_SENDING_ENDPOINT);
            put(IdleTimeoutSendingPingEndpoint.class, IDLE_TIMEOUT_SENDING_PING_ENDPOINT);
        }
    };

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        return SingletonConfigurator.getEndpoint(endpointClass);
    }

    public static <T> T getEndpoint(Class<T> endpointClass) {
        return (T) instanceMap.get(endpointClass);
    }
}
