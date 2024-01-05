/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.client.exception;

import org.glassfish.tyrus.core.HandshakeException;

import jakarta.websocket.DeploymentException;

/**
 * Converts the exceptions into more specific ones.
 */
public class Exceptions {

    /**
     * Get the Deployment Exception, or return the exception if of the type.
     * @param message The Exception message
     * @param cause The Cause Exception
     * @return a Deployment exception.
     */
    public static DeploymentException deploymentException(String message, Throwable cause) {
        if (DeploymentException.class.isInstance(cause)) {
            return (DeploymentException) cause;
        } else if (HandshakeException.class.isInstance(cause)) {
            return new DeploymentHandshakeException(message, (HandshakeException) cause);
        } else {
            return new DeploymentException(message, cause);
        }
    }
}
