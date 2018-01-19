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

package org.glassfish.tyrus.spi;

import javax.websocket.server.HandshakeRequest;

/**
 * Abstraction for a HTTP upgrade request. A transport creates an implementation
 * for this and uses {@link WebSocketEngine#upgrade} method to upgrade the
 * request.
 *
 * @author Danny Coward (danny.coward at oracle.com)
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public abstract class UpgradeRequest implements HandshakeRequest {

    /**
     * Expected value in HTTP handshake "Upgrade" header.
     * <p>
     * (Registered in RFC 6455).
     */
    public static final String WEBSOCKET = "websocket";

    /**
     * HTTP reason phrase for successful handshake response.
     */
    public static final String RESPONSE_CODE_MESSAGE = "Switching Protocols";

    /**
     * HTTP "Upgrade" header name and "Connection" header expected value.
     */
    public static final String UPGRADE = "Upgrade";

    /**
     * HTTP "Connection" header name.
     */
    public static final String CONNECTION = "Connection";

    /**
     * HTTP "Host" header name.
     */
    public static final String HOST = "Host";

    /**
     * WebSocket origin header name from previous versions.
     * <p>
     * Keeping here only for backwards compatibility, not used anymore.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static final String SEC_WS_ORIGIN_HEADER = "Sec-WebSocket-Origin";

    /**
     * HTTP "Origin" header name.
     */
    public static final String ORIGIN_HEADER = "Origin";

    /**
     * Tyrus cluster connection ID header name.
     */
    public static final String CLUSTER_CONNECTION_ID_HEADER = "tyrus-cluster-connection-id";

    /**
     * Server key hash used to compute "Sec-WebSocket-Accept" header value.
     * <p>
     * Defined in RFC 6455.
     */
    public static final String SERVER_KEY_HASH = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    /**
     * HTTP "Authorization" header name.
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * If this header is present in the handshake request and the tracing type is configured to "ON_DEMAND", tracing
     * headers will be sent in the handshake response. The value of the header is no taken into account.
     * <p>
     * Setting this header does not have any effect if the tracing type is configured to "ALL" or "OFF".
     */
    public static final String ENABLE_TRACING_HEADER = "X-Tyrus-Tracing-Accept";

    /**
     * This header allows temporarily changing tracing threshold. If present in the handshake request, the tracing
     * threshold will be changed for the handshake the request is part of.
     * <p>
     * The expected values are "SUMMARY" or "TRACE", of which "TRACE" will provide more fine-grained information.
     */
    public static final String TRACING_THRESHOLD = "X-Tyrus-Tracing-Threshold";

    /**
     * Returns the value of the specified request header name. If there are
     * multiple headers with the same name, this method returns the first
     * header in the request. The header name is case insensitive.
     *
     * @param name a header name.
     * @return value of the specified header name,
     * null if the request doesn't have a header of that name.
     */
    public abstract String getHeader(String name);

    /**
     * Get the undecoded request uri (up to the query string) of underlying
     * HTTP handshake request.
     *
     * @return request uri.
     */
    public abstract String getRequestUri();

    /**
     * Indicates whether this request was made using a secure channel
     * (such as HTTPS).
     *
     * @return true if the request was made using secure channel,
     * false otherwise.
     */
    public abstract boolean isSecure();
}
