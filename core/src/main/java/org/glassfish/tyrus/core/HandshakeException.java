/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

/**
 * {@link Exception}, which describes the error, occurred during the handshake phase.
 *
 * @author Alexey Stashok
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class HandshakeException extends Exception {

    private final int httpStatusCode;

    /**
     * Construct a HandshakeException. HTTP status code will be set to {@code 500}.
     *
     * @param message error description
     */
    public HandshakeException(String message) {
        this(500, message);
    }

    /**
     * Constructor.
     *
     * @param httpStatusCode http status code to be set to response.
     * @param message        the detail message. The detail message is saved for later retrieval by the {@link
     *                       #getMessage()} method.
     */
    public HandshakeException(int httpStatusCode, String message) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * Get the error code.
     *
     * @return the error code.
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
