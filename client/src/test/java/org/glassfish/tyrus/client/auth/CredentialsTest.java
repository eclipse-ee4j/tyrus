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

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class CredentialsTest {

    private static final String USERNAME = "username";
    private static final String PASSWORD_STRING = "password";
    private static final byte[] PASSWORD_BYTE_ARRAY = {0x01, 0x02, 0x03, 0x04};

    @Test
    public void testGetUsername() throws Exception {
        Credentials credentials = new Credentials(USERNAME, PASSWORD_STRING);

        assertEquals(USERNAME, credentials.getUsername());
    }

    @Test
    public void testGetPasswordString() throws Exception {
        Credentials credentials = new Credentials(USERNAME, PASSWORD_STRING);

        assertArrayEquals(PASSWORD_STRING.getBytes(AuthConfig.CHARACTER_SET), credentials.getPassword());
    }

    @Test
    public void testGetPasswordByteArray() throws Exception {
        Credentials credentials = new Credentials(USERNAME, PASSWORD_BYTE_ARRAY);

        assertEquals(PASSWORD_BYTE_ARRAY, credentials.getPassword());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullUsername1() {
        //noinspection ResultOfObjectAllocationIgnored
        new Credentials(null, PASSWORD_STRING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullUsername2() {
        //noinspection ResultOfObjectAllocationIgnored
        new Credentials(null, PASSWORD_BYTE_ARRAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPassword1() {
        //noinspection ResultOfObjectAllocationIgnored
        new Credentials(USERNAME, (String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPassword2() {
        //noinspection ResultOfObjectAllocationIgnored
        new Credentials(USERNAME, (byte[]) null);
    }
}
