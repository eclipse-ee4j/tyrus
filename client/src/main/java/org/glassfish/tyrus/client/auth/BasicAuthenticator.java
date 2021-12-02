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
import java.util.Base64;

import org.glassfish.tyrus.core.l10n.LocalizationMessages;

/**
 * Generates a value of {@code Authorization} header of HTTP request for Basic Http Authentication scheme.
 *
 * @author Ondrej Kosatka
 */
final class BasicAuthenticator extends Authenticator {

    @Override
    public String generateAuthorizationHeader(final URI uri, final String wwwAuthenticateHeader,
                                              final Credentials credentials) throws AuthenticationException {
        return generateAuthorizationHeader(credentials);
    }

    private String generateAuthorizationHeader(final Credentials credentials) throws AuthenticationException {
        if (credentials == null) {
            throw new AuthenticationException(LocalizationMessages.AUTHENTICATION_CREDENTIALS_MISSING());
        }
        String username = credentials.getUsername();
        byte[] password = credentials.getPassword();

        final byte[] prefix = (username + ":").getBytes(AuthConfig.CHARACTER_SET);
        final byte[] usernamePassword = new byte[prefix.length + password.length];

        System.arraycopy(prefix, 0, usernamePassword, 0, prefix.length);
        System.arraycopy(password, 0, usernamePassword, prefix.length, password.length);

        return "Basic " + Base64.getEncoder().encodeToString(usernamePassword);
    }

}
