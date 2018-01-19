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

package org.glassfish.tyrus.core;

import java.util.Arrays;

import org.glassfish.tyrus.core.frame.Frame;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class FrameTest {
    @Test
    public void testIsFin() throws Exception {
        assertTrue(new Frame.Builder().fin(true).build().isFin());
        assertFalse(new Frame.Builder().fin(false).build().isFin());
    }

    @Test
    public void testIsRsv1() throws Exception {
        assertTrue(new Frame.Builder().rsv1(true).build().isRsv1());
        assertFalse(new Frame.Builder().rsv1(false).build().isRsv1());
    }

    @Test
    public void testIsRsv2() throws Exception {
        assertTrue(new Frame.Builder().rsv2(true).build().isRsv2());
        assertFalse(new Frame.Builder().rsv2(false).build().isRsv2());
    }

    @Test
    public void testIsRsv3() throws Exception {
        assertTrue(new Frame.Builder().rsv3(true).build().isRsv3());
        assertFalse(new Frame.Builder().rsv3(false).build().isRsv3());
    }

    @Test
    public void testIsMask() throws Exception {
        assertTrue(new Frame.Builder().mask(true).build().isMask());
        assertFalse(new Frame.Builder().mask(false).build().isMask());
    }

    @Test
    public void testPayloadDataChange() throws Exception {
        byte[] payload = {'0', '1', '2'};

        final Frame frame = new Frame.Builder().payloadData(payload).build();
        assertTrue(Arrays.equals(payload, frame.getPayloadData()));
        frame.getPayloadData()[0] = '9';

        // original value should not be changed.
        assertTrue(Arrays.equals(payload, frame.getPayloadData()));

    }

    /**
     * TODO: test validation when added to Frame.
     */
}
