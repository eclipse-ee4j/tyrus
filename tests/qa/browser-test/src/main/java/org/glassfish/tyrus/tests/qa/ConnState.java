/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.qa;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public enum ConnState {

    NONE("SYN", "NO-CONN"),
    SERVER_OPEN("SYN", "NO-CONN"),
    SYN_SENT("ACK", "SYN-ACK"),
    ESTABLISHED("FIN", "ESTABLISHED"),
    CLOSED("ACK", "FIN-ACK"),
    BROWSER_OKAY("BROWSER_OKAY", null),
    BROWSER_FAIL("BROWSER_FAIL", null);
    private String expMsg, sendMsg;

    ConnState(String expMsg, String sendMsg) {
        this.expMsg = expMsg;
        this.sendMsg = sendMsg;
    }

    static ConnState next(String message, ConnState state) {
        if (message.equals(state.getExpMsg())) {
            switch (state) {
                case NONE:
                case SERVER_OPEN:
                    return SYN_SENT;
                case SYN_SENT:
                    return ESTABLISHED;
                case ESTABLISHED:
                    return CLOSED;
                case CLOSED:
                    return BROWSER_OKAY;
                case BROWSER_OKAY:
                    return BROWSER_OKAY;
                case BROWSER_FAIL:
                    return BROWSER_FAIL;
                default:
                    return NONE;
            }
        } else {
            return BROWSER_FAIL;
        }
    }

    String getExpMsg() {
        return expMsg;
    }

    String getSendMsg() {
        return sendMsg;
    }
}
