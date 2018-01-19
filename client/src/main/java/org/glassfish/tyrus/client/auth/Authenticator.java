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
import org.glassfish.tyrus.spi.UpgradeRequest;
import org.glassfish.tyrus.spi.UpgradeResponse;

/**
 * Authenticator provides a way how to plug-in custom authentication provider.
 * <p>
 * Authenticator is called when server-side returns HTTP 401 as a reply to handshake response. Tyrus client then looks
 * for authenticator instance registered to authentication scheme provided by server.
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 * @see AuthConfig.Builder#registerAuthProvider(String, Authenticator)
 * @see ClientProperties#AUTH_CONFIG
 * @see ClientProperties#CREDENTIALS
 */
@Beta
public abstract class Authenticator {

    /**
     * Generate value used as "{@value UpgradeRequest#AUTHORIZATION}" header value for next request.
     * <p>
     * Thrown {@link AuthenticationException} will be wrapped as {@link DeploymentException} and thrown as a result of
     * {@link WebSocketContainer}.connectToServer(...) method call.
     *
     * @param uri                   Uri of the server endpoint.
     * @param wwwAuthenticateHeader "{@value UpgradeResponse#WWW_AUTHENTICATE}" header value received in a handshake
     *                              response.
     * @param credentials           credentials passed by property {@link ClientProperties#CREDENTIALS}. Can be {@code
     *                              null} when there were no {@link Credentials} registered.
     * @return value for {@value UpgradeRequest#AUTHORIZATION} header which will be put into next handshake request.
     * @throws AuthenticationException when it is not possible to create "{@value UpgradeRequest#AUTHORIZATION}"
     *                                 header.
     */
    public abstract String generateAuthorizationHeader(final URI uri, final String wwwAuthenticateHeader,
                                                       final Credentials credentials) throws AuthenticationException;

}
