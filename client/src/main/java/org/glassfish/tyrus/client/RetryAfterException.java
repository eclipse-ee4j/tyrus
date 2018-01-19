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

package org.glassfish.tyrus.client;

import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

import org.glassfish.tyrus.core.HandshakeException;
import org.glassfish.tyrus.spi.UpgradeResponse;

/**
 * This exception is set as a cause of {@link DeploymentException} thrown from {@link
 * WebSocketContainer}.connectToServer(...) when HTTP response status code {@code 503 - Service Unavailable} is
 * received.
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 * @see ClientManager.ReconnectHandler
 * @see ClientProperties#RETRY_AFTER_SERVICE_UNAVAILABLE
 */
public class RetryAfterException extends HandshakeException {

    private final Long delay;

    /**
     * Constructor.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *                method.
     * @param delay   a delay to the time received handshake response in  header.
     */
    public RetryAfterException(String message, Long delay) {
        super(503, message);
        this.delay = delay;
    }

    /**
     * Get a delay specified in {@value UpgradeResponse#RETRY_AFTER} response header in seconds.
     *
     * @return a delay in seconds or {@code null} when response does not contain {@value UpgradeResponse#RETRY_AFTER} or
     * the value cannot be parsed as long ot {@code http-date}.
     */
    public Long getDelay() {
        return delay;
    }
}
