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

/**
 * Facade for handling client operations from containers.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
public interface ClientEngine {

    /**
     * Create upgrade request and register {@link TimeoutHandler}.
     *
     * @param timeoutHandler handshake timeout handler. {@link TimeoutHandler#handleTimeout()} is invoked if {@link
     *                       #processResponse(UpgradeResponse, Writer, Connection.CloseListener)} is not called within
     *                       handshake timeout.
     * @return request to be send on the wire or {@code null}, when the request cannot be created. When {@code null} is
     * returned, client should free all resources tied to current connection.
     */
    UpgradeRequest createUpgradeRequest(TimeoutHandler timeoutHandler);

    /**
     * Process handshake and return {@link ClientUpgradeInfo} with handshake status ({@link ClientUpgradeStatus}).
     *
     * @param upgradeResponse response to be processed.
     * @param writer          used for sending dataframes from client endpoint.
     * @param closeListener   will be called when connection is closed, will be set as listener of returned {@link
     *                        Connection}.
     * @return info with upgrade status.
     * @see #processError(Throwable)
     */
    ClientUpgradeInfo processResponse(UpgradeResponse upgradeResponse, final Writer writer,
                                      final Connection.CloseListener closeListener);

    /**
     * Process error.
     * <p>
     * This method can be called any time when client encounters an error which cannot be handled in the container
     * before {@link ClientUpgradeStatus#SUCCESS} is returned from {@link #processResponse(UpgradeResponse, Writer,
     * Connection.CloseListener)}.
     *
     * @param t encountered error.
     * @see #processResponse(UpgradeResponse, Writer, Connection.CloseListener)
     */
    void processError(Throwable t);

    /**
     * Indicates to container that handshake timeout was reached.
     */
    interface TimeoutHandler {
        /**
         * Invoked when timeout is reached. Container is supposed to clean all resources related to {@link
         * ClientEngine}
         * instance.
         */
        void handleTimeout();
    }

    /**
     * Upgrade process result.
     * <p>
     * Provides information about upgrade process. There are three possible states which can be reported:
     * <ul>
     * <li>{@link ClientUpgradeStatus#ANOTHER_UPGRADE_REQUEST_REQUIRED}</li>
     * <li>{@link ClientUpgradeStatus#UPGRADE_REQUEST_FAILED}</li>
     * <li>{@link ClientUpgradeStatus#SUCCESS}</li>
     * </ul>
     * <p>
     * When {@link #getUpgradeStatus()} returns {@link ClientUpgradeStatus#SUCCESS}, client container can create
     * {@link Connection} and start processing read events from the underlying connection and report them to Tyrus
     * runtime.
     * <p>
     * When {@link #getUpgradeStatus()} returns {@link ClientUpgradeStatus#UPGRADE_REQUEST_FAILED}, client container
     * HAS TO close all resources related to currently processed {@link UpgradeResponse}.
     * <p>
     * When {@link #getUpgradeStatus()} returns {@link ClientUpgradeStatus#ANOTHER_UPGRADE_REQUEST_REQUIRED}, client
     * container HAS TO close all resources related to currently processed {@link UpgradeResponse}, open new TCP
     * connection and send {@link UpgradeRequest} obtained from method {@link #createUpgradeRequest(TimeoutHandler)}.
     */
    interface ClientUpgradeInfo {

        /**
         * Get {@link ClientUpgradeStatus}.
         *
         * @return {@link ClientUpgradeStatus}.
         */
        ClientUpgradeStatus getUpgradeStatus();

        /**
         * Create new {@link Connection} when {@link #getUpgradeStatus()} returns {@link ClientUpgradeStatus#SUCCESS}.
         *
         * @return new {@link Connection} instance or {@code null}, when {@link #getUpgradeStatus()} does not return
         * {@link ClientUpgradeStatus}.
         */
        Connection createConnection();
    }

    /**
     * Status of upgrade process.
     * <p>
     * Returned by {@link #processResponse(UpgradeResponse, Writer, Connection.CloseListener)}.
     */
    enum ClientUpgradeStatus {

        /**
         * Client engine needs to send another request.
         *
         * @see #createUpgradeRequest(TimeoutHandler)
         */
        ANOTHER_UPGRADE_REQUEST_REQUIRED,

        /**
         * Upgrade process failed.
         */
        UPGRADE_REQUEST_FAILED,

        /**
         * Upgrade process was successful.
         *
         * @see ClientUpgradeInfo#createConnection()
         */
        SUCCESS
    }
}
