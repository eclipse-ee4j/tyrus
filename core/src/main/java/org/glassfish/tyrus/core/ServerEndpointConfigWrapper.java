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

import jakarta.websocket.Decoder;
import jakarta.websocket.Encoder;
import jakarta.websocket.Extension;
import jakarta.websocket.server.ServerEndpointConfig;

import java.util.List;
import java.util.Map;

/**
 * A public class that holds a wrapped ServerEndpointConfig.
 */
public class ServerEndpointConfigWrapper implements ServerEndpointConfig {

    protected final ServerEndpointConfig wrapped;

    /* package */ ServerEndpointConfigWrapper(ServerEndpointConfig wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Class<?> getEndpointClass() {
        return wrapped.getEndpointClass();
    }

    @Override
    public String getPath() {
        return wrapped.getPath();
    }

    @Override
    public List<String> getSubprotocols() {
        return wrapped.getSubprotocols();
    }

    @Override
    public List<Extension> getExtensions() {
        return wrapped.getExtensions();
    }

    @Override
    public Configurator getConfigurator() {
        return wrapped.getConfigurator();
    }

    @Override
    public List<Class<? extends Encoder>> getEncoders() {
        return wrapped.getEncoders();
    }

    @Override
    public List<Class<? extends Decoder>> getDecoders() {
        return wrapped.getDecoders();
    }

    @Override
    public Map<String, Object> getUserProperties() {
        return wrapped.getUserProperties();
    }

    /**
     * Get the wrapped {@link ServerEndpointConfig}.
     * @return the wrapped {@link ServerEndpointConfig}.
     */
    public ServerEndpointConfig getWrapped() {
        return wrapped;
    }
}
