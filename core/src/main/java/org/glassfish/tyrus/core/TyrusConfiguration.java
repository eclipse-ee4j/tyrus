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

import jakarta.websocket.server.ServerEndpointConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Inner Tyrus configuration properties holder object.
 * @since 2.1
 */
public interface TyrusConfiguration {
    /**
     * Get an immutable map of properties provided by
     * @return immutable property {@link Map} for optional Tyrus behavior
     */
    Map<String, Object> tyrusProperties();

    /**
     * Get a mutable copy of user properties first obtained by {@link ServerEndpointConfig.Configurator#getUserProperties()}.
     * @return a mutable {@link Map} of user properties.
     */
    Map<String, Object> userProperties();

    /* package */ static class Builder {
        private Map<String, Object> tyrusProperties;
        private Map<String, Object> userProperties;

        /* package */ Builder tyrusProperties(Map<String, Object> tyrusProperties) {
            this.tyrusProperties = tyrusProperties;
            return this;
        }

        /* package */ Builder userProperties(Map<String, Object> userProperties) {
            this.userProperties = userProperties;
            return this;
        }

        TyrusConfiguration build() {
            tyrusProperties = tyrusProperties == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(tyrusProperties);
            userProperties = userProperties == null ? new HashMap<>() : userProperties;
            return new TyrusConfiguration() {

                @Override
                public Map<String, Object> tyrusProperties() {
                    return tyrusProperties;
                }

                @Override
                public Map<String, Object> userProperties() {
                    return userProperties;
                }
            };
        }
    }

    /* package */ static final TyrusConfiguration EMPTY_CONFIGURATION = new Builder().build();
}
