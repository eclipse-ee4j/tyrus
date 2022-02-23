/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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
import org.junit.Assert;
import org.junit.Test;

public class CloseReasonsTest {
    @Test
    public void testReasonOfLength123() {
        String reason = createStringOfLength(123);
        CloseReason closeReason = CloseReasons.create(CloseReason.CloseCodes.NO_STATUS_CODE, reason);
        Assert.assertEquals(reason, closeReason.getReasonPhrase());
    }

    @Test
    public void testReasonOfLength124() {
        String reason = createStringOfLength(124);
        CloseReason closeReason = CloseReasons.create(CloseReason.CloseCodes.NO_STATUS_CODE, reason);

        String expected = createStringOfLength(120) + "...";
        Assert.assertEquals(expected, closeReason.getReasonPhrase());
    }

    @Test
    public void testNullReason() {
        CloseReason closeReason = CloseReasons.create(CloseReason.CloseCodes.NO_STATUS_CODE, null);
        Assert.assertEquals("", closeReason.getReasonPhrase());
    }

    private String createStringOfLength(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i != length; i++) {
            sb.append(i % 10);
        }
        return sb.toString();
    }
}
