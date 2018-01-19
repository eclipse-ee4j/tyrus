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

package org.glassfish.tyrus.tests.servlet.remote;

import java.util.HashMap;
import java.util.Map;

import javax.websocket.server.ServerEndpointConfig;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class SingletonConfigurator extends ServerEndpointConfig.Configurator {

    private static final NoTimeoutEndpointResultByFuture NO_TIMEOUT_ENDPOINT_RESULT_BY_FUTURE =
            new NoTimeoutEndpointResultByFuture();
    private static final NoTimeoutEndpointResultByHandler NO_TIMEOUT_ENDPOINT_RESULT_BY_HANDLER =
            new NoTimeoutEndpointResultByHandler();
    private static final TimeoutEndpointResultByFuture TIMEOUT_ENDPOINT_RESULT_BY_FUTURE =
            new TimeoutEndpointResultByFuture();
    private static final TimeoutEndpointResultByHandler TIMEOUT_ENDPOINT_RESULT_BY_HANDLER =
            new TimeoutEndpointResultByHandler();


    private static final Map<Class<?>, Object> instanceMap = new HashMap<Class<?>, Object>() {
        {
            put(NoTimeoutEndpointResultByFuture.class, NO_TIMEOUT_ENDPOINT_RESULT_BY_FUTURE);
            put(NoTimeoutEndpointResultByHandler.class, NO_TIMEOUT_ENDPOINT_RESULT_BY_HANDLER);
            put(TimeoutEndpointResultByFuture.class, TIMEOUT_ENDPOINT_RESULT_BY_FUTURE);
            put(TimeoutEndpointResultByHandler.class, TIMEOUT_ENDPOINT_RESULT_BY_HANDLER);
        }
    };

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        return SingletonConfigurator.getEndpoint(endpointClass);
    }

    public static <T> T getEndpoint(Class<T> endpointClass) {
        //noinspection unchecked
        return (T) instanceMap.get(endpointClass);
    }
}
