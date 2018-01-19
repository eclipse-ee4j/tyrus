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

package org.glassfish.tyrus.spi;

import javax.websocket.CloseReason;

/**
 * A logical websocket connection. Tyrus creates this connection after
 * successful upgrade and gets data from {@link ReadHandler} and writes data
 * to {@link Writer}.
 */
public interface Connection {

    /**
     * Returns a read handler. A transport can pass websocket data to
     * tyrus using the handler.
     *
     * @return tryus read handler that handles websocket data.
     */
    ReadHandler getReadHandler();

    /**
     * Returns the same writer that is passed for creating connection in
     * {@link WebSocketEngine.UpgradeInfo#createConnection(Writer, CloseListener)}
     * The transport writer that actually writes websocket data
     * to underlying connection.
     *
     * @return transport writer that actually writes websocket data
     * to underlying connection.
     */
    Writer getWriter();

    /**
     * Returns the same close listener that is passed for creating connection in
     * {@link WebSocketEngine.UpgradeInfo#createConnection(Writer, CloseListener)}.
     * <p>
     * This transport close listener receives connection close notifications
     * from Tyrus.
     *
     * @return close listener provided when the connection is created.
     */
    CloseListener getCloseListener();

    /**
     * Notifies tyrus that underlying transport is closing the connection.
     *
     * @param reason for closing the actual connection.
     */
    void close(CloseReason reason);

    /**
     * Transport close listener that receives connection close
     * notifications from Tyrus.
     */
    interface CloseListener {

        /**
         * Tyrus notifies that logical connection is closed.
         *
         * @param reason for closing the connection.
         */
        void close(CloseReason reason);
    }
}
