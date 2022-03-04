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

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Define properties that could be set on the server side to modify Tyrus runtime.
 */
public final class ServerProperties {

    /**
     * When calling
     * {@link ServerEndpointConfig.Configurator#modifyHandshake(ServerEndpointConfig, HandshakeRequest, HandshakeResponse)}, the
     * {@link ServerEndpointConfig} passed in as an argument is wrapped so that invocation of
     * {@link ServerEndpointConfig#getUserProperties()} returns a copy of a properties.
     * <p>
     * A default value is false.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     * @since 2.1.0
     */
    public static String WRAP_SERVER_ENDPOINT_CONFIG_AT_MODIFY_HANDSHAKE
            = "tyrus.config.server.wrap.serverenpointconfig.handshake";

    /**
     * Get property of a given name and a given type.
     * @param properties a set of properties to get the property from.
     * @param name Name of the property
     * @param type Expected type of the property to convert the property to it.
     * @param <T> Expected class type of the property
     * @param defaultValue the value to return if the property is not present or not convertible.
     * @return the converted property or {@code defaultValue} if the property is not present or not convertible.
     */
    public static <T> T getProperty(Map<String, Object> properties, String name, Class<T> type, T defaultValue) {
        Object object = properties.get(name);
        if (object == null) {
            return defaultValue;
        }
        if (type.isInstance(object)) {
            return type.cast(object);
        }
        Function function = converters.get(type);
        if (function != null) {
            return type.cast(function.apply(object));
        }
        return defaultValue;
    }

    private static final Map<Class, Function> converters = new HashMap<>();
    static {
        converters.put(String.class, (Function<String, String>) s -> s);
        converters.put(Integer.class, (Function<String, Integer>) s -> Integer.valueOf(s));
        converters.put(Long.class, (Function<String, Long>) s -> Long.parseLong(s));
        converters.put(Boolean.class, (Function<String, Boolean>) s -> s.equalsIgnoreCase("1")
                ? true
                : Boolean.parseBoolean(s));
    }
}
