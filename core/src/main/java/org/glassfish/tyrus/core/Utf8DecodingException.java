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

import javax.websocket.CloseReason;

import org.glassfish.tyrus.core.l10n.LocalizationMessages;

/**
 * TODO
 */
public class Utf8DecodingException extends WebSocketException {

    private static final CloseReason CLOSE_REASON =
            new CloseReason(CloseReason.CloseCodes.NOT_CONSISTENT, LocalizationMessages.ILLEGAL_UTF_8_SEQUENCE());
    private static final long serialVersionUID = 7766051445796057L;

    /**
     * TODO
     */
    public Utf8DecodingException() {
        super(CLOSE_REASON.getReasonPhrase());
    }

    @Override
    public CloseReason getCloseReason() {
        return CLOSE_REASON;
    }
}
