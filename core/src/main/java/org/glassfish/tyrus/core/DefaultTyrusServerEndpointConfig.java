/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;

/**
 * Default tyrus-specific implementation of {@code TyrusServerEndpointConfig}.
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
final class DefaultTyrusServerEndpointConfig implements TyrusServerEndpointConfig {

    /* wrapped origin config */
    private ServerEndpointConfig config;
    /* maximal number of open sessions */
    private int maxSessions;


    // The builder ensures nothing except configurator can be {@code null}.
    DefaultTyrusServerEndpointConfig(ServerEndpointConfig config, int maxSessions) {
        this.config = config;
        this.maxSessions = maxSessions;
    }

    @Override
    public int getMaxSessions() {
        return maxSessions;
    }

    @Override
    public Class<?> getEndpointClass() {
        return config.getEndpointClass();
    }

    @Override
    public List<Class<? extends Encoder>> getEncoders() {
        return config.getEncoders();
    }

    @Override
    public List<Class<? extends Decoder>> getDecoders() {
        return config.getDecoders();
    }

    @Override
    public String getPath() {
        return config.getPath();
    }

    @Override
    public ServerEndpointConfig.Configurator getConfigurator() {
        return config.getConfigurator();
    }

    @Override
    public final Map<String, Object> getUserProperties() {
        return config.getUserProperties();
    }

    @Override
    public final List<String> getSubprotocols() {
        return config.getSubprotocols();
    }

    @Override
    public final List<Extension> getExtensions() {
        return config.getExtensions();
    }

}
