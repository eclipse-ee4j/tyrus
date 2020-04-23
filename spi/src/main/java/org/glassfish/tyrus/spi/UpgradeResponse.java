/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import jakarta.websocket.HandshakeResponse;

/**
 * Abstraction for a HTTP upgrade response. A transport creates an
 * implementation for this and uses {@link WebSocketEngine#upgrade} method
 * to upgrade the request.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public abstract class UpgradeResponse implements HandshakeResponse {

    /**
     * Header containing challenge with authentication scheme and parameters.
     */
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    /**
     * Header containing a new URI when {@link #getStatus()} .
     */
    public static final String LOCATION = "Location";

    /**
     * Header containing delay or date in which client can try to reconnect to the server.
     */
    public static final String RETRY_AFTER = "Retry-After";

    /**
     * Prefix of headers used for including tracing information into handshake response.
     */
    public static final String TRACING_HEADER_PREFIX = "X-Tyrus-Tracing-";

    /**
     * Get the current HTTP status code of this response.
     *
     * @return the current HTTP status code.
     */
    public abstract int getStatus();

    /**
     * Set HTTP status code for this response.
     *
     * @param status HTTP status code for this response.
     */
    public abstract void setStatus(int status);

    /**
     * Get HTTP reason phrase.
     * <p>
     * TODO remove ?? we are using only for "Switching Protocols" and that is
     * TODO standard status code 101
     *
     * @param reason reason phrase to be set.
     */
    public abstract void setReasonPhrase(String reason);

    /**
     * Gets the value of the response header with the given name.
     * <p>
     * If a response header with the given name exists and contains
     * multiple values, the value that was added first will be returned.
     *
     * @param name header name.
     * @return the value of the response header with the given name,
     * null if no header with the given name has been set
     * on this response.
     * TODO rename to getHeader(String name) ?? similar to
     * TODO HttpServletResponse#getHeader(String)
     */
    public final String getFirstHeaderValue(final String name) {
        final List<String> stringList = getHeaders().get(name);
        return stringList == null ? null : (stringList.size() > 0 ? stringList.get(0) : null);
    }
}
