/*
 * Copyright (c) 2014, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.container.jdk.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;

import javax.net.ssl.HostnameVerifier;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslContextConfigurator;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.glassfish.tyrus.client.ThreadPoolConfig;
import org.glassfish.tyrus.core.ReflectionHelper;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.spi.ClientContainer;
import org.glassfish.tyrus.spi.ClientEngine;
import org.glassfish.tyrus.spi.CompletionHandler;
import org.glassfish.tyrus.spi.UpgradeRequest;

/**
 * {@link org.glassfish.tyrus.spi.ClientContainer} implementation based on Java 7 NIO API.
 *
 * @author Petr Janouch
 */
public class JdkClientContainer implements ClientContainer {

    /**
     * Input buffer that is used by {@link org.glassfish.tyrus.container.jdk.client.TransportFilter} when SSL is turned
     * on. The size cannot be smaller than a maximal size of a SSL packet, which is 16kB for payload + header, because
     * {@link org.glassfish.tyrus.container.jdk.client.SslFilter} does not have its own buffer for buffering incoming
     * {@link org.glassfish.tyrus.container.jdk.client.SslFilter} does not have its own buffer for buffering incoming
     * data and therefore the entire SSL packet must fit into
     * {@link org.glassfish.tyrus.container.jdk.client.TransportFilter}
     * input buffer.
     */
    private static final int SSL_INPUT_BUFFER_SIZE = 17_000;
    /**
     * Input buffer that is used by {@link org.glassfish.tyrus.container.jdk.client.TransportFilter} when SSL is not
     * turned on.
     */
    private static final int INPUT_BUFFER_SIZE = 2048;
    private static final Logger LOGGER = Logger.getLogger(JdkClientContainer.class.getName());

    @Override
    public void openClientSocket(final ClientEndpointConfig cec, final Map<String, Object> properties,
                                 final ClientEngine clientEngine) throws DeploymentException, IOException {


        ThreadPoolConfig threadPoolConfig =
                Utils.getProperty(properties, ClientProperties.WORKER_THREAD_POOL_CONFIG, ThreadPoolConfig.class);
        if (threadPoolConfig == null) {
            threadPoolConfig = ThreadPoolConfig.defaultConfig();
        }
        // weblogic.websocket.client.max-aio-threads has priority over what is in thread pool config
        String wlsMaxThreadsStr = System.getProperty(ClientManager.WLS_MAX_THREADS);
        if (wlsMaxThreadsStr != null) {
            try {
                int wlsMaxThreads = Integer.parseInt(wlsMaxThreadsStr);
                threadPoolConfig.setMaxPoolSize(wlsMaxThreads);
            } catch (Exception e) {
                LOGGER.log(Level.CONFIG,
                           String.format("Invalid type of configuration property of %s , %s cannot be cast to Integer",
                                         ClientManager.WLS_MAX_THREADS, wlsMaxThreadsStr));
            }
        }

        final Integer containerIdleTimeout =
                Utils.getProperty(properties, ClientProperties.SHARED_CONTAINER_IDLE_TIMEOUT, Integer.class);

        final InetAddress bindingAddress =
                Utils.getProperty(properties, ClientProperties.SOCKET_BINDING, InetAddress.class);

        final ThreadPoolConfig finalThreadPoolConfig = threadPoolConfig;
        final Callable<Void> jdkConnector = new Callable<Void>() {

            @Override
            public Void call() throws DeploymentException {

                final TaskQueueFilter writeQueue;

                TimeoutHandlerProxy timeoutHandlerProxy = new TimeoutHandlerProxy();

                final UpgradeRequest upgradeRequest = clientEngine.createUpgradeRequest(timeoutHandlerProxy);

                final URI uri = upgradeRequest.getRequestURI();

                List<Proxy> proxies = processProxy(properties, uri);
                final boolean secure = "wss".equalsIgnoreCase(uri.getScheme());

                if (secure) {
                    TransportFilter transportFilter =
                            createTransportFilter(SSL_INPUT_BUFFER_SIZE,
                                                  finalThreadPoolConfig,
                                                  containerIdleTimeout,
                                                  bindingAddress);
                    SslFilter sslFilter = createSslFilter(cec, properties, transportFilter, uri);
                    writeQueue = createTaskQueueFilter(sslFilter);

                } else {
                    TransportFilter transportFilter =
                            createTransportFilter(INPUT_BUFFER_SIZE,
                                                  finalThreadPoolConfig,
                                                  containerIdleTimeout,
                                                  bindingAddress);
                    writeQueue = createTaskQueueFilter(transportFilter);
                }

                final ClientFilter clientFilter =
                        createClientFilter(properties, writeQueue, clientEngine, this, upgradeRequest);
                timeoutHandlerProxy.setHandler(new ClientEngine.TimeoutHandler() {
                    @Override
                    public void handleTimeout() {
                        writeQueue.close();
                    }
                });

                Throwable exception = null;

                for (Proxy proxy : proxies) {
                    if (proxy.type() == Proxy.Type.DIRECT) {
                        SocketAddress serverAddress = getServerAddress(uri);
                        try {
                            connectSynchronously(clientFilter, serverAddress, false);
                            // connected.
                            return null;
                        } catch (Throwable throwable) {
                            exception = throwable;
                        }
                    }

                    LOGGER.log(Level.CONFIG, String.format("Connecting to '%s' via proxy '%s'.", uri, proxy));
                    // default ProxySelector always returns proxies with unresolved addresses.
                    SocketAddress proxyAddress = proxy.address();
                    if (proxyAddress instanceof InetSocketAddress) {
                        InetSocketAddress inetSocketAddress = (InetSocketAddress) proxyAddress;
                        if (inetSocketAddress.isUnresolved()) {
                            // resolve the address.
                            proxyAddress =
                                    new InetSocketAddress(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
                        }

                        try {
                            connectSynchronously(clientFilter, proxyAddress, true);
                            // connected.
                            return null;
                        } catch (Throwable t) {
                            LOGGER.log(Level.FINE, "Connecting to " + proxyAddress + " failed", t);
                            clientFilter.close();
                            exception = t;
                        }
                    }
                }

                throw new DeploymentException("Connection failed.", exception);
            }
        };

        try {
            jdkConnector.call();
        } catch (Exception e) {
            if (e instanceof DeploymentException) {
                throw (DeploymentException) e;
            }

            if (e instanceof IOException) {
                throw (IOException) e;
            }

            throw new DeploymentException(e.getMessage(), e);
        }
    }

    private SslFilter createSslFilter(ClientEndpointConfig cec, Map<String, Object> properties,
                                      TransportFilter transportFilter, URI uri) {
        Object sslEngineConfiguratorObject = properties.get(ClientProperties.SSL_ENGINE_CONFIGURATOR);

        SslFilter sslFilter = null;

        if (sslEngineConfiguratorObject != null) {
            // property is set, we need to figure out whether new or deprecated one is used and act accordingly.
            if (sslEngineConfiguratorObject instanceof SslEngineConfigurator) {
                sslFilter = new SslFilter(transportFilter, (SslEngineConfigurator) sslEngineConfiguratorObject,
                                          uri.getHost());
            } else if (sslEngineConfiguratorObject instanceof org.glassfish.tyrus.container.jdk.client
                    .SslEngineConfigurator) {
                sslFilter = new SslFilter(transportFilter,
                                          (org.glassfish.tyrus.container.jdk.client.SslEngineConfigurator)
                                                  sslEngineConfiguratorObject);
            } else {
                LOGGER.log(Level.WARNING, "Invalid '" + ClientProperties.SSL_ENGINE_CONFIGURATOR + "' property value: "
                        + sslEngineConfiguratorObject + ". Using system defaults.");
            }
        }

        // if we are trying to access "wss" scheme and we don't have sslEngineConfigurator instance
        // we should try to create ssl connection using JVM properties.
        if (sslFilter == null) {
            SslContextConfigurator defaultConfig = new SslContextConfigurator();
            defaultConfig.retrieve(System.getProperties());

            String wlsSslTrustStore = (String) cec.getUserProperties().get(ClientManager.WLS_SSL_TRUSTSTORE_PROPERTY);
            String wlsSslTrustStorePassword =
                    (String) cec.getUserProperties().get(ClientManager.WLS_SSL_TRUSTSTORE_PWD_PROPERTY);

            if (wlsSslTrustStore != null) {
                defaultConfig.setTrustStoreFile(wlsSslTrustStore);

                if (wlsSslTrustStorePassword != null) {
                    defaultConfig.setTrustStorePassword(wlsSslTrustStorePassword);
                }
            }

            // client mode = true, needClientAuth = false, wantClientAuth = false
            SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(defaultConfig, true, false, false);
            String wlsSslProtocols = (String) cec.getUserProperties().get(ClientManager.WLS_SSL_PROTOCOLS_PROPERTY);
            if (wlsSslProtocols != null) {
                sslEngineConfigurator.setEnabledProtocols(wlsSslProtocols.split(","));
            }

            // {@value ClientManager.WLS_IGNORE_HOSTNAME_VERIFICATION} system property
            String wlsIgnoreHostnameVerification =
                    System.getProperties().getProperty(ClientManager.WLS_IGNORE_HOSTNAME_VERIFICATION);
            if ("true".equalsIgnoreCase(wlsIgnoreHostnameVerification)) {
                sslEngineConfigurator.setHostVerificationEnabled(false);
            } else {

                // if hostname verification is not ignored, Tyrus looks for {@value ClientManager
                // .WLS_HOSTNAME_VERIFIER_CLASS}.
                // If that is found and the class can be instantiated properly, it will be used as Hostname
                // Verifier instance; if not, we will use the default one.
                final String className = System.getProperties().getProperty(ClientManager.WLS_HOSTNAME_VERIFIER_CLASS);
                if (className != null && !className.isEmpty()) {
                    //noinspection unchecked
                    final Class<HostnameVerifier> hostnameVerifierClass =
                            (Class<HostnameVerifier>) ReflectionHelper.classForName(className);
                    if (hostnameVerifierClass != null) {
                        try {
                            final HostnameVerifier hostnameVerifier =
                                    ReflectionHelper.getInstance(hostnameVerifierClass);
                            sslEngineConfigurator.setHostnameVerifier(hostnameVerifier);
                        } catch (IllegalAccessException | InstantiationException e) {
                            LOGGER.log(Level.INFO,
                                       String.format("Cannot instantiate class set as a value of '%s' property: %s",
                                                     ClientManager.WLS_HOSTNAME_VERIFIER_CLASS, className), e);
                        }
                    }
                }

            }

            sslFilter = new SslFilter(transportFilter, sslEngineConfigurator, uri.getHost());
        }
        return sslFilter;
    }

    private TransportFilter createTransportFilter(int sslInputBufferSize,
                                                  ThreadPoolConfig threadPoolConfig,
                                                  Integer containerIdleTimeout,
                                                  InetAddress bindingAddress) {
        return new TransportFilter(sslInputBufferSize, threadPoolConfig, containerIdleTimeout, bindingAddress);
    }

    private TaskQueueFilter createTaskQueueFilter(Filter downstreamFilter) {
        return new TaskQueueFilter(downstreamFilter);
    }

    private ClientFilter createClientFilter(Map<String, Object> properties, Filter downstreamFilter,
                                            ClientEngine clientEngine, Callable<Void> jdkConnector,
                                            UpgradeRequest upgradeRequest) throws DeploymentException {
        return new ClientFilter(downstreamFilter, clientEngine, properties, jdkConnector, upgradeRequest);
    }

    private SocketAddress getServerAddress(URI uri) throws DeploymentException {
        int port = Utils.getWsPort(uri);

        try {
            return new InetSocketAddress(uri.getHost(), port);
        } catch (IllegalArgumentException e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }

    /**
     * {@link org.glassfish.tyrus.container.jdk.client.ClientFilter#connect(java.net.SocketAddress, boolean,
     * CompletionHandler)} is asynchronous, this method will block until it either succeeds or fails.
     */
    private void connectSynchronously(ClientFilter clientFilter, final SocketAddress address, boolean proxy) throws
            Throwable {
        final AtomicReference<Throwable> exception = new AtomicReference<>(null);
        final CountDownLatch connectLatch = new CountDownLatch(1);

        try {
            clientFilter.connect(address, proxy, new CompletionHandler<Void>() {
                @Override
                public void completed(Void result) {
                    connectLatch.countDown();
                }

                @Override
                public void failed(Throwable exc) {
                    exception.set(exc);
                    connectLatch.countDown();
                }
            });

            connectLatch.await();

            Throwable throwable = exception.get();
            if (throwable != null) {
                throw throwable;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DeploymentException(
                    "The thread waiting for client to connect has been interrupted before the connection has finished",
                    e);
        }
    }

    private List<Proxy> processProxy(Map<String, Object> properties, URI uri) throws DeploymentException {
        final List<Proxy> proxies = new ArrayList<>();
        String wlsProxyHost = null;
        Integer wlsProxyPort = null;

        Object value = properties.get(ClientManager.WLS_PROXY_HOST);
        if (value != null) {
            if (value instanceof String) {
                wlsProxyHost = (String) value;
            } else {
                throw new DeploymentException(ClientManager.WLS_PROXY_HOST + " only accept String values.");
            }
        }

        value = properties.get(ClientManager.WLS_PROXY_PORT);
        if (value != null) {
            if (value instanceof Integer) {
                wlsProxyPort = (Integer) value;
            } else {
                throw new DeploymentException(ClientManager.WLS_PROXY_PORT + " only accept Integer values.");
            }
        }

        if (wlsProxyHost != null) {
            proxies.add(new Proxy(Proxy.Type.HTTP,
                                  new InetSocketAddress(wlsProxyHost, wlsProxyPort == null ? 80 : wlsProxyPort)));
        } else {
            Object proxyString = properties.get(ClientProperties.PROXY_URI);
            try {
                URI proxyUri;
                if (proxyString != null) {
                    proxyUri = new URI(proxyString.toString());
                    if (proxyUri.getHost() == null) {
                        LOGGER.log(Level.WARNING, String.format("Invalid proxy '%s'.", proxyString));
                    } else {
                        // proxy set via properties
                        int proxyPort = proxyUri.getPort() == -1 ? 80 : proxyUri.getPort();
                        proxies.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUri.getHost(), proxyPort)));
                    }
                }
            } catch (URISyntaxException e) {
                LOGGER.log(Level.WARNING, String.format("Invalid proxy '%s'.", proxyString), e);
            }
        }

        // ProxySelector
        final ProxySelector proxySelector = ProxySelector.getDefault();

        if (proxySelector != null) {
            // see WebSocket Protocol RFC, chapter 4.1.3: http://tools.ietf.org/html/rfc6455#section-4.1
            addProxies(proxySelector, uri, "socket", proxies);
            addProxies(proxySelector, uri, "https", proxies);
            addProxies(proxySelector, uri, "http", proxies);
        }

        if (proxies.isEmpty()) {
            proxies.add(Proxy.NO_PROXY);
        }

        return proxies;
    }

    private void addProxies(ProxySelector proxySelector, URI uri, String scheme, List<Proxy> proxies) {
        for (Proxy p : proxySelector.select(getProxyUri(uri, scheme))) {
            switch (p.type()) {
                case HTTP:
                    LOGGER.log(Level.FINE, String.format("Found proxy: '%s'", p));
                    proxies.add(p);
                    break;
                case SOCKS:
                    LOGGER.log(Level.INFO, String.format(
                            "Socks proxy is not supported, please file new issue at https://java"
                                    + ".net/jira/browse/TYRUS. Proxy '%s' will be ignored.",
                            p));
                    break;
                default:
                    break;
            }
        }
    }

    private URI getProxyUri(URI wsUri, String scheme) {
        try {
            return new URI(scheme, wsUri.getUserInfo(), wsUri.getHost(), wsUri.getPort(), wsUri.getPath(),
                           wsUri.getQuery(), wsUri.getFragment());
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, String.format("Exception during generating proxy URI '%s'", wsUri), e);
            return wsUri;
        }
    }

    private static class TimeoutHandlerProxy implements ClientEngine.TimeoutHandler {

        private volatile ClientEngine.TimeoutHandler handler;

        @Override
        public void handleTimeout() {
            if (handler != null) {
                handler.handleTimeout();
            }
        }

        public void setHandler(ClientEngine.TimeoutHandler handler) {
            this.handler = handler;
        }
    }
}
