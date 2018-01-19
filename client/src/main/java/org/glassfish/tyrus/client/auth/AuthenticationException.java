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

package org.glassfish.tyrus.client.auth;

import java.net.URI;

import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.core.Beta;
import org.glassfish.tyrus.core.HandshakeException;
import org.glassfish.tyrus.spi.UpgradeResponse;

/**
 * This exception is set as a cause of {@link DeploymentException} thrown when {@link
 * WebSocketContainer}.connectToServer(...)
 * fails because of any of the following:
 * <ul>
 * <li>
 * HTTP response status code 401 is received and "{@value UpgradeResponse#WWW_AUTHENTICATE}" header
 * contains scheme which is not handled by any {@link Authenticator} registered in {@link AuthConfig}.
 * </li>
 * <li>
 * HTTP response status code 401 is received and "{@value UpgradeResponse#WWW_AUTHENTICATE}" header
 * does not contain authentication scheme token or "{@value UpgradeResponse#WWW_AUTHENTICATE}" header is missing.
 * </li>
 * <li>
 * {@link AuthenticationException} is thrown from {@link Authenticator#generateAuthorizationHeader(URI, String,
 * Credentials)}
 * method.
 * </li>
 * <li>
 * Property {@link ClientProperties#AUTH_CONFIG} is not instance of {@link AuthConfig}.
 * </li>
 * </ul>
 * <p>
 * {@link #getHttpStatusCode()} returns always {@code 401}.
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 * @see Authenticator#generateAuthorizationHeader(URI, String, Credentials)
 * @see AuthConfig
 */
@Beta
public class AuthenticationException extends HandshakeException {

    /**
     * Constructor.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *                method.
     */
    public AuthenticationException(String message) {
        super(401, message);
    }

}
