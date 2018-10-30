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

package org.glassfish.tyrus.container.jdk.client;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.core.CloseReasons;
import org.glassfish.tyrus.core.TyrusUpgradeResponse;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.spi.ClientEngine;
import org.glassfish.tyrus.spi.CompletionHandler;
import org.glassfish.tyrus.spi.Connection;
import org.glassfish.tyrus.spi.Connection.CloseListener;
import org.glassfish.tyrus.spi.UpgradeRequest;
import org.glassfish.tyrus.spi.Writer;

/**
 * A filter that interacts with Tyrus SPI and handles proxy.
 *
 * @author Petr Janouch
 */
class ClientFilter extends Filter {

    private static final Logger LOGGER = Logger.getLogger(ClientFilter.class.getName());

    private final ClientEngine clientEngine;
    private final HttpResponseParser responseParser = new HttpResponseParser();
    private final Map<String, String> proxyHeaders;
    private final UpgradeRequest upgradeRequest;
    private final Callable<Void> jdkConnector;
    private final AtomicBoolean connectionClosed = new AtomicBoolean(false);

    private volatile boolean proxy;
    private volatile Connection wsConnection;
    private volatile boolean connectedToProxy = false;
    private volatile CompletionHandler<Void> connectCompletionHandler;

    /**
     * Constructor.
     *
     * @param downstreamFilter a filer that is positioned directly under this filter.
     * @param clientEngine     client engine instance.
     * @param properties       client properties.
     * @param jdkConnector     callback to connecting with modified {@link UpgradeRequest} if necessary.
     * @param upgradeRequest   upgrade request to be used for this client session.
     */
    // * @param proxyHeaders     map representing headers to be added to request sent to proxy (HTTP CONNECT).
    ClientFilter(Filter downstreamFilter, ClientEngine clientEngine, Map<String, Object> properties,
                 Callable<Void> jdkConnector, UpgradeRequest upgradeRequest)
            throws DeploymentException {
        super(downstreamFilter);
        this.clientEngine = clientEngine;
        this.proxyHeaders = getProxyHeaders(properties);
        this.jdkConnector = jdkConnector;
        this.upgradeRequest = upgradeRequest;
    }

    /**
     * @param address                  an address where to connect (server or proxy).
     * @param proxy                    {@code true} if the connection will be established via proxy, {@code false}
     *                                 otherwise.
     * @param connectCompletionHandler completion handler.
     */
    void connect(SocketAddress address, boolean proxy, CompletionHandler<Void> connectCompletionHandler) {
        this.connectCompletionHandler = connectCompletionHandler;
        this.proxy = proxy;
        downstreamFilter.connect(address, this);
    }

    @Override
    public void processConnect() {
        final JdkUpgradeRequest handshakeUpgradeRequest;

        if (proxy) {
            handshakeUpgradeRequest = createProxyUpgradeRequest(upgradeRequest.getRequestURI());
        } else {
            handshakeUpgradeRequest = getJdkUpgradeRequest(upgradeRequest, downstreamFilter);
        }

        sendRequest(downstreamFilter, handshakeUpgradeRequest);
    }

    private void sendRequest(final Filter downstreamFilter, JdkUpgradeRequest handshakeUpgradeRequest) {
        downstreamFilter.write(HttpRequestBuilder.build(handshakeUpgradeRequest), new CompletionHandler<ByteBuffer>() {
            @Override
            public void failed(Throwable throwable) {
                onError(throwable);
            }
        });
    }

    private JdkUpgradeRequest getJdkUpgradeRequest(final UpgradeRequest upgradeRequest, final Filter downstreamFilter) {
        downstreamFilter.startSsl();
        return createHandshakeUpgradeRequest(upgradeRequest);
    }

    @Override
    public boolean processRead(ByteBuffer data) {
        if (wsConnection == null) {

            TyrusUpgradeResponse tyrusUpgradeResponse;
            try {
                responseParser.appendData(data);

                if (!responseParser.isComplete()) {
                    return false;
                }

                try {
                    tyrusUpgradeResponse = responseParser.parseUpgradeResponse();
                } finally {
                    responseParser.clear();
                }
            } catch (ParseException e) {
                clientEngine.processError(e);
                closeConnection();
                return false;
            }

            if (proxy && !connectedToProxy) {
                if (tyrusUpgradeResponse.getStatus() != 200) {
                    processError(new IOException("Could not connect to a proxy. The proxy returned the following status code: "
                            + tyrusUpgradeResponse.getStatus()));
                    return false;
                }

                connectedToProxy = true;
                downstreamFilter.startSsl();
                sendRequest(downstreamFilter, createHandshakeUpgradeRequest(upgradeRequest));
                return false;
            }

            JdkWriter writer = new JdkWriter(downstreamFilter, connectionClosed);

            ClientEngine.ClientUpgradeInfo clientUpgradeInfo = clientEngine.processResponse(
                    tyrusUpgradeResponse,
                    writer,
                    new CloseListener() {

                        @Override
                        public void close(CloseReason reason) {
                            closeConnection();
                        }
                    }
            );

            switch (clientUpgradeInfo.getUpgradeStatus()) {
                case ANOTHER_UPGRADE_REQUEST_REQUIRED:
                    closeConnection();
                    try {
                        jdkConnector.call();
                    } catch (Exception e) {
                        closeConnection();
                        clientEngine.processError(e);
                    }
                    break;
                case SUCCESS:
                    wsConnection = clientUpgradeInfo.createConnection();

                    if (data.hasRemaining()) {
                        wsConnection.getReadHandler().handle(data);
                    }

                    break;
                case UPGRADE_REQUEST_FAILED:
                    closeConnection();
                    break;
                default:
                    break;
            }
        } else {
            wsConnection.getReadHandler().handle(data);
        }

        return false;
    }

    @Override
    public void processConnectionClosed() {
        LOGGER.log(Level.FINE, "Connection has been closed by the server");

        if (connectCompletionHandler != null) {
            connectCompletionHandler.failed(null);
        }
        if (wsConnection == null) {
            return;
        }

        wsConnection.close(CloseReasons.CLOSED_ABNORMALLY.getCloseReason());
    }

    @Override
    void processError(Throwable t) {
        // connectCompletionHandler != null means that we are still in "connecting state".
        if (connectCompletionHandler != null) {
            closeConnection();
            connectCompletionHandler.failed(t);
            return;
        }

        LOGGER.log(Level.SEVERE, "Connection error has occurred", t);
        if (wsConnection != null) {
            wsConnection.close(CloseReasons.CLOSED_ABNORMALLY.getCloseReason());
        }

        closeConnection();
    }

    @Override
    void processSslHandshakeCompleted() {
        // the connection is considered established at this point
        connectCompletionHandler.completed(null);
        connectCompletionHandler = null;
    }

    @Override
    void close() {
        closeConnection();
    }

    private void closeConnection() {
        if (connectionClosed.compareAndSet(false, true)) {
            downstreamFilter.close();
        }
    }

    private static class JdkWriter extends Writer {

        private final Filter downstreamFilter;
        private final AtomicBoolean connectionClosed;

        JdkWriter(Filter downstreamFilter, AtomicBoolean connectionClosed) {
            this.downstreamFilter = downstreamFilter;
            this.connectionClosed = connectionClosed;
        }

        @Override
        public void close() throws IOException {
            if (connectionClosed.compareAndSet(false, true)) {
                downstreamFilter.close();
            }
        }

        @Override
        public void write(ByteBuffer buffer, CompletionHandler<ByteBuffer> completionHandler) {
            downstreamFilter.write(buffer, completionHandler);
        }
    }

    private JdkUpgradeRequest createHandshakeUpgradeRequest(final UpgradeRequest upgradeRequest) {
        return new JdkUpgradeRequest(upgradeRequest) {

            @Override
            public String getHttpMethod() {
                return "GET";
            }

            @Override
            public String getRequestUri() {
                StringBuilder sb = new StringBuilder();
                final URI uri = URI.create(upgradeRequest.getRequestUri());
                sb.append(uri.getPath());
                final String query = uri.getQuery();
                if (query != null) {
                    sb.append('?').append(query);
                }
                if (sb.length() == 0) {
                    sb.append('/');
                }
                return sb.toString();
            }
        };
    }

    private JdkUpgradeRequest createProxyUpgradeRequest(final URI uri) {
        return new JdkUpgradeRequest(null) {

            @Override
            public String getHttpMethod() {
                return "CONNECT";
            }

            @Override
            public String getRequestUri() {
                final int requestPort = Utils.getWsPort(uri);
                return String.format("%s:%d", uri.getHost(), requestPort);
            }

            @Override
            public Map<String, List<String>> getHeaders() {
                Map<String, List<String>> headers = new HashMap<>();
                if (proxyHeaders != null) {
                    for (Map.Entry<String, String> entry : proxyHeaders.entrySet()) {
                        headers.put(entry.getKey(), Collections.singletonList(entry.getValue()));
                    }
                }
                headers.put("Host", Collections.singletonList(uri.getHost()));
                headers.put("ProxyConnection", Collections.singletonList("keep-alive"));
                headers.put("Connection", Collections.singletonList("keep-alive"));
                return headers;
            }
        };
    }


    private static Map<String, String> getProxyHeaders(Map<String, Object> properties) throws DeploymentException {
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
                proxyHeaders = new HashMap<>();
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
}
