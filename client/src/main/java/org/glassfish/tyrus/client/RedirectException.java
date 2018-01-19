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

import java.net.URI;

import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

import org.glassfish.tyrus.core.HandshakeException;
import org.glassfish.tyrus.spi.UpgradeResponse;

/**
 * This exception is set as a cause of {@link DeploymentException} thrown from {@link
 * WebSocketContainer}.connectToServer(...)
 * when any of the Redirect HTTP response status codes (300, 301, 302, 303, 307, 308) is received as a handshake
 * response and:
 * <ul>
 * <li>
 * {@link ClientProperties#REDIRECT_ENABLED} is not enabled
 * </li>
 * <li>
 * or the chained redirection count exceeds the value of {@link ClientProperties#REDIRECT_THRESHOLD}
 * </li>
 * <li>
 * or Infinite redirection loop is detected
 * </li>
 * <li>
 * or {@value UpgradeResponse#LOCATION} response header is missing, empty or does not contain a valid {@link URI}.
 * </li>
 * </ul>
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 * @see ClientProperties#REDIRECT_ENABLED
 * @see ClientProperties#REDIRECT_THRESHOLD
 */
public class RedirectException extends HandshakeException {

    private static final long serialVersionUID = 4357724300486801294L;

    /**
     * Constructor.
     *
     * @param httpStatusCode http status code to be set to response.
     * @param message        the detail message. The detail message is saved for later retrieval by the {@link
     *                       #getMessage()} method.
     */
    public RedirectException(int httpStatusCode, String message) {
        super(httpStatusCode, message);
    }
}
