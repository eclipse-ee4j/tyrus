/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Extension;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.server.HandshakeRequest;

import org.glassfish.tyrus.client.auth.AuthConfig;
import org.glassfish.tyrus.client.auth.AuthenticationException;
import org.glassfish.tyrus.client.auth.Authenticator;
import org.glassfish.tyrus.client.auth.Credentials;
import org.glassfish.tyrus.core.DebugContext;
import org.glassfish.tyrus.core.Handshake;
import org.glassfish.tyrus.core.HandshakeException;
import org.glassfish.tyrus.core.MaskingKeyGenerator;
import org.glassfish.tyrus.core.ProtocolHandler;
import org.glassfish.tyrus.core.RequestContext;
import org.glassfish.tyrus.core.TyrusEndpointWrapper;
import org.glassfish.tyrus.core.TyrusExtension;
import org.glassfish.tyrus.core.TyrusWebSocket;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.core.Version;
import org.glassfish.tyrus.core.WebSocketException;
import org.glassfish.tyrus.core.extension.ExtendedExtension;
import org.glassfish.tyrus.core.frame.CloseFrame;
import org.glassfish.tyrus.core.frame.Frame;
import org.glassfish.tyrus.core.l10n.LocalizationMessages;
import org.glassfish.tyrus.spi.ClientContainer;
import org.glassfish.tyrus.spi.ClientEngine;
import org.glassfish.tyrus.spi.Connection;
import org.glassfish.tyrus.spi.ReadHandler;
import org.glassfish.tyrus.spi.UpgradeRequest;
import org.glassfish.tyrus.spi.UpgradeResponse;
import org.glassfish.tyrus.spi.Writer;

/**
 * Tyrus {@link ClientEngine} implementation.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class TyrusClientEngine implements ClientEngine {

    /**
     * Default incoming buffer size for client container.
     */
    public static final int DEFAULT_INCOMING_BUFFER_SIZE = 4194315; // 4M (payload) + 11 (frame overhead)

    private static final Logger LOGGER = Logger.getLogger(TyrusClientEngine.class.getName());

    private static final Version DEFAULT_VERSION = Version.DRAFT17;
    private static final int BUFFER_STEP_SIZE = 256;
    private static final int DEFAULT_REDIRECT_THRESHOLD = 5;

    private final ProtocolHandler protocolHandler;
    private final TyrusEndpointWrapper endpointWrapper;
    private final ClientHandshakeListener listener;
    private final Map<String, Object> properties;
    private final URI connectToServerUriParam;
    private final Boolean redirectEnabled;
    private final int redirectThreshold;
    private final DebugContext debugContext;
    private final boolean logUpgradeMessages;

    private volatile Handshake clientHandShake = null;
    private volatile TimeoutHandler timeoutHandler = null;
    private volatile TyrusClientEngineState clientEngineState = TyrusClientEngineState.INIT;
    private volatile URI redirectLocation = null;

    private final Set<URI> redirectUriHistory;

    /**
     * Create {@link org.glassfish.tyrus.spi.WebSocketEngine} instance based on passed {@link WebSocketContainer} and
     * with configured maximal incoming buffer size.
     *
     * @param endpointWrapper         wrapped client endpoint.
     * @param listener                used for reporting back the outcome of handshake. {@link
     *                                ClientHandshakeListener#onSessionCreated(jakarta.websocket.Session)} is invoked if
     *                                handshake is completed and provided {@link Session} is open and ready to be
     *                                returned from {@link WebSocketContainer#connectToServer(Class,
     *                                jakarta.websocket.ClientEndpointConfig, java.net.URI)} (and alternatives) call.
     * @param properties              passed container properties, see {@link org.glassfish.tyrus.client *
     *                                .ClientManager#getProperties()}.
     * @param connectToServerUriParam to which the client is connecting.
     * @param debugContext            debug context.
     */
    /* package */ TyrusClientEngine(TyrusEndpointWrapper endpointWrapper, ClientHandshakeListener listener,
                                    Map<String, Object> properties, URI connectToServerUriParam,
                                    DebugContext debugContext) {
        this.endpointWrapper = endpointWrapper;
        this.listener = listener;
        this.properties = properties;
        this.connectToServerUriParam = connectToServerUriParam;

        MaskingKeyGenerator maskingKeyGenerator = Utils.getProperty(properties, ClientProperties
                .MASKING_KEY_GENERATOR, MaskingKeyGenerator.class, null);
        protocolHandler = DEFAULT_VERSION.createHandler(true, maskingKeyGenerator);

        this.redirectUriHistory = Collections.synchronizedSet(new HashSet<URI>(DEFAULT_REDIRECT_THRESHOLD));

        this.redirectEnabled = Utils.getProperty(properties, ClientProperties.REDIRECT_ENABLED, Boolean.class, false);
        Integer redirectThreshold = Utils.getProperty(properties, ClientProperties.REDIRECT_THRESHOLD, Integer.class,
                                                      DEFAULT_REDIRECT_THRESHOLD);
        if (redirectThreshold == null) {
            redirectThreshold = DEFAULT_REDIRECT_THRESHOLD;
        }
        this.redirectThreshold = redirectThreshold;

        this.debugContext = debugContext;
        this.logUpgradeMessages =
                Utils.getProperty(properties, ClientProperties.LOG_HTTP_UPGRADE, Boolean.class, false);

        debugContext.appendLogMessage(LOGGER, Level.FINE, DebugContext.Type.OTHER, "Redirect enabled: ",
                                      redirectEnabled);
        if (redirectEnabled) {
            debugContext.appendLogMessage(LOGGER, Level.FINE, DebugContext.Type.OTHER, "Redirect threshold: ",
                                          redirectThreshold);
        }
    }

    @Override
    public UpgradeRequest createUpgradeRequest(TimeoutHandler timeoutHandler) {

        switch (clientEngineState) {
            case INIT: {
                ClientEndpointConfig config = (ClientEndpointConfig) endpointWrapper.getEndpointConfig();
                this.timeoutHandler = timeoutHandler;

                clientHandShake = Handshake.createClientHandshake(
                        RequestContext.Builder.create().requestURI(connectToServerUriParam)
                                              .secure("wss".equals(connectToServerUriParam.getScheme())).build());
                clientHandShake.setExtensions(config.getExtensions());
                clientHandShake.setSubProtocols(config.getPreferredSubprotocols());
                clientHandShake.prepareRequest();

                UpgradeRequest upgradeRequest = clientHandShake.getRequest();
                config.getConfigurator().beforeRequest(upgradeRequest.getHeaders());

                clientEngineState = TyrusClientEngineState.UPGRADE_REQUEST_CREATED;
                logUpgradeRequest(upgradeRequest);
                return upgradeRequest;
            }
            case REDIRECT_REQUIRED: {
                this.timeoutHandler = timeoutHandler;

                final URI requestUri = redirectLocation;

                RequestContext requestContext =
                        RequestContext.Builder.create(clientHandShake.getRequest())
                                              .requestURI(requestUri)
                                              .secure("wss".equalsIgnoreCase(requestUri.getScheme())).build();
                Handshake.updateHostAndOrigin(requestContext);

                clientEngineState = TyrusClientEngineState.UPGRADE_REQUEST_CREATED;
                logUpgradeRequest(requestContext);
                return requestContext;
            }
            case AUTH_REQUIRED: {
                UpgradeRequest upgradeRequest = clientHandShake.getRequest();

                if (clientEngineState.getAuthenticator() != null) {

                    if (LOGGER.isLoggable(Level.CONFIG)) {
                        debugContext.appendLogMessage(LOGGER, Level.CONFIG, DebugContext.Type.MESSAGE_OUT, "Using "
                                + "authenticator: ", clientEngineState.getAuthenticator().getClass().getName());
                    }

                    String authorizationHeader;
                    try {
                        final Credentials credentials = (Credentials) properties.get(ClientProperties.CREDENTIALS);
                        debugContext.appendLogMessage(LOGGER, Level.CONFIG, DebugContext.Type.MESSAGE_OUT, "Using "
                                + "credentials: ", credentials);
                        authorizationHeader =
                                clientEngineState.getAuthenticator()
                                                 .generateAuthorizationHeader(
                                                         upgradeRequest.getRequestURI(),
                                                         clientEngineState.getWwwAuthenticateHeader(),
                                                         credentials);
                    } catch (AuthenticationException e) {
                        listener.onError(e);
                        return null;
                    }
                    upgradeRequest.getHeaders().put(UpgradeRequest.AUTHORIZATION,
                                                    Collections.singletonList(authorizationHeader));
                }

                clientEngineState = TyrusClientEngineState.AUTH_UPGRADE_REQUEST_CREATED;
                logUpgradeRequest(upgradeRequest);
                return upgradeRequest;
            }

            default:
                redirectUriHistory.clear();
                throw new IllegalStateException();
        }
    }

    @Override
    public ClientUpgradeInfo processResponse(final UpgradeResponse upgradeResponse,
                                             final Writer writer, final Connection.CloseListener closeListener) {

        if (LOGGER.isLoggable(Level.FINE)) {
            debugContext.appendLogMessage(LOGGER, Level.FINE, DebugContext.Type.MESSAGE_IN, "Received handshake "
                    + "response: \n" + Utils.stringifyUpgradeResponse(upgradeResponse));
        } else {
            if (logUpgradeMessages) {
                debugContext.appendStandardOutputMessage(DebugContext.Type.MESSAGE_IN, "Received handshake response: "
                        + "\n" + Utils.stringifyUpgradeResponse(upgradeResponse));
            }
        }

        if (clientEngineState == TyrusClientEngineState.AUTH_UPGRADE_REQUEST_CREATED
                || clientEngineState == TyrusClientEngineState.UPGRADE_REQUEST_CREATED) {

            if (upgradeResponse == null) {
                throw new IllegalArgumentException(LocalizationMessages.ARGUMENT_NOT_NULL("upgradeResponse"));
            }

            switch (upgradeResponse.getStatus()) {
                case 101:
                    return handleSwitchProtocol(upgradeResponse, writer, closeListener);
                case 300:
                case 301:
                case 302:
                case 303:
                case 307:
                case 308:
                    return handleRedirect(upgradeResponse);
                case 401:
                    return handleAuth(upgradeResponse);
                case 503:

                    // get Retry-After header
                    String retryAfterString = null;
                    final List<String> retryAfterHeader = upgradeResponse.getHeaders()
                                                                         .get(UpgradeResponse.RETRY_AFTER);
                    if (retryAfterHeader != null) {
                        retryAfterString = Utils.getHeaderFromList(retryAfterHeader);
                    }

                    Long delay;
                    if (retryAfterString != null) {
                        try {
                            // parse http date
                            Date date = Utils.parseHttpDate(retryAfterString);
                            delay = (date.getTime() - System.currentTimeMillis()) / 1000;
                        } catch (ParseException e) {
                            try {
                                // it could be interval in seconds
                                delay = Long.parseLong(retryAfterString);
                            } catch (NumberFormatException iae) {
                                delay = null;
                            }
                        }
                    } else {
                        delay = null;
                    }

                    listener.onError(new RetryAfterException(
                            LocalizationMessages.HANDSHAKE_HTTP_RETRY_AFTER_MESSAGE(), delay));
                    return UPGRADE_INFO_FAILED;
                default:
                    clientEngineState = TyrusClientEngineState.FAILED;
                    HandshakeException e = new HandshakeException(
                            upgradeResponse.getStatus(),
                            LocalizationMessages.INVALID_RESPONSE_CODE(101, upgradeResponse.getStatus()));
                    listener.onError(e);
                    redirectUriHistory.clear();
                    return UPGRADE_INFO_FAILED;
            }

        }

        redirectUriHistory.clear();
        throw new IllegalStateException();
    }

    private ClientUpgradeInfo handleSwitchProtocol(UpgradeResponse upgradeResponse, Writer writer,
                                                   Connection.CloseListener closeListener) {
        // the connection has been upgraded
        clientEngineState = TyrusClientEngineState.SUCCESS;

        try {
            return processUpgradeResponse(upgradeResponse, writer, closeListener);
        } catch (HandshakeException e) {
            clientEngineState = TyrusClientEngineState.FAILED;
            listener.onError(e);
            return UPGRADE_INFO_FAILED;
        } finally {
            redirectUriHistory.clear();
        }
    }

    private ClientUpgradeInfo handleAuth(UpgradeResponse upgradeResponse) {
        if (clientEngineState == TyrusClientEngineState.AUTH_UPGRADE_REQUEST_CREATED) {
            clientEngineState = TyrusClientEngineState.FAILED;
            listener.onError(new AuthenticationException(LocalizationMessages.AUTHENTICATION_FAILED()));
            return UPGRADE_INFO_FAILED;
        }

        AuthConfig authConfig = Utils.getProperty(properties, ClientProperties.AUTH_CONFIG,
                                                  AuthConfig.class,
                                                  AuthConfig.Builder.create().build());
        debugContext.appendLogMessage(LOGGER, Level.FINE, DebugContext.Type.MESSAGE_OUT, "Using "
                + "authentication config: ", authConfig);
        if (authConfig == null) {
            clientEngineState = TyrusClientEngineState.FAILED;
            listener.onError(new AuthenticationException(LocalizationMessages.AUTHENTICATION_FAILED()));
            return UPGRADE_INFO_FAILED;
        }

        String wwwAuthenticateHeader = null;
        final List<String> authHeader = upgradeResponse.getHeaders().get(UpgradeResponse
                                                                                 .WWW_AUTHENTICATE);
        if (authHeader != null) {
            wwwAuthenticateHeader = Utils.getHeaderFromList(authHeader);
        }

        if (wwwAuthenticateHeader == null || wwwAuthenticateHeader.equals("")) {
            clientEngineState = TyrusClientEngineState.FAILED;
            listener.onError(new AuthenticationException(LocalizationMessages.AUTHENTICATION_FAILED()));
            return UPGRADE_INFO_FAILED;
        }

        final String[] tokens = wwwAuthenticateHeader.trim().split("\\s+", 2);
        final String scheme = tokens[0];

        debugContext.appendLogMessage(LOGGER, Level.FINE, DebugContext.Type.MESSAGE_OUT, "Using "
                + "authentication scheme: ", scheme);
        final Authenticator authenticator = authConfig.getAuthenticators().get(scheme);
        if (authenticator == null) {
            clientEngineState = TyrusClientEngineState.FAILED;
            listener.onError(new AuthenticationException(LocalizationMessages.AUTHENTICATION_FAILED()));
            return UPGRADE_INFO_FAILED;
        }

        clientEngineState = TyrusClientEngineState.AUTH_REQUIRED;
        clientEngineState.setAuthenticator(authenticator);
        clientEngineState.setWwwAuthenticateHeader(wwwAuthenticateHeader);

        return UPGRADE_INFO_ANOTHER_REQUEST_REQUIRED;
    }

    private ClientUpgradeInfo handleRedirect(UpgradeResponse upgradeResponse) {
        if (!redirectEnabled) {
            clientEngineState = TyrusClientEngineState.FAILED;
            listener.onError(new RedirectException(upgradeResponse.getStatus(), LocalizationMessages
                    .HANDSHAKE_HTTP_REDIRECTION_NOT_ENABLED(upgradeResponse.getStatus())));
            return UPGRADE_INFO_FAILED;
        }

        // get location header
        String locationString = null;
        final List<String> locationHeader = upgradeResponse.getHeaders().get(UpgradeResponse.LOCATION);
        if (locationHeader != null) {
            locationString = Utils.getHeaderFromList(locationHeader);
        }

        if (locationString == null || locationString.equals("")) {
            listener.onError(new RedirectException(upgradeResponse.getStatus(), LocalizationMessages
                    .HANDSHAKE_HTTP_REDIRECTION_NEW_LOCATION_MISSING()));
            clientEngineState = TyrusClientEngineState.FAILED;
            return UPGRADE_INFO_FAILED;
        }

        // location header could contain http scheme
        URI location;
        try {
            location = new URI(locationString);
            String scheme = location.getScheme();
            if ("http".equalsIgnoreCase(scheme)) {
                scheme = "ws";
            }
            if ("https".equalsIgnoreCase(scheme)) {
                scheme = "wss";
            }
            int port = Utils.getWsPort(location, scheme);
            location = new URI(scheme, location.getUserInfo(), location.getHost(), port, location
                    .getPath(), location.getQuery(), location.getFragment());

            if (!location.isAbsolute()) {
                // location is not absolute, we need to resolve it.
                URI baseUri = redirectLocation == null ? connectToServerUriParam : redirectLocation;
                location = baseUri.resolve(location.normalize());

                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.finest("HTTP Redirect - Base URI for resolving target location: " + baseUri);
                    LOGGER.finest("HTTP Redirect - Location URI header: " + locationString);
                    LOGGER.finest("HTTP Redirect - Normalized and resolved Location URI header "
                                          + "against base URI: " + location);
                }
            }
        } catch (URISyntaxException e) {
            clientEngineState = TyrusClientEngineState.FAILED;
            listener.onError(new RedirectException(
                    upgradeResponse.getStatus(),
                    LocalizationMessages.HANDSHAKE_HTTP_REDIRECTION_NEW_LOCATION_ERROR(locationString)));
            return UPGRADE_INFO_FAILED;
        }

        // infinite loop detection
        boolean alreadyRequested = !redirectUriHistory.add(location);
        if (alreadyRequested) {
            clientEngineState = TyrusClientEngineState.FAILED;
            listener.onError(new RedirectException(
                    upgradeResponse.getStatus(),
                    LocalizationMessages.HANDSHAKE_HTTP_REDIRECTION_INFINITE_LOOP()));
            return UPGRADE_INFO_FAILED;
        }

        // maximal number of redirection
        if (redirectUriHistory.size() > redirectThreshold) {
            clientEngineState = TyrusClientEngineState.FAILED;
            listener.onError(new RedirectException(
                    upgradeResponse.getStatus(),
                    LocalizationMessages.HANDSHAKE_HTTP_REDIRECTION_MAX_REDIRECTION(redirectThreshold)));
            return UPGRADE_INFO_FAILED;
        }

        clientEngineState = TyrusClientEngineState.REDIRECT_REQUIRED;
        redirectLocation = location;
        return UPGRADE_INFO_ANOTHER_REQUEST_REQUIRED;
    }

    @Override
    public void processError(Throwable t) {
        if (clientEngineState == TyrusClientEngineState.SUCCESS) {
            throw new IllegalStateException();
        }

        if (clientEngineState != TyrusClientEngineState.FAILED) {
            clientEngineState = TyrusClientEngineState.FAILED;
            listener.onError(t);
        }
    }

    private void logUpgradeRequest(UpgradeRequest upgradeRequest) {
        if (LOGGER.isLoggable(Level.FINE)) {
            debugContext.appendLogMessage(LOGGER, Level.FINE, DebugContext.Type.MESSAGE_OUT, "Sending handshake "
                    + "request:\n" + Utils.stringifyUpgradeRequest(upgradeRequest));
        } else {
            if (logUpgradeMessages) {
                debugContext.appendStandardOutputMessage(DebugContext.Type.MESSAGE_OUT, "Sending handshake "
                        + "request:\n" + Utils.stringifyUpgradeRequest(upgradeRequest));
            }
        }
    }

    /**
     * Process upgrade response. This method should be called only when the response HTTP status code is {@code 101}.
     *
     * @param upgradeResponse upgrade response received from client container.
     * @param writer          writer instance to be used for sending websocket frames.
     * @param closeListener   client container connection listener.
     * @return client upgrade info with {@link ClientUpgradeStatus#SUCCESS} status.
     * @throws HandshakeException when there is a problem with passed {@link UpgradeResponse}.
     */
    private ClientUpgradeInfo processUpgradeResponse(UpgradeResponse upgradeResponse, final Writer writer, final
    Connection.CloseListener closeListener) throws HandshakeException {
        clientHandShake.validateServerResponse(upgradeResponse);

        final TyrusWebSocket socket = new TyrusWebSocket(protocolHandler, endpointWrapper);
        final List<Extension> handshakeResponseExtensions = TyrusExtension.fromHeaders(
                upgradeResponse.getHeaders().get(HandshakeRequest.SEC_WEBSOCKET_EXTENSIONS));
        final List<Extension> extensions = new ArrayList<Extension>();

        final ExtendedExtension.ExtensionContext extensionContext = new ExtendedExtension.ExtensionContext() {

            private final Map<String, Object> properties = new HashMap<String, Object>();

            @Override
            public Map<String, Object> getProperties() {
                return properties;
            }
        };

        for (Extension responseExtension : handshakeResponseExtensions) {
            for (Extension installedExtension : ((ClientEndpointConfig) endpointWrapper.getEndpointConfig())
                    .getExtensions()) {
                final String responseExtensionName = responseExtension.getName();
                if (responseExtensionName != null && responseExtensionName.equals(installedExtension.getName())) {

                    /**
                     * @see TyrusServerEndpointConfigurator#getNegotiatedExtensions(...)
                     */
                    boolean alreadyAdded = false;

                    for (Extension extension : extensions) {
                        if (extension.getName().equals(responseExtensionName)) {
                            alreadyAdded = true;
                        }
                    }

                    if (!alreadyAdded) {
                        if (installedExtension instanceof ExtendedExtension) {
                            ((ExtendedExtension) installedExtension)
                                    .onHandshakeResponse(extensionContext, responseExtension.getParameters());
                        }

                        extensions.add(installedExtension);
                        debugContext.appendLogMessage(LOGGER, Level.FINE, DebugContext.Type.OTHER, "Installed "
                                + "extension: ", installedExtension.getName());
                    }
                }
            }
        }

        final Session sessionForRemoteEndpoint =
                endpointWrapper.createSessionForRemoteEndpoint(
                        socket, upgradeResponse.getFirstHeaderValue(HandshakeRequest.SEC_WEBSOCKET_PROTOCOL),
                        extensions, debugContext);

        ((ClientEndpointConfig) endpointWrapper.getEndpointConfig()).getConfigurator().afterResponse(upgradeResponse);

        protocolHandler.setWriter(writer);
        protocolHandler.setWebSocket(socket);
        protocolHandler.setExtensions(extensions);
        protocolHandler.setExtensionContext(extensionContext);

        // subprotocol and extensions are already set -- TODO: introduce new method (onClientConnect)?
        socket.onConnect(this.clientHandShake.getRequest(), null, null, null, debugContext);

        listener.onSessionCreated(sessionForRemoteEndpoint);

        // incoming buffer size - max frame size possible to receive.
        Integer tyrusIncomingBufferSize = Utils.getProperty(properties, ClientProperties.INCOMING_BUFFER_SIZE,
                                                            Integer.class);
        Integer wlsIncomingBufferSize = Utils.getProperty(endpointWrapper.getEndpointConfig().getUserProperties(),
                                                          ClientContainer.WLS_INCOMING_BUFFER_SIZE, Integer.class);
        final Integer incomingBufferSize;
        if (tyrusIncomingBufferSize == null && wlsIncomingBufferSize == null) {
            incomingBufferSize = DEFAULT_INCOMING_BUFFER_SIZE;
        } else if (wlsIncomingBufferSize != null) {
            incomingBufferSize = wlsIncomingBufferSize;
        } else {
            incomingBufferSize = tyrusIncomingBufferSize;
        }

        debugContext.appendLogMessage(LOGGER, Level.FINE, DebugContext.Type.OTHER, "Incoming buffer size: ",
                                      incomingBufferSize);

        return new ClientUpgradeInfo() {
            @Override
            public ClientUpgradeStatus getUpgradeStatus() {
                return ClientUpgradeStatus.SUCCESS;
            }

            @Override
            public Connection createConnection() {
                return new Connection() {

                    private final ReadHandler readHandler =
                            new TyrusReadHandler(protocolHandler, socket, incomingBufferSize,
                                                 sessionForRemoteEndpoint.getNegotiatedExtensions(), extensionContext);

                    @Override
                    public ReadHandler getReadHandler() {
                        return readHandler;
                    }

                    @Override
                    public Writer getWriter() {
                        return writer;
                    }

                    @Override
                    public CloseListener getCloseListener() {
                        return closeListener;
                    }

                    @Override
                    public void close(CloseReason reason) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, e.getMessage(), e);
                        }

                        socket.close(reason.getCloseCode().getCode(), reason.getReasonPhrase());

                        for (Extension extension : sessionForRemoteEndpoint.getNegotiatedExtensions()) {
                            if (extension instanceof ExtendedExtension) {
                                ((ExtendedExtension) extension).destroy(extensionContext);
                            }
                        }

                    }
                };

            }
        };
    }

    /**
     * Get {@link TimeoutHandler} associated with current {@link ClientEngine} instance.
     *
     * @return timeout handler instance or {@code null} when not present.
     */
    public TimeoutHandler getTimeoutHandler() {
        return timeoutHandler;
    }

    /**
     * Called when response is received from the server.
     */
    public static interface ClientHandshakeListener {

        /**
         * Invoked when handshake is completed and provided {@link Session} is open and ready to be returned from
         * {@link
         * WebSocketContainer#connectToServer(Class, jakarta.websocket.ClientEndpointConfig, java.net.URI)} (and
         * alternatives) call.
         *
         * @param session opened client session.
         */
        public void onSessionCreated(Session session);


        /**
         * Called when an error is found in handshake response.
         *
         * @param exception error found during handshake response check.
         */
        public void onError(Throwable exception);
    }

    private static class TyrusReadHandler implements ReadHandler {

        private final int incomingBufferSize;
        private final ProtocolHandler handler;
        private final TyrusWebSocket socket;
        private final List<Extension> negotiatedExtensions;
        private final ExtendedExtension.ExtensionContext extensionContext;

        private ByteBuffer buffer = null;

        TyrusReadHandler(final ProtocolHandler protocolHandler, final TyrusWebSocket socket, int incomingBufferSize,
                         List<Extension> negotiatedExtensions, ExtendedExtension.ExtensionContext extensionContext) {
            this.handler = protocolHandler;
            this.socket = socket;
            this.incomingBufferSize = incomingBufferSize;
            this.negotiatedExtensions = negotiatedExtensions;
            this.extensionContext = extensionContext;

            protocolHandler.setExtensionContext(extensionContext);
        }

        @Override
        public void handle(ByteBuffer data) {
            try {
                if (data != null && data.hasRemaining()) {

                    if (buffer != null) {
                        data = Utils.appendBuffers(buffer, data, incomingBufferSize, BUFFER_STEP_SIZE);
                    } else {
                        int newSize = data.remaining();
                        if (newSize > incomingBufferSize) {
                            throw new IllegalArgumentException("Buffer overflow.");
                        } else {
                            final int roundedSize = (newSize % BUFFER_STEP_SIZE) > 0 ? ((newSize / BUFFER_STEP_SIZE)
                                    + 1) * BUFFER_STEP_SIZE : newSize;
                            final ByteBuffer result = ByteBuffer.allocate(roundedSize > incomingBufferSize ? newSize
                                                                                  : roundedSize);
                            ((Buffer) result).flip();
                            data = Utils.appendBuffers(result, data, incomingBufferSize, BUFFER_STEP_SIZE);
                        }
                    }

                    do {
                        Frame frame = handler.unframe(data);
                        if (frame == null) {
                            buffer = data;
                            break;
                        } else {
                            for (Extension extension : negotiatedExtensions) {
                                if (extension instanceof ExtendedExtension) {
                                    try {
                                        frame = ((ExtendedExtension) extension)
                                                .processIncoming(extensionContext, frame);
                                    } catch (Throwable t) {
                                        LOGGER.log(
                                                Level.FINE,
                                                String.format(
                                                        "Extension '%s' threw an exception during processIncoming "
                                                                + "method invocation: \"%s\".",
                                                        extension.getName(), t.getMessage()), t);
                                    }
                                }
                            }

                            handler.process(frame, socket);
                        }
                    } while (true);
                }
            } catch (WebSocketException e) {
                LOGGER.log(Level.FINE, e.getMessage(), e);
                socket.onClose(new CloseFrame(e.getCloseReason()));
            } catch (Exception e) {
                LOGGER.log(Level.FINE, e.getMessage(), e);
                socket.onClose(new CloseFrame(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, e
                        .getMessage())));
            }
        }
    }

    private static final ClientUpgradeInfo UPGRADE_INFO_FAILED = new ClientUpgradeInfo() {

        @Override
        public ClientUpgradeStatus getUpgradeStatus() {
            return ClientUpgradeStatus.UPGRADE_REQUEST_FAILED;
        }

        @Override
        public Connection createConnection() {
            return null;
        }
    };

    private static final ClientUpgradeInfo UPGRADE_INFO_ANOTHER_REQUEST_REQUIRED = new ClientUpgradeInfo() {

        @Override
        public ClientUpgradeStatus getUpgradeStatus() {
            return ClientUpgradeStatus.ANOTHER_UPGRADE_REQUEST_REQUIRED;
        }

        @Override
        public Connection createConnection() {
            return null;
        }
    };

    /**
     * State controls flow in {@link #processResponse(UpgradeResponse, Writer, Connection.CloseListener)}
     * and depends on upgrade response status code and previous state.
     */
    private static enum TyrusClientEngineState {

        /**
         * Initial state.
         */
        INIT,

        /**
         * Upgrade request must be redirected.
         * <p/>
         * Set in {@link #processResponse(UpgradeResponse, Writer, Connection.CloseListener)} when 3xx HTTP status code
         * is received.
         */
        REDIRECT_REQUIRED,

        /**
         * Authentication required.
         * <p/>
         * Set in {@link #processResponse(UpgradeResponse, Writer, Connection.CloseListener)} when 401 HTTP status code
         * is received and the last upgrade request does not contain {@value UpgradeRequest#AUTHORIZATION} header
         * (the last state was not {@link #AUTH_UPGRADE_REQUEST_CREATED}).
         */
        AUTH_REQUIRED,

        /**
         * Upgrade request with {@value UpgradeRequest#AUTHORIZATION} header has been created.
         * <p/>
         * Set in {@link #createUpgradeRequest(TimeoutHandler)}.
         */
        AUTH_UPGRADE_REQUEST_CREATED,

        /**
         * Upgrade request has been created.
         * <p/>
         * Set in {@link #createUpgradeRequest(TimeoutHandler)}.
         */
        UPGRADE_REQUEST_CREATED,

        /**
         * Handshake failed (final state).
         */
        FAILED,

        /**
         * Handshake succeeded (final state).
         */
        SUCCESS;

        private volatile Authenticator authenticator;
        private volatile String wwwAuthenticateHeader;

        Authenticator getAuthenticator() {
            return authenticator;
        }

        void setAuthenticator(Authenticator authenticator) {
            this.authenticator = authenticator;
        }

        String getWwwAuthenticateHeader() {
            return wwwAuthenticateHeader;
        }

        void setWwwAuthenticateHeader(String wwwAuthenticateHeader) {
            this.wwwAuthenticateHeader = wwwAuthenticateHeader;
        }
    }
}
