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

package org.glassfish.tyrus.container.grizzly.server;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerEndpointConfig;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import org.glassfish.tyrus.core.DebugContext;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.core.cluster.ClusterContext;
import org.glassfish.tyrus.core.monitoring.ApplicationEventListener;
import org.glassfish.tyrus.core.wsadl.model.Application;
import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.spi.ServerContainer;
import org.glassfish.tyrus.spi.ServerContainerFactory;
import org.glassfish.tyrus.spi.WebSocketEngine;

import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.http.util.ContentType;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

/**
 * Grizzly implementation of {@link ServerContainerFactory} and {@link ServerContainer}.
 *
 * @author Danny Coward
 * @author Pavel Bucek
 */
public class GrizzlyServerContainer extends ServerContainerFactory {

    /**
     * Server-side property to set custom worker {@link ThreadPoolConfig}.
     * <p>
     * Value is expected to be instance of {@link ThreadPoolConfig}, can be {@code null} (it won't be used).
     */
    public static final String WORKER_THREAD_POOL_CONFIG =
            "org.glassfish.tyrus.container.grizzly.server.workerThreadPoolConfig";

    /**
     * Server-side property to set custom selector {@link ThreadPoolConfig}.
     * <p>
     * Value is expected to be instance of {@link ThreadPoolConfig}, can be {@code null} (it won't be used).
     */
    public static final String SELECTOR_THREAD_POOL_CONFIG =
            "org.glassfish.tyrus.container.grizzly.server.selectorThreadPoolConfig";

    @Override
    public ServerContainer createContainer(Map<String, Object> properties) {

        final Map<String, Object> localProperties;
        // defensive copy
        if (properties == null) {
            localProperties = Collections.emptyMap();
        } else {
            localProperties = new HashMap<String, Object>(properties);
        }

        return new TyrusGrizzlyServerContainer(localProperties);
    }

    /* package */ static class TyrusGrizzlyServerContainer extends TyrusServerContainer {
        private final Map<String, Object> localProperties;
        private final WebSocketEngine engine;
        private final ApplicationEventListener applicationEventListener;

        TyrusGrizzlyServerContainer(Map<String, Object> properties) {
            super((Set<Class<?>>) null);
            this.localProperties = properties;

            final Integer incomingBufferSize =
                    Utils.getProperty(localProperties, TyrusWebSocketEngine.INCOMING_BUFFER_SIZE, Integer.class);
            final ClusterContext clusterContext =
                    Utils.getProperty(localProperties, ClusterContext.CLUSTER_CONTEXT, ClusterContext.class);
            final Integer maxSessionsPerApp =
                    Utils.getProperty(localProperties, TyrusWebSocketEngine.MAX_SESSIONS_PER_APP, Integer.class);
            final Integer maxSessionsPerRemoteAddr =
                    Utils.getProperty(localProperties, TyrusWebSocketEngine.MAX_SESSIONS_PER_REMOTE_ADDR, Integer.class);
            final Boolean parallelBroadcastEnabled =
                    Utils.getProperty(localProperties, TyrusWebSocketEngine.PARALLEL_BROADCAST_ENABLED, Boolean.class);
            final DebugContext.TracingType tracingType =
                    Utils.getProperty(localProperties, TyrusWebSocketEngine.TRACING_TYPE, DebugContext.TracingType.class,
                            DebugContext.TracingType.OFF);
            final DebugContext.TracingThreshold tracingThreshold =
                    Utils.getProperty(localProperties, TyrusWebSocketEngine.TRACING_THRESHOLD,
                            DebugContext.TracingThreshold.class, DebugContext.TracingThreshold.TRACE);

            applicationEventListener = Utils.getProperty(localProperties, ApplicationEventListener.APPLICATION_EVENT_LISTENER,
                            ApplicationEventListener.class);

            engine = TyrusWebSocketEngine.builder(this)
                            .incomingBufferSize(incomingBufferSize)
                            .clusterContext(clusterContext)
                            .applicationEventListener(applicationEventListener)
                            .maxSessionsPerApp(maxSessionsPerApp)
                            .maxSessionsPerRemoteAddr(maxSessionsPerRemoteAddr)
                            .parallelBroadcastEnabled(parallelBroadcastEnabled)
                            .tracingType(tracingType)
                            .tracingThreshold(tracingThreshold)
                            .build();
        }

        private HttpServer server;
        private String contextPath;
        private volatile NetworkListener listener = null;

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
        public void start(final String rootPath, int port) throws IOException, DeploymentException {
            contextPath = rootPath;
            server = new HttpServer();
            final ServerConfiguration config = server.getServerConfiguration();

            listener = new NetworkListener("grizzly", "0.0.0.0", port);
            server.addListener(listener);

            // server = HttpServer.createSimpleServer(rootPath, port);
            ThreadPoolConfig workerThreadPoolConfig =
                    Utils.getProperty(localProperties, WORKER_THREAD_POOL_CONFIG, ThreadPoolConfig.class);
            ThreadPoolConfig selectorThreadPoolConfig =
                    Utils.getProperty(localProperties, SELECTOR_THREAD_POOL_CONFIG, ThreadPoolConfig.class);

            // TYRUS-287: configurable server thread pools
            if (workerThreadPoolConfig != null || selectorThreadPoolConfig != null) {
                TCPNIOTransportBuilder transportBuilder = TCPNIOTransportBuilder.newInstance();
                if (workerThreadPoolConfig != null) {
                    transportBuilder.setWorkerThreadPoolConfig(workerThreadPoolConfig);
                }
                if (selectorThreadPoolConfig != null) {
                    transportBuilder.setSelectorThreadPoolConfig(selectorThreadPoolConfig);
                }
                transportBuilder.setIOStrategy(WorkerThreadIOStrategy.getInstance());
                server.getListener("grizzly").setTransport(transportBuilder.build());
            } else {
                // if no configuration is set, just update IO Strategy to worker thread strat.
                server.getListener("grizzly").getTransport().setIOStrategy(WorkerThreadIOStrategy.getInstance());
            }

            // idle timeout set to indefinite.
            server.getListener("grizzly").getKeepAlive().setIdleTimeoutInSeconds(-1);
            server.getListener("grizzly").registerAddOn(new WebSocketAddOn(this, contextPath));

            final WebSocketEngine webSocketEngine = getWebSocketEngine();

            final Object staticContentPath = localProperties.get(Server.STATIC_CONTENT_ROOT);
            HttpHandler staticHandler = null;
            if (staticContentPath != null && !staticContentPath.toString().isEmpty()) {
                staticHandler = new StaticHttpHandler(staticContentPath.toString());
            }

            final Object wsadl = localProperties.get(TyrusWebSocketEngine.WSADL_SUPPORT);

            if (wsadl != null && wsadl.toString().equalsIgnoreCase("true")) { // wsadl enabled
                config.addHttpHandler(new WsadlHttpHandler((TyrusWebSocketEngine) webSocketEngine, staticHandler));
            } else if (staticHandler != null) { // wsadl disabled
                config.addHttpHandler(staticHandler);
            }

            if (applicationEventListener != null) {
                applicationEventListener.onApplicationInitialized(rootPath);
            }

            server.start();
            super.start(rootPath, port);
        }

        @Override
        public int getPort() {
            if (listener != null && listener.getPort() > 0) {
                return listener.getPort();
            } else {
                return -1;
            }
        }

        @Override
        public void stop() {
            super.stop();
            server.shutdownNow();
            if (applicationEventListener != null) {
                applicationEventListener.onApplicationDestroyed();
            }
        }

        /* package */ Map<String, Object> getProperties() {
            return localProperties;
        }
    }

    private static class WsadlHttpHandler extends HttpHandler {

        private final TyrusWebSocketEngine engine;
        private final HttpHandler staticHttpHandler;

        private JAXBContext wsadlJaxbContext;

        private WsadlHttpHandler(TyrusWebSocketEngine engine, HttpHandler staticHttpHandler) {
            this.engine = engine;
            this.staticHttpHandler = staticHttpHandler;
        }

        private synchronized JAXBContext getWsadlJaxbContext() throws JAXBException {
            if (wsadlJaxbContext == null) {
                wsadlJaxbContext = JAXBContext.newInstance(Application.class.getPackage().getName());
            }
            return wsadlJaxbContext;
        }

        @Override
        public void service(Request request, Response response) throws Exception {
            if (request.getMethod().equals(Method.GET) && request.getRequestURI().endsWith("application.wsadl")) {

                getWsadlJaxbContext().createMarshaller().marshal(engine.getWsadlApplication(), response.getWriter());
                response.setStatus(200);
                response.setContentType(ContentType.newContentType("application/wsadl+xml"));

                return;
            }

            if (staticHttpHandler != null) {
                staticHttpHandler.service(request, response);
            } else {
                response.sendError(404);
            }
        }
    }
}
