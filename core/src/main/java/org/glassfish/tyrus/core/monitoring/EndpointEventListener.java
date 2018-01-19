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

package org.glassfish.tyrus.core.monitoring;

import org.glassfish.tyrus.core.Beta;

/**
 * Listens to endpoint-level events that are interesting for monitoring.
 *
 * @author Petr Janouch
 */
@Beta
public interface EndpointEventListener {

    /**
     * Called when a session has been opened.
     *
     * @param sessionId an ID of the newly opened session.
     * @return listener that listens for message-level events.
     */
    MessageEventListener onSessionOpened(String sessionId);

    /**
     * Called when a session has been closed.
     *
     * @param sessionId an ID of the closed session.
     */
    void onSessionClosed(String sessionId);

    /**
     * Called when an error has occurred.
     * <p>
     * Errors that occur either during {@link javax.websocket.Endpoint#onOpen(javax.websocket.Session,
     * javax.websocket.EndpointConfig)}, {@link javax.websocket.Endpoint#onClose(javax.websocket.Session,
     * javax.websocket.CloseReason)} and their annotated equivalent or when handling an incoming message, cause this
     * listener to be called. It corresponds to the event of invocation of {@link javax.websocket.Endpoint#onError
     * (javax.websocket.Session, Throwable)} and its annotated equivalent.
     *
     * @param sessionId an ID of the session on which the error occurred.
     * @param t         throwable that has been thrown.
     */
    void onError(String sessionId, Throwable t);

    /**
     * An instance of @EndpointEventListener that does not do anything.
     */
    public static final EndpointEventListener NO_OP = new EndpointEventListener() {
        @Override
        public MessageEventListener onSessionOpened(String sessionId) {
            return MessageEventListener.NO_OP;
        }

        @Override
        public void onSessionClosed(String sessionId) {
            // do nothing
        }

        @Override
        public void onError(String sessionId, Throwable t) {
            // do nothing
        }
    };
}
