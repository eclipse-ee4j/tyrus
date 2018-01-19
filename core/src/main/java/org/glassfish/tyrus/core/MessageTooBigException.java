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

package org.glassfish.tyrus.core;

import javax.websocket.CloseReason;

/**
 * Thrown when {@link javax.websocket.OnMessage#maxMessageSize()} is smaller than received message size.
 * <p>
 * Underlying web socket connection will be closed with {@link javax.websocket.CloseReason.CloseCode}
 * {@link javax.websocket.CloseReason.CloseCodes#TOO_BIG} and {@link javax.websocket.OnError} annotated method (or
 * {@link javax.websocket.Endpoint#onError(javax.websocket.Session, Throwable)} will be called with instance of this
 * class as {@link Throwable} parameter.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
class MessageTooBigException extends WebSocketException {

    private static final CloseReason CLOSE_REASON = CloseReasons.TOO_BIG.getCloseReason();
    private static final long serialVersionUID = -1636733948291376261L;

    MessageTooBigException(String message) {
        super(message);
    }

    @Override
    public CloseReason getCloseReason() {
        return CLOSE_REASON;
    }
}
