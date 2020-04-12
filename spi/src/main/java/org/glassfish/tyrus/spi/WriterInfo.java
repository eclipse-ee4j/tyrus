/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Additional data for the transport.
 * @since 1.17
 */
public final class WriterInfo {

    /**
     * Possible WebSocket Message Types.
     */
    public static enum MessageType {
        /**
         * Text type.
         */
        TEXT,
        /**
         * Continuation text type.
         */
        TEXT_CONTINUATION,
        /**
         * Binary type.
         */
        BINARY,
        /**
         * Continuation binary type.
         */
        BINARY_CONTINUATION,
        /**
         * Object type.
         */
        OBJECT,
        /**
         * Ping type.
         */
        PING,
        /**
         * Pong type.
         */
        PONG,
        /**
         * Close type.
         */
        CLOSE
    }

    public static enum RemoteEndpointType {
        /**
         * RemoteEndpoint.Async
         */
        ASYNC,
        /**
         * RemoteEndpoint.Basic
         */
        BASIC,
        /**
         * Broadcast
         */
        BROADCAST,
        /**
         * RemoteEndpoint
         */
        SUPER
    }

    private final MessageType messageType;
    private final RemoteEndpointType remoteEndpointType;

    /**
     * Create new {@code WriterData} instance
     * @param messageType         The outbound message type
     * @param remoteEndpointType  The outbound message remote endpoint
     */
    public WriterInfo(MessageType messageType, RemoteEndpointType remoteEndpointType) {
        this.messageType = messageType;
        this.remoteEndpointType = remoteEndpointType;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public RemoteEndpointType getRemoteEndpointType() {
        return remoteEndpointType;
    }
}
