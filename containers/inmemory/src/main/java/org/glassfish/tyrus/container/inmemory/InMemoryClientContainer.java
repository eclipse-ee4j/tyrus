/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.container.inmemory;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.RequestContext;
import org.glassfish.tyrus.core.TyrusUpgradeResponse;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.spi.ClientContainer;
import org.glassfish.tyrus.spi.ClientEngine;
import org.glassfish.tyrus.spi.CompletionHandler;
import org.glassfish.tyrus.spi.Connection;
import org.glassfish.tyrus.spi.ReadHandler;
import org.glassfish.tyrus.spi.UpgradeRequest;
import org.glassfish.tyrus.spi.WebSocketEngine;
import org.glassfish.tyrus.spi.Writer;

/**
 * In-Memory {@link org.glassfish.tyrus.spi.ClientContainer} implementation.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class InMemoryClientContainer implements ClientContainer {

    /**
     * Property used to define server config used for in-memory container.
     * <p>
     * Value has to be instance of {@link javax.websocket.server.ServerApplicationConfig} and is provided as user
     * parameter in {@link javax.websocket.ClientEndpointConfig}.
     */
    public static final String SERVER_CONFIG = "org.glassfish.tyrus.container.inmemory.ServerConfig";

    @Override
    public void openClientSocket(ClientEndpointConfig cec, Map<String, Object> properties,
                                 final ClientEngine clientEngine) throws DeploymentException, IOException {
        final UpgradeRequest upgradeRequest = clientEngine.createUpgradeRequest(null);

        final ServerApplicationConfig serverApplicationConfig = getServerApplicationConfig(cec);
        final TyrusServerContainer tyrusServerContainer = new TyrusServerContainer(serverApplicationConfig) {
            private final WebSocketEngine webSocketEngine = TyrusWebSocketEngine.builder(this).build();

            private ClientManager clientManager = null;

            @Override
            public void register(Class<?> endpointClass) throws DeploymentException {
                webSocketEngine.register(endpointClass, "/");
            }

            @Override
            public void register(ServerEndpointConfig serverEndpointConfig) throws DeploymentException {
                webSocketEngine.register(serverEndpointConfig, "/");
            }

            @Override
            public WebSocketEngine getWebSocketEngine() {
                return webSocketEngine;
            }

            // This has to be overridden, because Grizzly container is used by default
            @Override
            protected synchronized ClientManager getClientManager() {
                if (clientManager == null) {
                    clientManager = ClientManager.createClient(InMemoryClientContainer.class.getName(), this);
                }

                return clientManager;
            }
        };

        tyrusServerContainer.doneDeployment();
        // placeholder values, not used anywhere in this case.
        tyrusServerContainer.start("/inmemory", 0);

        final TyrusUpgradeResponse upgradeResponse = new TyrusUpgradeResponse();

        StringBuilder sb = new StringBuilder();
        sb.append(upgradeRequest.getRequestURI().getPath());
        if (upgradeRequest.getRequestURI().getQuery() != null) {
            sb.append('?').append(upgradeRequest.getRequestURI().getQuery());
        }
        if (sb.length() == 0) {
            sb.append('/');
        }

        final RequestContext requestContext =
                new RequestContext.Builder().requestURI(URI.create(sb.toString())).build();
        requestContext.getHeaders().putAll(upgradeRequest.getHeaders());

        final WebSocketEngine.UpgradeInfo upgradeInfo =
                tyrusServerContainer.getWebSocketEngine().upgrade(requestContext, upgradeResponse);
        switch (upgradeInfo.getStatus()) {
            case HANDSHAKE_FAILED:
                tyrusServerContainer.shutdown();
                throw new DeploymentException("");
            case NOT_APPLICABLE:
                tyrusServerContainer.shutdown();
                throw new DeploymentException("");
            case SUCCESS:

                final InMemoryWriter clientWriter = new InMemoryWriter() {
                    @Override
                    public void close() throws IOException {
                        tyrusServerContainer.shutdown();
                    }
                };
                final InMemoryWriter serverWriter = new InMemoryWriter() {
                    @Override
                    public void close() throws IOException {
                        tyrusServerContainer.shutdown();
                    }
                };

                final Connection serverConnection = upgradeInfo.createConnection(serverWriter, null);
                final ClientEngine.ClientUpgradeInfo clientClientUpgradeInfo =
                        clientEngine.processResponse(upgradeResponse, clientWriter, null);
                final Connection clientConnection = clientClientUpgradeInfo.createConnection();

                if (clientConnection == null) {
                    throw new DeploymentException("");
                }

                serverWriter.setReadHandler(clientConnection.getReadHandler());
                clientWriter.setReadHandler(serverConnection.getReadHandler());
        }
    }

    private ServerApplicationConfig getServerApplicationConfig(ClientEndpointConfig clientEndpointConfig) throws
            DeploymentException {
        final Object o = clientEndpointConfig.getUserProperties().get(SERVER_CONFIG);
        if (o != null && o instanceof ServerApplicationConfig) {
            return (ServerApplicationConfig) o;
        }

        throw new DeploymentException("ServerApplicationConfig not present.");
    }

    private abstract static class InMemoryWriter extends Writer {

        private final List<ByteBuffer> cache = new ArrayList<ByteBuffer>();
        private volatile ReadHandler readHandler = null;

        @Override
        public void write(ByteBuffer buffer, CompletionHandler<ByteBuffer> completionHandler) {
            synchronized (cache) {
                if (readHandler == null) {
                    cache.add(buffer);
                } else {
                    readHandler.handle(buffer);
                }

                completionHandler.completed(buffer);
            }
        }

        private void setReadHandler(ReadHandler readHandler) {
            synchronized (cache) {
                for (ByteBuffer buffer : cache) {
                    readHandler.handle(buffer);
                }
                this.readHandler = readHandler;
            }
        }
    }
}
