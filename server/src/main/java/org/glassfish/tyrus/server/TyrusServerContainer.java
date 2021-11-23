/*
 * Copyright (c) 2012, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.server;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.BaseContainer;
import org.glassfish.tyrus.core.ErrorCollector;
import org.glassfish.tyrus.spi.ServerContainer;

/**
 * Server Container Implementation.
 *
 * @author Martin Matula
 * @author Pavel Bucek
 * @author Stepan Kopriva
 */
public abstract class TyrusServerContainer extends BaseContainer implements ServerContainer {
    private final ErrorCollector collector;

    private final Set<Class<?>> dynamicallyAddedClasses;
    private final Set<ServerEndpointConfig> dynamicallyAddedEndpointConfigs;
    private final Set<Class<?>> classes;
    private final ServerApplicationConfig serverApplicationConfig;

    private boolean notYetStarted = true;
    private long defaultMaxSessionIdleTimeout = 0;
    private long defaultAsyncSendTimeout = 0;
    private int maxTextMessageBufferSize = Integer.MAX_VALUE;
    private int maxBinaryMessageBufferSize = Integer.MAX_VALUE;

    private ClientManager clientManager = null;

    private volatile int port = -1;

    /**
     * Create new {@link TyrusServerContainer}.
     *
     * @param classes classes to be included in this application instance. Can contain any combination of annotated
     *                endpoints (see {@link jakarta.websocket.server.ServerEndpoint}) or {@link jakarta.websocket.Endpoint}
     *                descendants.
     */
    public TyrusServerContainer(Set<Class<?>> classes) {
        this.collector = new ErrorCollector();
        this.classes = classes == null ? Collections.<Class<?>>emptySet() : new HashSet<Class<?>>(classes);
        this.dynamicallyAddedClasses = new HashSet<Class<?>>();
        this.dynamicallyAddedEndpointConfigs = new HashSet<ServerEndpointConfig>();
        this.serverApplicationConfig = null;
    }

    /**
     * Create new {@link TyrusServerContainer} using already created {@link ServerApplicationConfig} instance.
     *
     * @param serverApplicationConfig provided application config.
     */
    public TyrusServerContainer(ServerApplicationConfig serverApplicationConfig) {
        this.collector = new ErrorCollector();
        this.classes = new HashSet<Class<?>>();
        this.dynamicallyAddedClasses = new HashSet<Class<?>>();
        this.dynamicallyAddedEndpointConfigs = new HashSet<ServerEndpointConfig>();
        this.serverApplicationConfig = serverApplicationConfig;
    }

    /**
     * Start the container.
     *
     * @param rootPath context path of the deployed websocket application.
     * @throws IOException         when any IO related issues emerge during {@link
     *                             org.glassfish.tyrus.spi.ServerContainer#start(String, int)}.
     * @throws DeploymentException when any deployment related error is found; should contain list of all found issues.
     */
    @Override
    public void start(String rootPath, int port) throws IOException, DeploymentException {
        ServerApplicationConfig configuration =
                new TyrusServerConfiguration(classes, dynamicallyAddedClasses, dynamicallyAddedEndpointConfigs,
                                             this.collector);

        // start the underlying server
        try {
            // deploy all the annotated endpoints
            for (Class<?> endpointClass : configuration.getAnnotatedEndpointClasses(null)) {
                register(endpointClass);
            }

            // deploy all the programmatic endpoints
            for (ServerEndpointConfig serverEndpointConfiguration : configuration.getEndpointConfigs(null)) {
                if (serverEndpointConfiguration != null) {
                    register(serverEndpointConfiguration);
                }
            }

            if (serverApplicationConfig != null) {
                // deploy all the annotated endpoints
                for (Class<?> endpointClass : serverApplicationConfig.getAnnotatedEndpointClasses(null)) {
                    register(endpointClass);
                }

                // deploy all the programmatic endpoints
                for (ServerEndpointConfig serverEndpointConfiguration : serverApplicationConfig
                        .getEndpointConfigs(null)) {
                    if (serverEndpointConfiguration != null) {
                        register(serverEndpointConfiguration);
                    }
                }
            }
        } catch (DeploymentException de) {
            collector.addException(de);
        }

        if (!collector.isEmpty()) {
            this.stop();
            throw collector.composeComprehensiveException();
        }

        if (this.port == -1) {
            this.port = port;
        }
    }

    /**
     * Undeploy all endpoints and stop underlying {@link org.glassfish.tyrus.spi.ServerContainer}.
     * <p>
     * Release all created threadpools / executor services.
     */
    @Override
    public void stop() {

        // release executor services managed by {@link BaseContainer}.
        shutdown();
    }

    public abstract void register(Class<?> endpointClass) throws DeploymentException;

    public abstract void register(ServerEndpointConfig serverEndpointConfig) throws DeploymentException;

    @Override
    public void addEndpoint(Class<?> endpointClass) throws DeploymentException {
        dynamicallyAddedClasses.add(endpointClass);
        if (!notYetStarted) {
            register(endpointClass);
        }
    }

    @Override
    public void addEndpoint(ServerEndpointConfig serverEndpointConfig) throws DeploymentException {
        dynamicallyAddedEndpointConfigs.add(serverEndpointConfig);
        if (!notYetStarted) {
            register(serverEndpointConfig);
        }
    }

    /**
     * Get port of the started container.
     *
     * @return the port of the started container or {@code -1}, when the container is not started or the container does
     * not provide the port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Can be overridden to provide own {@link ClientManager} implementation or instance.
     *
     * @return {@link ClientManager} associated with this server container.
     */
    protected synchronized ClientManager getClientManager() {
        if (clientManager == null) {
            clientManager = ClientManager.createClient(this);
        }

        return clientManager;
    }

    @Override
    public Session connectToServer(Class annotatedEndpointClass, URI path) throws DeploymentException, IOException {
        return getClientManager().connectToServer(annotatedEndpointClass, path);
    }

    @Override
    public Session connectToServer(Class<? extends Endpoint> endpointClass, ClientEndpointConfig cec, URI path) throws
            DeploymentException, IOException {
        return getClientManager().connectToServer(endpointClass, cec, path);
    }

    @Override
    public Session connectToServer(Object annotatedEndpointInstance, URI path) throws DeploymentException, IOException {
        return getClientManager().connectToServer(annotatedEndpointInstance, path);
    }

    @Override
    public Session connectToServer(Endpoint endpointInstance, ClientEndpointConfig cec, URI path) throws
            DeploymentException, IOException {
        return getClientManager().connectToServer(endpointInstance, cec, path);
    }

    /**
     * Non-blocking version of {@link jakarta.websocket.WebSocketContainer#connectToServer(Class, java.net.URI)}.
     * <p>
     * Only simple checks are performed in the main thread; client container is created in different thread, same
     * applies to connecting etc.
     *
     * @param annotatedEndpointClass the annotated websocket client endpoint.
     * @param path                   the complete path to the server endpoint.
     * @return Future for the Session created if the connection is successful.
     * @throws DeploymentException if the class is not a valid annotated endpoint class.
     */
    public Future<Session> asyncConnectToServer(Class<?> annotatedEndpointClass, URI path) throws DeploymentException {
        return getClientManager().asyncConnectToServer(annotatedEndpointClass, path);
    }

    /**
     * Non-blocking version of {@link jakarta.websocket.WebSocketContainer#connectToServer(Class,
     * jakarta.websocket.ClientEndpointConfig, java.net.URI)}.
     * <p>
     * Only simple checks are performed in the main thread; client container is created in different thread, same
     * applies to connecting etc.
     *
     * @param endpointClass the programmatic client endpoint class {@link Endpoint}.
     * @param path          the complete path to the server endpoint.
     * @param cec           the configuration used to configure the programmatic endpoint.
     * @return the Session created if the connection is successful.
     * @throws DeploymentException if the configuration is not valid
     * @see jakarta.websocket.WebSocketContainer#connectToServer(Class, jakarta.websocket.ClientEndpointConfig,
     * java.net.URI)
     */
    public Future<Session> asyncConnectToServer(Class<? extends Endpoint> endpointClass, ClientEndpointConfig cec,
                                                URI path) throws DeploymentException {
        return getClientManager().asyncConnectToServer(endpointClass, cec, path);
    }

    /**
     * Non-blocking version of {@link jakarta.websocket.WebSocketContainer#connectToServer(jakarta.websocket.Endpoint,
     * jakarta.websocket.ClientEndpointConfig, java.net.URI)}.
     * <p>
     * Only simple checks are performed in the main thread; client container is created in different thread, same
     * applies to connecting etc.
     *
     * @param endpointInstance the programmatic client endpoint instance {@link Endpoint}.
     * @param path             the complete path to the server endpoint.
     * @param cec              the configuration used to configure the programmatic endpoint.
     * @return the Session created if the connection is successful.
     * @throws DeploymentException if the configuration is not valid
     * @see jakarta.websocket.WebSocketContainer#connectToServer(jakarta.websocket.Endpoint,
     * jakarta.websocket.ClientEndpointConfig, java.net.URI)
     */
    public Future<Session> asyncConnectToServer(Endpoint endpointInstance, ClientEndpointConfig cec, URI path) throws
            DeploymentException {
        return getClientManager().asyncConnectToServer(endpointInstance, cec, path);
    }

    /**
     * Non-blocking version of {@link jakarta.websocket.WebSocketContainer#connectToServer(Object, java.net.URI)}.
     * <p>
     * Only simple checks are performed in the main thread; client container is created in different thread, same
     * applies to connecting etc.
     *
     * @param obj  the annotated websocket client endpoint instance.
     * @param path the complete path to the server endpoint.
     * @return the Session created if the connection is successful.
     * @throws DeploymentException if the annotated endpoint instance is not valid.
     * @see jakarta.websocket.WebSocketContainer#connectToServer(Object, java.net.URI)
     */
    public Future<Session> asyncConnectToServer(Object obj, URI path) throws DeploymentException {
        return getClientManager().asyncConnectToServer(obj, path);
    }

    @Override
    public int getDefaultMaxBinaryMessageBufferSize() {
        return maxBinaryMessageBufferSize;
    }

    @Override
    public void setDefaultMaxBinaryMessageBufferSize(int max) {
        this.maxBinaryMessageBufferSize = max;
    }

    @Override
    public int getDefaultMaxTextMessageBufferSize() {
        return maxTextMessageBufferSize;
    }

    @Override
    public void setDefaultMaxTextMessageBufferSize(int max) {
        this.maxTextMessageBufferSize = max;
    }

    @Override
    public Set<Extension> getInstalledExtensions() {
        return Collections.emptySet();
    }

    @Override
    public long getDefaultAsyncSendTimeout() {
        return defaultAsyncSendTimeout;
    }

    @Override
    public void setAsyncSendTimeout(long timeoutmillis) {
        defaultAsyncSendTimeout = timeoutmillis;
    }

    @Override
    public long getDefaultMaxSessionIdleTimeout() {
        return defaultMaxSessionIdleTimeout;
    }

    @Override
    public void setDefaultMaxSessionIdleTimeout(long defaultMaxSessionIdleTimeout) {
        this.defaultMaxSessionIdleTimeout = defaultMaxSessionIdleTimeout;
    }

    /**
     * Container is no longer required to accept {@link #addEndpoint(jakarta.websocket.server.ServerEndpointConfig)} and
     * {@link #addEndpoint(Class)} calls.
     */
    public void doneDeployment() {
        notYetStarted = false;
    }

    // @Override in 2.1
    public void upgradeHttpToWebSocket(Object httpServletRequest, Object httpServletResponse, ServerEndpointConfig sec,
                                       Map<String, String> pathParameters) throws IOException, DeploymentException {
        throw new UnsupportedOperationException();
    }
}
