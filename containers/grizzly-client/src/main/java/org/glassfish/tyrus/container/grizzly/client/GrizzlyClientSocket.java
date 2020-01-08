/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.container.grizzly.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.DeploymentException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.glassfish.tyrus.core.TyrusFuture;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.spi.ClientEngine;
import org.glassfish.tyrus.spi.UpgradeRequest;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.Processor;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.FilterChainEvent;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.ssl.SSLConnectionContext;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

/**
 * Implementation of the WebSocket interface.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class GrizzlyClientSocket {

    /**
     * Client-side user property to set proxy URI.
     * <p>
     * Value is expected to be {@link String} and represent proxy URI. Protocol part is currently ignored
     * but must be present ({@link URI#URI(String)} is used for parsing).
     * <pre>
     *     client.getProperties().put(GrizzlyClientSocket.PROXY_URI, "http://my.proxy.com:80");
     *     client.connectToServer(...);
     * </pre>
     *
     * @see javax.websocket.ClientEndpointConfig#getUserProperties()
     * @deprecated please use {@link org.glassfish.tyrus.client.ClientProperties#PROXY_URI}.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static final String PROXY_URI = ClientProperties.PROXY_URI;

    /**
     * Client-side user property to set additional proxy headers.
     * <p>
     * Value is expected to be {@link Map}&lt;{@link String}, {@link String}&gt; and represent raw http headers
     * to be added to initial request which is sent to proxy. Key corresponds to header name, value is header
     * value.
     * <p>
     * Sample below demonstrates use of this feature to set preemptive basic proxy authentication:
     * <pre>
     *     final HashMap&lt;String, String&gt; proxyHeaders = new HashMap&lt;String, String&gt;();
     *     proxyHeaders.put("Proxy-Authorization", "Basic " +
     *         Base64.getEncoder().encodeToString("username:password".getBytes(Charset.forName("UTF-8"))));
     *
     *     client.getProperties().put(GrizzlyClientSocket.PROXY_HEADERS, proxyHeaders);
     *     client.connectToServer(...);
     * </pre>
     * Please note that these headers will be used only when establishing proxy connection, for modifying WebSocket
     * handshake headers, see {@link javax.websocket.ClientEndpointConfig.Configurator#beforeRequest(java.util.Map)}.
     *
     * @see javax.websocket.ClientEndpointConfig#getUserProperties()
     * @deprecated please use {@link org.glassfish.tyrus.client.ClientProperties#PROXY_HEADERS}
     */
    @SuppressWarnings("UnusedDeclaration")
    public static final String PROXY_HEADERS = ClientProperties.PROXY_HEADERS;

    /**
     * Client-side property to set custom worker {@link ThreadPoolConfig}.
     * <p>
     * Value is expected to be instance of {@link ThreadPoolConfig}, can be {@code null} (it won't be used).
     *
     * @deprecated please use {@link org.glassfish.tyrus.container.grizzly.client
     * .GrizzlyClientProperties#WORKER_THREAD_POOL_CONFIG}.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static final String WORKER_THREAD_POOL_CONFIG = GrizzlyClientProperties.WORKER_THREAD_POOL_CONFIG;

    /**
     * Client-side property to set custom selector {@link ThreadPoolConfig}.
     * <p>
     * Value is expected to be instance of {@link ThreadPoolConfig}, can be {@code null} (it won't be used).
     *
     * @deprecated please use {@link org.glassfish.tyrus.container.grizzly.client
     * .GrizzlyClientProperties#SELECTOR_THREAD_POOL_CONFIG}.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static final String SELECTOR_THREAD_POOL_CONFIG = GrizzlyClientProperties.SELECTOR_THREAD_POOL_CONFIG;

    private static final Logger LOGGER = Logger.getLogger(GrizzlyClientSocket.class.getName());

    private final List<Proxy> proxies = new ArrayList<Proxy>();

    private final long timeoutMs;
    private final ThreadPoolConfig workerThreadPoolConfig;
    private final ThreadPoolConfig selectorThreadPoolConfig;
    private final ClientEngine clientEngine;
    private final boolean sharedTransport;
    private final Integer sharedTransportTimeout;
    private final Map<String, String> proxyHeaders;
    private final Map<String, Object> properties;

    private static volatile TCPNIOTransport transport;
    private static final Object TRANSPORT_LOCK = new Object();
    private final Callable<Void> grizzlyConnector;

    private volatile TCPNIOTransport privateTransport;

    /**
     * Create new instance.
     *
     * @param timeoutMs    TODO
     * @param clientEngine engine used for this websocket communication
     * @param properties   properties map. Cannot be {@code null}.
     */
    GrizzlyClientSocket(long timeoutMs,
                        ClientEngine clientEngine,
                        Map<String, Object> properties) throws DeploymentException {
        this.timeoutMs = timeoutMs;
        this.properties = properties;
        this.proxyHeaders = getProxyHeaders(properties);

        try {
            this.workerThreadPoolConfig = getWorkerThreadPoolConfig(properties);
            this.selectorThreadPoolConfig =
                    Utils.getProperty(properties, GrizzlyClientProperties.SELECTOR_THREAD_POOL_CONFIG,
                                      ThreadPoolConfig.class);

            Boolean shared = Utils.getProperty(properties, ClientProperties.SHARED_CONTAINER, Boolean.class);
            if (shared == null || !shared) {
                // TODO introduce some better (generic) way how to configure client from system properties.
                final String property = System.getProperty(ClientProperties.SHARED_CONTAINER);
                if (property != null && property.equals("true")) {
                    shared = true;
                }
            }
            sharedTransport = (shared == null ? false : shared);
            if (sharedTransport) {
                GrizzlyTransportTimeoutFilter.touch();
            }

            final Integer sharedTransportTimeoutProperty =
                    Utils.getProperty(properties, ClientProperties.SHARED_CONTAINER_IDLE_TIMEOUT, Integer.class);
            // default value for shared transport timeout is 30.
            sharedTransportTimeout =
                    (sharedTransport && sharedTransportTimeoutProperty != null) ? sharedTransportTimeoutProperty : 30;
            this.clientEngine = clientEngine;
        } catch (RuntimeException e) {
            throw new DeploymentException(e.getMessage(), e);
        }

        grizzlyConnector = new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                GrizzlyClientSocket.this._connect();
                return null;
            }
        };
    }

    /**
     * Performs connect to server endpoint.
     *
     * @throws DeploymentException when there the server endpoint cannot be reached.
     * @throws IOException         when transport fails to start.
     */
    public void connect() throws DeploymentException, IOException {
        try {
            grizzlyConnector.call();
        } catch (DeploymentException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }

    private ThreadPoolConfig getWorkerThreadPoolConfig(Map<String, Object> properties) {
        if (properties.containsKey(GrizzlyClientProperties.WORKER_THREAD_POOL_CONFIG)) {
            return Utils
                    .getProperty(properties, GrizzlyClientProperties.WORKER_THREAD_POOL_CONFIG, ThreadPoolConfig.class);
        } else if (properties.containsKey(ClientProperties.WORKER_THREAD_POOL_CONFIG)) {
            Object threadPoolConfig =
                    Utils.getProperty(properties, ClientProperties.WORKER_THREAD_POOL_CONFIG, Object.class);

            if (threadPoolConfig instanceof org.glassfish.tyrus.client.ThreadPoolConfig) {
                org.glassfish.tyrus.client.ThreadPoolConfig clientThreadPoolConfig =
                        (org.glassfish.tyrus.client.ThreadPoolConfig) threadPoolConfig;
                ThreadPoolConfig grizzlyThreadPoolConfig = ThreadPoolConfig.defaultConfig();
                grizzlyThreadPoolConfig.setMaxPoolSize(clientThreadPoolConfig.getMaxPoolSize())
                                       .setCorePoolSize(clientThreadPoolConfig.getCorePoolSize())
                                       .setPriority(clientThreadPoolConfig.getPriority())
                                       .setDaemon(clientThreadPoolConfig.isDaemon())
                                       .setKeepAliveTime(clientThreadPoolConfig.getKeepAliveTime(TimeUnit.MILLISECONDS),
                                                         TimeUnit.MILLISECONDS)
                                       .setInitialClassLoader(clientThreadPoolConfig.getInitialClassLoader())
                                       .setPoolName(clientThreadPoolConfig.getPoolName())
                                       .setQueue(clientThreadPoolConfig.getQueue())
                                       .setQueueLimit(clientThreadPoolConfig.getQueueLimit())
                                       .setThreadFactory(clientThreadPoolConfig.getThreadFactory());
                return grizzlyThreadPoolConfig;
            } else if (threadPoolConfig instanceof ThreadPoolConfig) {
                return (ThreadPoolConfig) threadPoolConfig;
            } else {
                LOGGER.log(Level.CONFIG, String.format(
                        "Invalid type of configuration property of %s (%s), %s cannot be cast to %s or %s",
                        ClientProperties.WORKER_THREAD_POOL_CONFIG, threadPoolConfig.toString(),
                        threadPoolConfig.getClass().toString(),
                        ThreadPoolConfig.class.toString(),
                        org.glassfish.tyrus.client.ThreadPoolConfig.class.toString()));
            }
        }
        return null;
    }

    /**
     * Connects to the given {@link URI}.
     */
    private void _connect() throws IOException, DeploymentException {
        // TCPNIOTransport privateTransport = null;

        try {
            if (sharedTransport) {
                transport = getOrCreateSharedTransport(workerThreadPoolConfig, selectorThreadPoolConfig);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Transport failed to start.", e);
            synchronized (TRANSPORT_LOCK) {
                transport = null;
            }
            throw e;
        }


        final ClientEngine.TimeoutHandler timeoutHandler = sharedTransport ? null : new ClientEngine.TimeoutHandler() {
            @Override
            public void handleTimeout() {
                closeTransport(privateTransport);
            }
        };

        final UpgradeRequest upgradeRequest = clientEngine.createUpgradeRequest(timeoutHandler);
        URI requestURI = upgradeRequest.getRequestURI();

        SocketAddress socketAddress = processProxy(requestURI, properties);
        Throwable exception = null;

        for (Proxy proxy : proxies) {
            if (!sharedTransport) {
                privateTransport = createTransport(workerThreadPoolConfig, selectorThreadPoolConfig);
                privateTransport.start();
            }

            final TCPNIOConnectorHandler connectorHandler =
                    new TCPNIOConnectorHandler(sharedTransport ? transport : privateTransport) {
                    };

            connectorHandler.setSyncConnectTimeout(timeoutMs, TimeUnit.MILLISECONDS);

            GrizzlyFuture<Connection> connectionGrizzlyFuture;

            SocketAddress connectAddress;
            switch (proxy.type()) {
                case DIRECT:
                    try {
                        connectAddress = new InetSocketAddress(requestURI.getHost(), Utils.getWsPort(requestURI));
                    } catch (IllegalArgumentException e) {
                        closeTransport(privateTransport);
                        throw new DeploymentException(e.getMessage(), e);
                    }

                    LOGGER.log(Level.CONFIG, String.format("Connecting to '%s' (no proxy).", requestURI));
                    break;
                default:
                    LOGGER.log(Level.CONFIG, String.format("Connecting to '%s' via proxy '%s'.", requestURI, proxy));

                    // default ProxySelector always returns proxies with unresolved addresses.
                    SocketAddress address = proxy.address();
                    if (address instanceof InetSocketAddress) {
                        InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
                        if (inetSocketAddress.isUnresolved()) {
                            // resolve the address.
                            address =
                                    new InetSocketAddress(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
                        }
                    }
                    connectAddress = address;
                    break;
            }

            // this will block until the SSL engine handshake is complete, so SSL handshake error can be handled here
            TyrusFuture sslHandshakeFuture = null;
            ExtendedSSLEngineConfigurator clientSSLEngineConfigurator =
                    getSSLEngineConfigurator(requestURI, properties);
            if (clientSSLEngineConfigurator != null) {
                sslHandshakeFuture = new TyrusFuture();
            }

            connectorHandler.setProcessor(
                    createFilterChain(clientEngine, null, clientSSLEngineConfigurator,
                                      !(proxy.type() == Proxy.Type.DIRECT), requestURI, sharedTransport,
                                      sharedTransportTimeout, proxyHeaders, grizzlyConnector, sslHandshakeFuture,
                                      upgradeRequest));

            InetAddress bindingAddress = Utils.getProperty(properties, ClientProperties.SOCKET_BINDING, InetAddress.class);

            if (bindingAddress == null) {
                connectionGrizzlyFuture = connectorHandler.connect(connectAddress);
            } else {
                connectionGrizzlyFuture = connectorHandler.connect(socketAddress, new InetSocketAddress(bindingAddress, 0));
            }

            try {
                final Connection connection = connectionGrizzlyFuture.get(timeoutMs, TimeUnit.MILLISECONDS);

                // wait for the SSL handshake to finish and handle error, if they occur
                if (sslHandshakeFuture != null) {
                    try {
                        sslHandshakeFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
                    } catch (ExecutionException e) {
                        throw new DeploymentException("SSL handshake has failed", e.getCause());
                    } catch (Exception e) {
                        throw new DeploymentException(String.format("Connection to '%s' failed.", requestURI),
                                                      e.getCause());
                    }
                }

                LOGGER.log(Level.CONFIG, String.format("Connected to '%s'.", connection.getPeerAddress()));

                return;
            } catch (InterruptedException interruptedException) {
                LOGGER.log(Level.CONFIG, String.format("Connection to '%s' failed.", requestURI), interruptedException);
                exception = interruptedException;
                closeTransport(privateTransport);
            } catch (TimeoutException timeoutException) {
                LOGGER.log(Level.CONFIG, String.format("Connection to '%s' failed.", requestURI), timeoutException);
                exception = timeoutException;
                closeTransport(privateTransport);
            } catch (ExecutionException executionException) {
                LOGGER.log(Level.CONFIG, String.format("Connection to '%s' failed.", requestURI), executionException);

                IOException ioException;
                exception = executionException.getCause();
                if ((exception != null) && (exception instanceof IOException)) {
                    ioException = (IOException) exception;
                    ProxySelector.getDefault().connectFailed(requestURI, socketAddress, ioException);
                }

                closeTransport(privateTransport);
            }
        }

        throw new DeploymentException("Connection failed.", exception);
    }

    private static TCPNIOTransport createTransport(ThreadPoolConfig workerThreadPoolConfig,
                                                   ThreadPoolConfig selectorThreadPoolConfig) {
        return createTransport(workerThreadPoolConfig, selectorThreadPoolConfig, false);
    }

    private static TCPNIOTransport createTransport(ThreadPoolConfig workerThreadPoolConfig,
                                                   ThreadPoolConfig selectorThreadPoolConfig, boolean sharedTransport) {

        // TYRUS-188: lots of threads were created for every single client instance.
        TCPNIOTransportBuilder transportBuilder = TCPNIOTransportBuilder.newInstance();

        /* This is set by Grizzly to true by default, but during some tests which created a lot of short-lived clients,
           a BindException has been thrown repeatedly by the Grizzly connect method. Setting this property to false
           removed the problem.*/
        transportBuilder.setReuseAddress(false);

        if (workerThreadPoolConfig == null) {
            if (sharedTransport) {
                // if the container is shared, we don't want to limit thread pool size by default.
                transportBuilder.setWorkerThreadPoolConfig(ThreadPoolConfig.defaultConfig());
            } else {
                transportBuilder.setWorkerThreadPoolConfig(
                        ThreadPoolConfig.defaultConfig().setMaxPoolSize(2).setCorePoolSize(2));
            }
        } else {
            transportBuilder.setWorkerThreadPoolConfig(workerThreadPoolConfig);
        }

        if (selectorThreadPoolConfig == null) {
            if (sharedTransport) {
                // if the container is shared, we don't want to limit thread pool size by default.
                transportBuilder.setSelectorThreadPoolConfig(ThreadPoolConfig.defaultConfig());
            } else {
                transportBuilder.setSelectorThreadPoolConfig(
                        ThreadPoolConfig.defaultConfig().setMaxPoolSize(1).setCorePoolSize(1));
                TCPNIOTransport transport = transportBuilder.build();
                // TODO: remove once setSelectorRunnersCount is in builder
                transport.setSelectorRunnersCount(1);
                return transport;
            }
        } else {
            transportBuilder.setSelectorThreadPoolConfig(selectorThreadPoolConfig);
        }

        return transportBuilder.build();
    }

    private Map<String, String> getProxyHeaders(Map<String, Object> properties) throws DeploymentException {
        //noinspection unchecked
        Map<String, String> proxyHeaders = Utils.getProperty(properties, ClientProperties.PROXY_HEADERS, Map.class);

        String wlsProxyUsername = null;
        String wlsProxyPassword = null;

        Object value = properties.get(ClientManager.WLS_PROXY_USERNAME);
        if (value != null) {
            if (value instanceof String) {
                wlsProxyUsername = (String) value;
            } else {
                throw new DeploymentException(ClientManager.WLS_PROXY_USERNAME + " only accept String values.");
            }
        }

        value = properties.get(ClientManager.WLS_PROXY_PASSWORD);
        if (value != null) {
            if (value instanceof String) {
                wlsProxyPassword = (String) value;
            } else {
                throw new DeploymentException(ClientManager.WLS_PROXY_PASSWORD + " only accept String values.");
            }
        }

        if (proxyHeaders == null) {
            if (wlsProxyUsername != null && wlsProxyPassword != null) {
                proxyHeaders = new HashMap<String, String>();
                proxyHeaders.put("Proxy-Authorization", "Basic "
                        + Base64.getEncoder().encodeToString(
                        (wlsProxyUsername + ":" + wlsProxyPassword).getBytes(Charset.forName("UTF-8"))));
            }
        } else {
            boolean proxyAuthPresent = false;
            for (Map.Entry<String, String> entry : proxyHeaders.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("Proxy-Authorization")) {
                    proxyAuthPresent = true;
                }
            }

            // if (proxyAuthPresent == true) then do nothing, proxy authorization header is already added.
            if (!proxyAuthPresent && wlsProxyUsername != null && wlsProxyPassword != null) {
                proxyHeaders.put("Proxy-Authorization", "Basic "
                        + Base64.getEncoder().encodeToString(
                        (wlsProxyUsername + ":" + wlsProxyPassword).getBytes(Charset.forName("UTF-8"))));
            }
        }
        return proxyHeaders;
    }

    private SocketAddress processProxy(URI uri, Map<String, Object> properties) throws DeploymentException {
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

        // compute direct address in case no proxy is found
        int port = Utils.getWsPort(uri);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, String.format("Not using proxy for URI '%s'.", uri));
        }
        return new InetSocketAddress(uri.getHost(), port);
    }

    /**
     * Add proxies to supplied list. Proxies will be obtained via supplied {@link ProxySelector} instance.
     *
     * @param proxySelector proxy selector.
     * @param uri           original request {@link URI}.
     * @param scheme        scheme used for proxy selection.
     * @param proxies       list of proxies (found proxies will be added to this list).
     */
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
                                    + ".net/jira/browse/TYRUS. Proxy '%s' will be ignored.", p));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Since standard Java {@link ProxySelector} does not support "ws" and "wss" schemes in {@link URI URIs},
     * we need to replace them by others ("socket", "https" or "http").
     *
     * @param wsUri  original {@link URI}.
     * @param scheme new scheme.
     * @return {@link URI} with updated scheme.
     */
    private URI getProxyUri(URI wsUri, String scheme) {
        try {
            return new URI(scheme, wsUri.getUserInfo(), wsUri.getHost(), wsUri.getPort(), wsUri.getPath(),
                           wsUri.getQuery(), wsUri.getFragment());
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, String.format("Exception during generating proxy URI '%s'", wsUri), e);
            return wsUri;
        }
    }

    private static Processor createFilterChain(ClientEngine engine,
                                               SSLEngineConfigurator serverSSLEngineConfigurator,
                                               final ExtendedSSLEngineConfigurator clientSSLEngineConfigurator,
                                               boolean proxy,
                                               final URI uri,
                                               boolean sharedTransport, Integer sharedTransportTimeout,
                                               Map<String, String> proxyHeaders,
                                               Callable<Void> grizzlyConnector,
                                               final TyrusFuture<Void> sslHandshakeFuture,
                                               final UpgradeRequest upgradeRequest) {
        FilterChainBuilder clientFilterChainBuilder = FilterChainBuilder.stateless();
        Filter sslFilter = null;

        clientFilterChainBuilder.add(new TransportFilter());
        if (serverSSLEngineConfigurator != null || clientSSLEngineConfigurator != null) {
            sslFilter = new SSLFilter(serverSSLEngineConfigurator, clientSSLEngineConfigurator) {
                {
                    addHandshakeListener(new HandshakeListener() {
                        @Override
                        public void onStart(Connection connection) {
                            // do nothing
                        }

                        @Override
                        public void onComplete(Connection connection) {
                            final SSLConnectionContext sslCtx = obtainSslConnectionContext(connection);
                            final SSLEngine sslEngine = sslCtx.getSslEngine();

                            // apply a custom host name verifier if present
                            HostnameVerifier customHostnameVerifier = clientSSLEngineConfigurator.hostnameVerifier;
                            if (customHostnameVerifier != null
                                    && !customHostnameVerifier.verify(uri.getHost(), sslEngine.getSession())) {
                                sslHandshakeFuture.setFailure(new SSLException(
                                        "Server host name verification using " + customHostnameVerifier.getClass()
                                                + " has failed"));
                                connection.terminateSilently();
                            } else {
                                sslHandshakeFuture.setResult(null);
                            }
                        }

                        @Override
                        public void onFailure(Connection connection, Throwable t) {
                            sslHandshakeFuture.setFailure(t);
                            connection.terminateSilently();
                        }
                    });
                }

                @Override
                protected void notifyHandshakeFailed(Connection connection, Throwable t) {
                    sslHandshakeFuture.setFailure(t);
                    connection.terminateSilently();
                }
            };

            if (proxy) {
                sslFilter = new FilterWrapper(sslFilter);
            }
            clientFilterChainBuilder.add(sslFilter);
        }

        if (sharedTransport) {
            clientFilterChainBuilder.add(new GrizzlyTransportTimeoutFilter(sharedTransportTimeout));
        }

        final HttpCodecFilter httpCodecFilter = new HttpCodecFilter();
        clientFilterChainBuilder.add(httpCodecFilter);


        clientFilterChainBuilder.add(new GrizzlyClientFilter(engine, proxy,
                                                             sslFilter, httpCodecFilter, uri, sharedTransport,
                                                             proxyHeaders, grizzlyConnector, upgradeRequest));

        return clientFilterChainBuilder.build();
    }

    private void closeTransport(TCPNIOTransport transport) {
        if (transport != null) {
            try {
                transport.shutdownNow();
            } catch (IOException e) {
                Logger.getLogger(GrizzlyClientSocket.class.getName())
                      .log(Level.INFO, "Exception thrown when closing Grizzly transport: " + e.getMessage(), e);
            }
        }
    }

    private static TCPNIOTransport getOrCreateSharedTransport(
            ThreadPoolConfig workerThreadPoolConfig, ThreadPoolConfig selectorThreadPoolConfig) throws IOException {
        synchronized (TRANSPORT_LOCK) {
            if (transport == null) {
                Logger.getLogger(GrizzlyClientSocket.class.getName()).log(Level.FINE, "Starting shared container.");
                transport = createTransport(workerThreadPoolConfig, selectorThreadPoolConfig, true);
                transport.start();
            }
        }

        return transport;
    }

    static void closeSharedTransport() {
        synchronized (TRANSPORT_LOCK) {
            if (transport != null) {
                try {
                    Logger.getLogger(GrizzlyClientSocket.class.getName()).log(Level.FINE, "Stopping shared container.");
                    transport.shutdownNow();
                } catch (IOException e) {
                    Logger.getLogger(GrizzlyClientSocket.class.getName())
                          .log(Level.INFO, "Exception thrown when closing Grizzly transport: " + e.getMessage(), e);
                }
            }
            transport = null;
        }
    }

    private ExtendedSSLEngineConfigurator getSSLEngineConfigurator(URI uri, Map<String, Object> properties) {
        Object configuratorObject = properties.get(ClientProperties.SSL_ENGINE_CONFIGURATOR);

        if (configuratorObject == null) {
            // if we are trying to access "wss" scheme and we don't have sslEngineConfigurator instance
            // we should try to create ssl connection using JVM properties.
            if ("wss".equalsIgnoreCase(uri.getScheme())) {
                final SSLContextConfigurator defaultConfig = new SSLContextConfigurator();
                defaultConfig.retrieve(System.getProperties());
                return new ExtendedSSLEngineConfigurator(defaultConfig.createSSLContext(), uri.getHost());
            } else {
                return null;
            }
        }

        if (configuratorObject instanceof SSLEngineConfigurator) {
            return new ExtendedSSLEngineConfigurator((SSLEngineConfigurator) configuratorObject, uri.getHost());
        }

        if (configuratorObject instanceof SslEngineConfigurator) {
            return new ExtendedSSLEngineConfigurator((SslEngineConfigurator) configuratorObject, uri.getHost());
        }

        // if we have reached here the ssl engine configuration property is set, but is of incompatible type
        LOGGER.log(Level.CONFIG,
                   String.format("Invalid type of configuration property of %s (%s), %s cannot be cast to %s or %s",
                                 ClientProperties.SSL_ENGINE_CONFIGURATOR, configuratorObject.toString(),
                                 configuratorObject.getClass().toString(),
                                 SSLEngineConfigurator.class.toString(), SslEngineConfigurator.class.toString()));
        return null;
    }

    /**
     * {@link SSLFilter} wrapper used for proxied connections. SSL filter gets "enabled" after initial proxy
     * communication, so after connection is established and SSL layer should start handling reading/writing messages.
     */
    static class FilterWrapper implements Filter {

        private final Filter filter;
        private boolean enabled;
        private volatile FilterChain filterChain;

        FilterWrapper(Filter filter) {
            this.filter = filter;
        }

        public void enable() {
            if (!enabled && filterChain != null) {
                filter.onAdded(filterChain);
            }

            this.enabled = true;
        }

        @Override
        public void onAdded(FilterChain filterChain) {
            this.filterChain = filterChain;

            if (enabled) {
                filter.onAdded(filterChain);
            }
        }

        @Override
        public void onRemoved(FilterChain filterChain) {
            filter.onRemoved(filterChain);

            if (enabled) {
                filter.onRemoved(filterChain);
            }
        }

        @Override
        public void onFilterChainChanged(FilterChain filterChain) {
            filter.onFilterChainChanged(filterChain);
        }

        @Override
        public NextAction handleRead(FilterChainContext ctx) throws IOException {
            if (enabled) {
                return filter.handleRead(ctx);
            } else {
                return ctx.getInvokeAction();
            }
        }

        @Override
        public NextAction handleWrite(FilterChainContext ctx) throws IOException {
            if (enabled) {
                return filter.handleWrite(ctx);
            } else {
                return ctx.getInvokeAction();
            }
        }

        @Override
        public NextAction handleConnect(FilterChainContext ctx) throws IOException {
            return ctx.getInvokeAction();
        }

        @Override
        public NextAction handleAccept(FilterChainContext ctx) throws IOException {
            return ctx.getInvokeAction();
        }

        @Override
        public NextAction handleEvent(FilterChainContext ctx, FilterChainEvent event) throws IOException {
            if (enabled) {
                return filter.handleEvent(ctx, event);
            } else {
                return ctx.getInvokeAction();
            }
        }

        @Override
        public NextAction handleClose(FilterChainContext ctx) throws IOException {
            if (enabled) {
                return filter.handleClose(ctx);
            } else {
                return ctx.getInvokeAction();
            }
        }

        @Override
        public void exceptionOccurred(FilterChainContext ctx, Throwable error) {
            if (enabled) {
                filter.exceptionOccurred(ctx, error);
            } else {
                ctx.getInvokeAction();
            }
        }
    }

    private static class ExtendedSSLEngineConfigurator extends SSLEngineConfigurator {
        private final HostnameVerifier hostnameVerifier;
        private final boolean hostVerificationEnabled;
        private final String peerHost;

        ExtendedSSLEngineConfigurator(SSLContext sslContext, String peerHost) {
            super(sslContext, true, false, false);
            this.hostnameVerifier = null;
            this.hostVerificationEnabled = true;
            this.peerHost = peerHost;
        }

        ExtendedSSLEngineConfigurator(SSLEngineConfigurator sslEngineConfigurator, String peerHost) {
            super(sslEngineConfigurator.getSslContext(), sslEngineConfigurator.isClientMode(),
                  sslEngineConfigurator.isNeedClientAuth(), sslEngineConfigurator.isWantClientAuth());
            this.hostnameVerifier = null;
            this.hostVerificationEnabled = true;
            this.peerHost = peerHost;
        }

        ExtendedSSLEngineConfigurator(SslEngineConfigurator sslEngineConfigurator, String peerHost) {
            super(sslEngineConfigurator.getSslContext(), sslEngineConfigurator.isClientMode(),
                  sslEngineConfigurator.isNeedClientAuth(), sslEngineConfigurator.isWantClientAuth());
            this.hostnameVerifier = sslEngineConfigurator.getHostnameVerifier();
            this.hostVerificationEnabled = sslEngineConfigurator.isHostVerificationEnabled();
            this.peerHost = peerHost;
        }

        @Override
        public SSLEngine createSSLEngine(final String peerHost, final int peerPort) {
            /* the port is not part of host name verification, it is present in the constructor because of Kerberos
            (which is not supported by Tyrus) */

            // We use the peerHost provided by Tyrus, because Grizzly provides the peerHost only for JDK 7+
            SSLEngine sslEngine = super.createSSLEngine(this.peerHost, peerPort);

            if (hostVerificationEnabled && hostnameVerifier == null) {
                SSLParameters sslParameters = sslEngine.getSSLParameters();
                sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
                sslEngine.setSSLParameters(sslParameters);
            }

            return sslEngine;
        }
    }
}
