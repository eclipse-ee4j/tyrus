/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.servlet;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import org.glassfish.tyrus.core.DebugContext;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.spi.WebSocketEngine;

/**
 * Registers a filter for upgrade handshake.
 * <p>
 * All requests will be handled by registered filter if not specified otherwise.
 *
 * @author Jitendra Kotamraju
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@HandlesTypes({ServerEndpoint.class, ServerApplicationConfig.class, Endpoint.class})
public class TyrusServletContainerInitializer implements ServletContainerInitializer {
    private static final Logger LOGGER = Logger.getLogger(TyrusServletContainerInitializer.class.getName());

    /**
     * Tyrus classes scanned by container will be filtered.
     */
    private static final Set<Class<?>> FILTERED_CLASSES = new HashSet<Class<?>>() {
        {
            add(org.glassfish.tyrus.server.TyrusServerConfiguration.class);
        }
    };

    @Override
    public void onStartup(Set<Class<?>> classes, final ServletContext ctx) throws ServletException {
        if (classes == null || classes.isEmpty()) {
            return;
        }

        classes.removeAll(FILTERED_CLASSES);

        final Integer incomingBufferSize = getIntContextParam(ctx, TyrusHttpUpgradeHandler.FRAME_BUFFER_SIZE);
        final Integer maxSessionsPerApp = getIntContextParam(ctx, TyrusWebSocketEngine.MAX_SESSIONS_PER_APP);
        final Integer maxSessionsPerRemoteAddr = getIntContextParam(
                ctx, TyrusWebSocketEngine.MAX_SESSIONS_PER_REMOTE_ADDR);
        final Boolean parallelBroadcastEnabled = getBooleanContextParam(
                ctx, TyrusWebSocketEngine.PARALLEL_BROADCAST_ENABLED);
        final DebugContext.TracingType tracingType = getEnumContextParam(
                ctx, TyrusWebSocketEngine.TRACING_TYPE, DebugContext.TracingType.class, DebugContext.TracingType.OFF);
        final DebugContext.TracingThreshold tracingThreshold =
                getEnumContextParam(ctx, TyrusWebSocketEngine.TRACING_THRESHOLD, DebugContext.TracingThreshold.class,
                                    DebugContext.TracingThreshold.TRACE);

        final ApplicationEventListener applicationEventListener = createApplicationEventListener(ctx);
        final TyrusServerContainer serverContainer = new TyrusServerContainer(classes) {

            private final WebSocketEngine engine =
                    TyrusWebSocketEngine.builder(this)
                                        .applicationEventListener(applicationEventListener)
                                        .incomingBufferSize(incomingBufferSize)
                                        .maxSessionsPerApp(maxSessionsPerApp)
                                        .maxSessionsPerRemoteAddr(maxSessionsPerRemoteAddr)
                                        .parallelBroadcastEnabled(parallelBroadcastEnabled)
                                        .tracingType(tracingType)
                                        .tracingThreshold(tracingThreshold)
                                        .build();

            @Override
            public void register(Class<?> endpointClass) throws DeploymentException {
                engine.register(endpointClass, ctx.getContextPath());
            }

            @Override
            public void register(ServerEndpointConfig serverEndpointConfig) throws DeploymentException {
                engine.register(serverEndpointConfig, ctx.getContextPath());
            }

            @Override
            public WebSocketEngine getWebSocketEngine() {
                return engine;
            }
        };
        ctx.setAttribute(ServerContainer.class.getName(), serverContainer);
        Boolean wsadlEnabled = getBooleanContextParam(ctx, TyrusWebSocketEngine.WSADL_SUPPORT);
        if (wsadlEnabled == null) {
            wsadlEnabled = false;
        }
        LOGGER.config("WSADL enabled: " + wsadlEnabled);

        TyrusServletFilter filter =
                new TyrusServletFilter((TyrusWebSocketEngine) serverContainer.getWebSocketEngine(), wsadlEnabled);

        // HttpSessionListener registration
        ctx.addListener(filter);

        // Filter registration
        final FilterRegistration.Dynamic reg = ctx.addFilter("WebSocket filter", filter);
        reg.setAsyncSupported(true);
        reg.addMappingForUrlPatterns(null, true, "/*");
        LOGGER.info("Registering WebSocket filter for url pattern /*");
        if (applicationEventListener != null) {
            applicationEventListener.onApplicationInitialized(ctx.getContextPath());
        }
    }

    /**
     * Get {@link Integer} parameter from {@link javax.servlet.ServletContext}.
     *
     * @param ctx       used to retrieve init parameter.
     * @param paramName parameter name.
     * @return parsed {@link Integer} value or {@code null} when the value is not integer or when the init parameter is
     * not present.
     */
    private Integer getIntContextParam(ServletContext ctx, String paramName) {
        String initParameter = ctx.getInitParameter(paramName);
        if (initParameter != null) {
            try {
                return Integer.parseInt(initParameter);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.CONFIG, "Invalid configuration value [" + paramName + " = " + initParameter + "], "
                        + "integer expected");
            }
        }

        return null;
    }

    /**
     * Get {@link java.lang.Boolean} parameter from {@link javax.servlet.ServletContext}.
     *
     * @param ctx       used to retrieve init parameter.
     * @param paramName parameter name.
     * @return parsed {@link java.lang.Boolean} value or {@code null} when the value is not boolean or when the init
     * parameter is not present.
     */
    private Boolean getBooleanContextParam(ServletContext ctx, String paramName) {
        String initParameter = ctx.getInitParameter(paramName);
        if (initParameter != null) {
            if (initParameter.equalsIgnoreCase("true")) {
                return true;
            }

            if (initParameter.equalsIgnoreCase("false")) {
                return false;
            }

            LOGGER.log(Level.CONFIG, "Invalid configuration value [" + paramName + " = " + initParameter + "], "
                    + "boolean expected");
            return null;
        }

        return null;
    }

    private <T extends Enum<T>> T getEnumContextParam(ServletContext ctx, String paramName, Class<T> type, T
            defaultValue) {
        String initParameter = ctx.getInitParameter(paramName);

        if (initParameter == null) {
            return defaultValue;
        }

        try {
            return Enum.valueOf(type, initParameter.trim().toUpperCase(Locale.US));
        } catch (Exception e) {
            LOGGER.log(Level.CONFIG, "Invalid configuration value [" + paramName + " = " + initParameter + "]");
        }

        return defaultValue;
    }

    private ApplicationEventListener createApplicationEventListener(final ServletContext ctx) {
        String listenerClassName = ctx.getInitParameter(ApplicationEventListener.APPLICATION_EVENT_LISTENER);
        if (listenerClassName == null) {
            return null;
        }
        try {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            Class listenerClass = Class.forName(listenerClassName, true, contextClassLoader);

            Object o = listenerClass.newInstance();
            if (o instanceof ApplicationEventListener) {
                return (ApplicationEventListener) o;
            } else {
                LOGGER.log(Level.WARNING, "Class " + listenerClassName + " does not implement "
                        + "ApplicationEventListener");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "ApplicationEventListener implementation " + listenerClassName + " not found", e);
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.log(Level.WARNING, "ApplicationEventListener implementation " + listenerClassName + " could not "
                    + "have been instantiated", e);
        }
        return null;
    }
}
