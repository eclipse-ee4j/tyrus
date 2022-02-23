/*
 * Copyright (c) 2014, 2022 Oracle and/or its affiliates. All rights reserved.
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

import javax.websocket.CloseReason;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Enum containing standard CloseReasons defined in RFC 6455, see chapter
 * <a href="https://tools.ietf.org/html/rfc6455#section-7.4.1">7.4.1 Defined Status Codes</a>.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public enum CloseReasons {

    /**
     * 1000 indicates a normal closure, meaning that the purpose for
     * which the connection was established has been fulfilled.
     */
    NORMAL_CLOSURE(CloseReason.CloseCodes.NORMAL_CLOSURE, "Normal closure."),

    /**
     * 1001 indicates that an endpoint is "going away", such as a server
     * going down or a browser having navigated away from a page.
     */
    GOING_AWAY(CloseReason.CloseCodes.GOING_AWAY, "Going away."),

    /**
     * 1002 indicates that an endpoint is terminating the connection due
     * to a protocol error.
     */
    PROTOCOL_ERROR(CloseReason.CloseCodes.PROTOCOL_ERROR, "Protocol error."),

    /**
     * 1003 indicates that an endpoint is terminating the connection
     * because it has received a type of data it cannot accept (e.g., an
     * endpoint that understands only text data MAY send this if it
     * receives a binary message).
     */
    CANNOT_ACCEPT(CloseReason.CloseCodes.CANNOT_ACCEPT, "Cannot accept."),

    /**
     * Reserved.  The specific meaning might be defined in the future.
     */
    RESERVED(CloseReason.CloseCodes.RESERVED, "Reserved."),

    /**
     * 1005 is a reserved value and MUST NOT be set as a status code in a
     * Close control frame by an endpoint.  It is designated for use in
     * applications expecting a status code to indicate that no status
     * code was actually present.
     */
    NO_STATUS_CODE(CloseReason.CloseCodes.NO_STATUS_CODE, "No status code."),

    /**
     * 1006 is a reserved value and MUST NOT be set as a status code in a
     * Close control frame by an endpoint.  It is designated for use in
     * applications expecting a status code to indicate that the
     * connection was closed abnormally, e.g., without sending or
     * receiving a Close control frame.
     */
    CLOSED_ABNORMALLY(CloseReason.CloseCodes.CLOSED_ABNORMALLY, "Closed abnormally."),

    /**
     * 1007 indicates that an endpoint is terminating the connection
     * because it has received data within a message that was not
     * consistent with the type of the message (e.g., non-UTF-8
     * data within a text message).
     */
    NOT_CONSISTENT(CloseReason.CloseCodes.NOT_CONSISTENT, "Not consistent."),

    /**
     * 1008 indicates that an endpoint is terminating the connection
     * because it has received a message that violates its policy.  This
     * is a generic status code that can be returned when there is no
     * other more suitable status code (e.g., 1003 or 1009) or if there
     * is a need to hide specific details about the policy.
     */
    VIOLATED_POLICY(CloseReason.CloseCodes.VIOLATED_POLICY, "Violated policy."),

    /**
     * 1009 indicates that an endpoint is terminating the connection
     * because it has received a message that is too big for it to
     * process.
     */
    TOO_BIG(CloseReason.CloseCodes.TOO_BIG, "Too big."),

    /**
     * 1010 indicates that an endpoint (client) is terminating the
     * connection because it has expected the server to negotiate one or
     * more extension, but the server didn't return them in the response
     * message of the WebSocket handshake.  The list of extensions that
     * are needed SHOULD appear in the /reason/ part of the Close frame.
     * Note that this status code is not used by the server, because it
     * can fail the WebSocket handshake instead.
     */
    NO_EXTENSION(CloseReason.CloseCodes.NO_EXTENSION, "No extension."),

    /**
     * 1011 indicates that a server is terminating the connection because
     * it encountered an unexpected condition that prevented it from
     * fulfilling the request.
     */
    UNEXPECTED_CONDITION(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Unexpected condition."),

    /**
     * 1012 indicates that the service will be restarted.
     */
    SERVICE_RESTART(CloseReason.CloseCodes.SERVICE_RESTART, "Service restart."),

    /**
     * 1013 indicates that the service is experiencing overload
     */
    TRY_AGAIN_LATER(CloseReason.CloseCodes.TRY_AGAIN_LATER, "Try again later."),

    /**
     * 1015 is a reserved value and MUST NOT be set as a status code in a
     * Close control frame by an endpoint.  It is designated for use in
     * applications expecting a status code to indicate that the
     * connection was closed due to a failure to perform a TLS handshake
     * (e.g., the server certificate can't be verified).
     */
    TLS_HANDSHAKE_FAILURE(CloseReason.CloseCodes.TLS_HANDSHAKE_FAILURE, "TLS handshake failure.");

    private final CloseReason closeReason;

    CloseReasons(CloseReason.CloseCode closeCode, String reasonPhrase) {
        this.closeReason = new CloseReason(closeCode, reasonPhrase);
    }

    /**
     * Get close reason.
     *
     * @return close reason represented by this value;
     */
    public CloseReason getCloseReason() {
        return closeReason;
    }

    /* Utility CloseReason methods */

    public static CloseReason create(CloseReason.CloseCode closeCode, String reasonPhrase) {
        return new CloseReason(closeCode, trimTo123Bytes(reasonPhrase));
    }

    private static String trimTo123Bytes(String reasonPhrase) {
        try {
            final byte[] bytes;
            return reasonPhrase == null
                    ? reasonPhrase
                    : ((bytes = reasonPhrase.getBytes("UTF-8")).length <= 123)
                        ? reasonPhrase
                        : new String(trimTo123Bytes(bytes), "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalArgumentException(uee);
        }
    }

    private static byte[] trimTo123Bytes(byte[] bytes) {
        byte[] newBytes = new byte[123];
        System.arraycopy(bytes, 0, newBytes, 0, 120);
        newBytes[120] = newBytes[121] = newBytes[122] = (byte) '.';
        return newBytes;
    }
}
