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

package org.glassfish.tyrus.client;


import java.net.InetAddress;
import java.net.URI;

import org.glassfish.tyrus.client.auth.AuthConfig;
import org.glassfish.tyrus.client.auth.AuthenticationException;
import org.glassfish.tyrus.client.auth.Authenticator;
import org.glassfish.tyrus.client.auth.Credentials;
import org.glassfish.tyrus.spi.UpgradeResponse;

/**
 * Tyrus client configuration properties.
 *
 * @author Petr Janouch
 */
public final class ClientProperties {

    /**
     * Property usable in {@link ClientManager#getProperties()}.
     * <p>
     * Value must be {@code int} and represents handshake timeout in milliseconds. Default value is 30000 (30 seconds).
     */
    public static final String HANDSHAKE_TIMEOUT = "org.glassfish.tyrus.client.ClientManager.ContainerTimeout";

    /**
     * Property usable in {@link ClientManager#getProperties()}.
     * <p>
     * Value must be {@link org.glassfish.tyrus.client.ClientManager.ReconnectHandler} instance.
     *
     * @see ClientProperties#RETRY_AFTER_SERVICE_UNAVAILABLE
     */
    public static final String RECONNECT_HANDLER = "org.glassfish.tyrus.client.ClientManager.ReconnectHandler";

    /**
     * User property to set proxy URI.
     * <p>
     * Value is expected to be {@link String} and represent proxy URI. Protocol part is currently ignored
     * but must be present ({@link java.net.URI#URI(String)} is used for parsing).
     * <pre>
     *     client.getProperties().put(ClientProperties.PROXY_URI, "http://my.proxy.com:80");
     *     client.connectToServer(...);
     * </pre>
     *
     * @see javax.websocket.ClientEndpointConfig#getUserProperties()
     */
    public static final String PROXY_URI = "org.glassfish.tyrus.client.proxy";

    /**
     * User property to set additional proxy headers.
     * <p>
     * Value is expected to be {@link java.util.Map}&lt;{@link String}, {@link String}&gt; and represent raw http headers
     * to be added to initial request which is sent to proxy. Key corresponds to header name, value is header
     * value.
     * <p>
     * Sample below demonstrates use of this feature to set preemptive basic proxy authentication:
     * <pre>
     *     final HashMap&lt;String, String&gt; proxyHeaders = new HashMap&lt;String, String&gt;();
     *     proxyHeaders.put("Proxy-Authorization", "Basic " +
     *         Base64.getEncoder().encodeToString("username:password".getBytes(Charset.forName("UTF-8"))));
     *
     *     client.getProperties().put(ClientProperties.PROXY_HEADERS, proxyHeaders);
     *     client.connectToServer(...);
     * </pre>
     * Please note that these headers will be used only when establishing proxy connection, for modifying
     * WebSocket handshake headers, see
     * {@link javax.websocket.ClientEndpointConfig.Configurator#beforeRequest(java.util.Map)}.
     *
     * @see javax.websocket.ClientEndpointConfig#getUserProperties()
     */
    public static final String PROXY_HEADERS = "org.glassfish.tyrus.client.proxy.headers";

    /**
     * Property usable in {@link ClientManager#getProperties()} as a key for SSL configuration.
     * <p>
     * Value is expected to be either {@code org.glassfish.grizzly.ssl.SSLEngineConfigurator} or
     * {@link org.glassfish.tyrus.client.SslEngineConfigurator} when configuring Grizzly client or only
     * {@link org.glassfish.tyrus.client.SslEngineConfigurator} when configuring JDK client.
     * <p>
     * The advantage of using {@link org.glassfish.tyrus.client.SslEngineConfigurator} with Grizzly client is that
     * {@link org.glassfish.tyrus.client.SslEngineConfigurator} allows configuration of host name verification
     * (which is turned on by default)
     * <p>
     * Example configuration for JDK client:
     * <pre>
     *      SslContextConfigurator sslContextConfigurator = new SslContextConfigurator();
     *      sslContextConfigurator.setTrustStoreFile("...");
     *      sslContextConfigurator.setTrustStorePassword("...");
     *      sslContextConfigurator.setTrustStoreType("...");
     *      sslContextConfigurator.setKeyStoreFile("...");
     *      sslContextConfigurator.setKeyStorePassword("...");
     *      sslContextConfigurator.setKeyStoreType("...");
     *      SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(sslContextConfigurator, true,
     *          false, false);
     *      client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
     * </pre>
     */
    public static final String SSL_ENGINE_CONFIGURATOR = "org.glassfish.tyrus.client.sslEngineConfigurator";

    /**
     * Property name for maximal incoming buffer size.
     * <p>
     * Can be set in properties map (see {@link
     * org.glassfish.tyrus.spi.ClientContainer#openClientSocket(javax.websocket.ClientEndpointConfig, java.util.Map,
     * org.glassfish.tyrus.spi.ClientEngine)}.
     */
    public static final String INCOMING_BUFFER_SIZE = "org.glassfish.tyrus.incomingBufferSize";

    /**
     * When set to {@code true} (boolean value), client runtime preserves used container and reuses it for outgoing
     * connections.
     * <p>
     * A single thread pool is reused by all clients with this property set to {@code true}.
     * JDK client supports only shared container option, so setting this property has no effect.
     *
     * @see #SHARED_CONTAINER_IDLE_TIMEOUT
     */
    public static final String SHARED_CONTAINER = "org.glassfish.tyrus.client.sharedContainer";

    /**
     * Container idle timeout in seconds ({@link Integer} value).
     * <p>
     * When the timeout elapses, the shared thread pool will be destroyed.
     *
     * @see #SHARED_CONTAINER
     */
    public static final String SHARED_CONTAINER_IDLE_TIMEOUT = "org.glassfish.tyrus.client.sharedContainerIdleTimeout";

    /**
     * User property to set worker thread pool configuration.
     * <p>
     * An instance of {@link org.glassfish.tyrus.client.ThreadPoolConfig} is expected for both JDK
     * and Grizzly client. Instance of {@code org.glassfish.grizzly.threadpool.ThreadPoolConfig}, can be used
     * for Grizzly client.
     * <p>
     * Sample below demonstrates how to use this property:
     * <pre>
     *     client.getProperties().put(ClientProperties.WORKER_THREAD_POOL_CONFIG, ThreadPoolConfig.defaultConfig());
     * </pre>
     */
    public static final String WORKER_THREAD_POOL_CONFIG = "org.glassfish.tyrus.client.workerThreadPoolConfig";

    /**
     * Authentication configuration. If no AuthConfig is specified then default configuration will be used,
     * containing both Basic and Digest provided authenticators.
     * <p>
     * Value must be {@link AuthConfig} instance.
     * <p>
     * Sample below demonstrates how to use this property:
     * <pre>
     *     client.getProperties().put(ClientProperties.AUTH_CONFIG, AuthConfig.builder().enableProvidedBasicAuth()
     *     .build());
     * </pre>
     *
     * @see AuthConfig
     * @see AuthConfig.Builder
     * @see Authenticator
     */
    public static final String AUTH_CONFIG = "org.glassfish.tyrus.client.http.auth.AuthConfig";

    /**
     * Authentication credentials.
     * <p>
     * Value must be {@link Credentials} instance.
     * <p>
     * Provided authenticators (both Basic and Digest) require this property set,
     * otherwise {@link AuthenticationException} will be thrown during a handshake.
     * User defined authenticators may look up credentials in another sources.
     * <p>
     * Sample below demonstrates how to use this property:
     * <pre>
     *     client.getProperties().put(ClientProperties.CREDENTIALS, new Credentials("websocket_user", "password");
     * </pre>
     *
     * @see Credentials
     * @see AuthConfig
     * @see Authenticator
     */
    public static final String CREDENTIALS = "org.glassfish.tyrus.client.http.auth.Credentials";

    /**
     * HTTP Redirect support.
     * <p>
     * Value is expected to be {@code boolean}. Default value is {@code false}.
     * <p>
     * When set to {@code true} and one of the following redirection HTTP response status code (3xx) is received during
     * a handshake, client will attempt to connect to the {@link URI} contained in {@value UpgradeResponse#LOCATION}
     * header from handshake response. Number of redirection is limited by property {@link #REDIRECT_THRESHOLD}
     * (integer value), while default value is {@value TyrusClientEngine#DEFAULT_REDIRECT_THRESHOLD}.
     * <p>
     * List of supported HTTP status codes:
     * <ul>
     * <li>{@code 300 - Multiple Choices}</li>
     * <li>{@code 301 - Moved permanently}</li>
     * <li>{@code 302 - Found}</li>
     * <li>{@code 303 - See Other (since HTTP/1.1)}</li>
     * <li>{@code 307 - Temporary Redirect (since HTTP/1.1)}</li>
     * <li>{@code 308 - Permanent Redirect (Experimental RFC; RFC 7238)}</li>
     * </ul>
     *
     * @see #REDIRECT_THRESHOLD
     */
    public static final String REDIRECT_ENABLED = "org.glassfish.tyrus.client.http.redirect";

    /**
     * The maximal number of redirects during single handshake.
     * <p>
     * Value is expected to be positive {@link Integer}. Default value is {@value
     * TyrusClientEngine#DEFAULT_REDIRECT_THRESHOLD}.
     * <p>
     * HTTP redirection must be enabled by property {@link #REDIRECT_ENABLED}, otherwise {@code REDIRECT_THRESHOLD} is
     * not applied.
     *
     * @see #REDIRECT_ENABLED
     * @see RedirectException
     */
    public static final String REDIRECT_THRESHOLD = "org.glassfish.tyrus.client.http.redirect.threshold";

    /**
     * HTTP Service Unavailable - {@value UpgradeResponse#RETRY_AFTER} reconnect support.
     * <p>
     * Value is expected to be {@code boolean}. Default value is {@code false}.
     * <p>
     * When set to {@code true} and HTTP response code {@code 503 - Service Unavailable} is received, client will
     * attempt to reconnect after delay specified in {@value UpgradeResponse#RETRY_AFTER} header from handshake
     * response. According to RFC 2616 the value must be decimal integer (representing delay in seconds) or {@code
     * http-date}.
     * <p>
     * Tyrus client will try to reconnect after this delay if:
     * <ul>
     * <li>{@value UpgradeResponse#RETRY_AFTER} header is present and is not empty</li>
     * <li>{@value UpgradeResponse#RETRY_AFTER} header can be parsed</li>
     * <li>number of reconnection attempts does not exceed 5</li>
     * <li>delay is not longer then 300 seconds</li>
     * </ul>
     *
     * @see RetryAfterException
     * @see ClientProperties#RECONNECT_HANDLER
     * @see ClientManager.ReconnectHandler
     * @see ClientManager.ReconnectHandler#onConnectFailure(Exception)
     */
    public static final String RETRY_AFTER_SERVICE_UNAVAILABLE = "org.glassfish.tyrus.client.http.retryAfter";

    /**
     * User property to configure logging of HTTP upgrade messages.
     * <p>
     * Value is expected to be {@code boolean}. Default value is {@code false}.
     * <p>
     * When set to {@code true} upgrade request and response messages will be logged regardless of the logging
     * level configuration. When the logging is configured to {@link java.util.logging.Level#FINE} or lower,
     * this setting will have no effect as at this level HTTP upgrade messages will be logged anyway.
     */
    public static final String LOG_HTTP_UPGRADE = "org.glassfish.tyrus.client.http.logUpgrade";

    /**
     * Property name for registering a custom masking key generator. The expected value is an instance of
     * {@link org.glassfish.tyrus.core.MaskingKeyGenerator}.
     * <p>
     * As a security measure, all frames originating on websocket client have to be masked with random 4B value, which
     * should be freshly generated for each frame. Moreover to fully comply with the security requirements of RFC 6455,
     * a masking key of a frame must not be predictable from masking keys of previous frames and therefore Tyrus uses
     * {@link java.security.SecureRandom} as a default masking key generator. While this is perfectly OK for most Tyrus
     * client use cases, usage of {@link java.security.SecureRandom} might prove to be a performance issue,
     * when the client is used for instance for highly parallel stress testing as {@link java.security.SecureRandom}
     * uses a synchronized singleton as a random entropy provider in its internals.
     * <p>
     * This property allows replacing the default {@link java.security.SecureRandom} with a more scalable provider
     * of masking keys.
     */
    public static final String MASKING_KEY_GENERATOR = "org.glassfish.tyrus.client.maskingKeyGenerator";

    /**
     * Property name for defining local binding address for all socket created by the client. The expected value is an instance
     * of {@link java.net.InetAddress}.
     * <p>
     * Sample below demonstrates how to use this property:
     * <pre>
     *     client.getProperties().put(ClientProperties.SOCKET_BINDING, InetAddress.getByName("127.0.0.1"));
     * </pre>
     */
    public static final String SOCKET_BINDING = "org.glassfish.tyrus.client.socketBinding";
}