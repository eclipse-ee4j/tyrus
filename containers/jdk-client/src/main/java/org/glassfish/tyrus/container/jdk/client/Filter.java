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

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.glassfish.tyrus.spi.CompletionHandler;

/**
 * A filter can add functionality to JDK client transport. Filters are composed together to
 * create JDK client transport.
 *
 * @author Petr Janouch
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
class Filter {

    protected volatile Filter upstreamFilter = null;
    protected final Filter downstreamFilter;

    /**
     * Constructor.
     *
     * @param downstreamFilter downstream filter. Accessible directly as {@link #downstreamFilter} protected field.
     */
    Filter(Filter downstreamFilter) {
        this.downstreamFilter = downstreamFilter;
    }

    /**
     * Perform write operation for this filter and invokes write method on the next filter in the filter chain.
     *
     * @param data              on which write operation is performed.
     * @param completionHandler will be invoked when the write operation is completed or has failed.
     */
    void write(ByteBuffer data, CompletionHandler<ByteBuffer> completionHandler) {
    }

    /**
     * Close the filter, invokes close operation on the next filter in the filter chain.
     * <p/>
     * The filter is expected to clean up any allocated resources and pass the invocation to downstream filter.
     */
    void close() {
    }

    /**
     * Signal to turn on SSL, it is passed on in the filter chain until a filter responsible for SSL is reached.
     */
    void startSsl() {
    }

    /**
     * Initiate connect.
     * <p/>
     * If the {@link Filter} needs to do something during this phase, it must implement {@link
     * #handleConnect(SocketAddress, Filter)} method.
     *
     * @param address        an address where to connect (server or proxy).
     * @param upstreamFilter a filter positioned upstream.
     */
    final void connect(SocketAddress address, Filter upstreamFilter) {
        this.upstreamFilter = upstreamFilter;

        handleConnect(address, upstreamFilter);

        if (downstreamFilter != null) {
            downstreamFilter.connect(address, this);
        }
    }

    /**
     * An event listener that is called when a connection is set up.
     * This event travels up in the filter chain.
     * <p/>
     * If the {@link Filter} needs to process this event, it must implement {@link #processConnect()} method.
     */
    final void onConnect() {
        processConnect();

        if (upstreamFilter != null) {
            upstreamFilter.onConnect();
        }
    }

    /**
     * An event listener that is called when some data is read.
     * <p/>
     * If the {@link Filter} needs to process this event, it must implement {@link #processRead(ByteBuffer)} ()} method.
     * If the method returns {@code true}, the processing will continue with upstream filters; if the method invocation
     * returns {@code false}, the processing won't continue.
     *
     * @param data that has been read.
     */
    final void onRead(ByteBuffer data) {
        if (processRead(data)) {
            if (upstreamFilter != null) {
                upstreamFilter.onRead(data);
            }
        }
    }

    /**
     * An event listener that is called when the connection is closed by the peer.
     * <p/>
     * If the {@link Filter} needs to process this event, it must implement {@link #processConnectionClosed()} method.
     */
    final void onConnectionClosed() {
        processConnectionClosed();

        final Filter filter = upstreamFilter;
        if (filter != null) {
            filter.onConnectionClosed();
        }
    }

    /**
     * An event listener that is called, when SSL completes its handshake.
     * <p/>
     * If the {@link Filter} needs to process this event, it must implement {@link #processSslHandshakeCompleted()}
     * method.
     */
    final void onSslHandshakeCompleted() {
        processSslHandshakeCompleted();

        if (upstreamFilter != null) {
            upstreamFilter.onSslHandshakeCompleted();
        }
    }


    /**
     * An event listener that is called when an error has occurred.
     * <p/>
     * Errors travel in direction from downstream filter to upstream filter.
     * <p/>
     * If the {@link Filter} needs to process this event, it must implement {@link #processError(Throwable)} method.
     *
     * @param t an error that has occurred.
     */
    final void onError(Throwable t) {
        processError(t);

        if (upstreamFilter != null) {
            upstreamFilter.onError(t);
        }
    }

    /**
     * Handle {@link #connect(SocketAddress, Filter)}.
     *
     * @param address        an address where to connect (server or proxy).
     * @param upstreamFilter a filter positioned upstream.
     * @see #connect(SocketAddress, Filter)
     */
    void handleConnect(SocketAddress address, Filter upstreamFilter) {
    }

    /**
     * Process {@link #onConnect()}.
     *
     * @see #onConnect()
     */
    void processConnect() {
    }

    /**
     * Process {@link #onRead(ByteBuffer)}.
     *
     * @param data read data.
     * @return {@code true} if the data should be sent to processing to upper filter in the chain, {@code false}
     * otherwise.
     * @see #onRead(ByteBuffer)
     */
    boolean processRead(ByteBuffer data) {
        return true;
    }

    /**
     * Process {@link #onConnectionClosed()}.
     *
     * @see #onConnectionClosed()
     */
    void processConnectionClosed() {
    }

    /**
     * Process {@link #onSslHandshakeCompleted()}.
     *
     * @see #onSslHandshakeCompleted()
     */
    void processSslHandshakeCompleted() {
    }

    /**
     * Process {@link #onError(Throwable)}.
     *
     * @param t an error that has occurred.
     * @see #onError(Throwable)
     */
    void processError(Throwable t) {
    }
}
