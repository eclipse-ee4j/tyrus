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

import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.core.Beta;
import org.glassfish.tyrus.core.l10n.LocalizationMessages;

/**
 * Credentials can be used when configuring authentication properties used during client handshake.
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 * @see ClientProperties#CREDENTIALS
 */
@Beta
public final class Credentials {

    private final String username;
    private final byte[] password;

    /**
     * Create new credentials.
     *
     * @param username Username. Cannot be {@code null}.
     * @param password Password as byte array. Cannot be {@code null}.
     */
    public Credentials(String username, byte[] password) {
        if (username == null) {
            throw new IllegalArgumentException(LocalizationMessages.ARGUMENT_NOT_NULL("username"));
        }

        if (password == null) {
            throw new IllegalArgumentException(LocalizationMessages.ARGUMENT_NOT_NULL("password"));
        }

        this.username = username;
        this.password = password;
    }

    /**
     * Create new credentials.
     *
     * @param username Username. Cannot be {@code null}.
     * @param password Password. Cannot be {@code null}.
     */
    public Credentials(String username, String password) {
        if (username == null) {
            throw new IllegalArgumentException(LocalizationMessages.ARGUMENT_NOT_NULL("username"));
        }

        if (password == null) {
            throw new IllegalArgumentException(LocalizationMessages.ARGUMENT_NOT_NULL("password"));
        }

        this.username = username;
        this.password = password.getBytes(AuthConfig.CHARACTER_SET);
    }

    /**
     * Get the username.
     *
     * @return username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the password as byte array.
     *
     * @return Password string in byte array representation.
     */
    public byte[] getPassword() {
        return password;
    }

    public String toString() {
        return "Credentials{username: " + username + ", password: *****}";
    }
}
