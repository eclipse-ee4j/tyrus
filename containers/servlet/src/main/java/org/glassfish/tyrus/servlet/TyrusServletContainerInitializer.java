/*
 * Copyright (c) 2012, 2022 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HandlesTypes;

import org.glassfish.tyrus.core.DebugContext;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.core.uri.internal.PathSegment;
import org.glassfish.tyrus.core.uri.internal.UriComponent;
import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.spi.WebSocketEngine;

/**
 * Registers a filter for upgrade handshake.
 * <p>
 * All requests will be handled by registered filter if not specified otherwise.
 *
 * @author Jitendra Kotamraju
 * @author Pavel Bucek
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
        Boolean wsadlEnabled = getBooleanContextParam(ctx, TyrusWebSocketEngine.WSADL_SUPPORT);
        if (wsadlEnabled == null) {
            wsadlEnabled = false;
        }
        LOGGER.config("WSADL enabled: " + wsadlEnabled);

        final ApplicationEventListener applicationEventListener = createApplicationEventListener(ctx);
        final TyrusServerContainerImpl serverContainer = new TyrusServerContainerImpl(classes, applicationEventListener,
                incomingBufferSize, maxSessionsPerApp, maxSessionsPerRemoteAddr, parallelBroadcastEnabled, tracingType,
                tracingThreshold, ctx.getContextPath(), wsadlEnabled);
        ctx.setAttribute(ServerContainer.class.getName(), serverContainer);

        final TyrusServletFilter filter = new TyrusServletFilter(serverContainer.getServletUpgrade());

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
     * Get {@link Integer} parameter from {@link jakarta.servlet.ServletContext}.
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
     * Get {@link java.lang.Boolean} parameter from {@link jakarta.servlet.ServletContext}.
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

    private static class TyrusServerContainerImpl extends TyrusServerContainer {
        private final ApplicationEventListener applicationEventListener;
        private final Integer incomingBufferSize;
        private final Integer maxSessionsPerApp;
        private final Integer maxSessionsPerRemoteAddr;
        private final Boolean parallelBroadcastEnabled;
        private final DebugContext.TracingType tracingType;
        private final DebugContext.TracingThreshold tracingThreshold;
        private final String contextPath;
        private final WebSocketEngine engine;
        private final TyrusServletUpgrade tyrusServletUpgrade;

        public TyrusServerContainerImpl(Set<Class<?>> set, ApplicationEventListener applicationEventListener,
                Integer incomingBufferSize, Integer maxSessionsPerApp, Integer maxSessionsPerRemoteAddr,
                Boolean parallelBroadcastEnabled, DebugContext.TracingType tracingType,
                DebugContext.TracingThreshold tracingThreshold, String contextPath, boolean wsadlEnabled) {
            super(set);
            this.applicationEventListener = applicationEventListener;
            this.incomingBufferSize = incomingBufferSize;
            this.maxSessionsPerApp = maxSessionsPerApp;
            this.maxSessionsPerRemoteAddr = maxSessionsPerRemoteAddr;
            this.parallelBroadcastEnabled = parallelBroadcastEnabled;
            this.tracingType = tracingType;
            this.tracingThreshold = tracingThreshold;
            this.contextPath = contextPath;
            this.engine = TyrusWebSocketEngine.builder(this)
                        .applicationEventListener(applicationEventListener)
                        .incomingBufferSize(incomingBufferSize)
                        .maxSessionsPerApp(maxSessionsPerApp)
                        .maxSessionsPerRemoteAddr(maxSessionsPerRemoteAddr)
                        .parallelBroadcastEnabled(parallelBroadcastEnabled)
                        .tracingType(tracingType)
                        .tracingThreshold(tracingThreshold)
                        .build();
            this.tyrusServletUpgrade = new TyrusServletUpgrade((TyrusWebSocketEngine) engine, wsadlEnabled);

        }

        @Override
        public void register(Class<?> endpointClass) throws DeploymentException {
            engine.register(endpointClass, contextPath);
        }

        @Override
        public void register(ServerEndpointConfig serverEndpointConfig) throws DeploymentException {
            engine.register(serverEndpointConfig, contextPath);
        }

        @Override
        public WebSocketEngine getWebSocketEngine() {
            return engine;
        }

        @Override
        public void upgradeHttpToWebSocket(Object servletRequest, Object servletResponse, ServerEndpointConfig sec,
                                           Map<String, String> pathParameters) throws IOException, DeploymentException {
            final String requestUri = computeRequestPath(sec.getPath(), pathParameters);

            final HttpServletRequest httpServletRequest = new HttpServletRequestWrapper((HttpServletRequest) servletRequest) {
                @Override
                public String getRequestURI() {
                    return contextPath + requestUri;
                }

                @Override
                public String getContextPath() {
                    return "/";
                }

            };

            try {
                tyrusServletUpgrade.upgrade(httpServletRequest, (HttpServletResponse) servletResponse);
            } catch (ServletException e) {
                throw new DeploymentException(e.getMessage(), e);
            }
        }

        private TyrusServletUpgrade getServletUpgrade() {
            return tyrusServletUpgrade;
        }

        private static String computeRequestPath(String path, Map<String, String> pathParams) {
            final StringBuilder resultPath = new StringBuilder();

            final List<PathSegment> endpointPathSegments = UriComponent.decodePath(path, true);
            for (int i = 0; i < endpointPathSegments.size(); i++) {
                resultPath.append('/');
                String endpointSegment = endpointPathSegments.get(i).getPath();

                if (isVariable(endpointSegment)) {
                    final String pathParam = pathParams.get(getVariableName(endpointSegment));
                    resultPath.append(pathParam == null ? endpointSegment : pathParam);
                } else {
                    resultPath.append(endpointSegment);
                }
            }

            final String result = resultPath.toString();
            return result.startsWith("//") ? result.substring(1) : result;
        }

        private static boolean isVariable(String segment) {
            return segment.startsWith("{") && segment.endsWith("}");
        }

        private static String getVariableName(String segment) {
            return segment.substring(1, segment.length() - 1);
        }
    }
}
