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

import java.net.URI;

import org.junit.Test;
import static org.junit.Assert.*;


/**
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
public class AuthConfigTest {

    private static final String PROPRIETARY = "Proprietary";

    @Test
    public void testDefaultBuilder() {
        AuthConfig authConfig = AuthConfig.builder().build();
        assertEquals("Default AuthConfig should have just 2 authenticators", authConfig.getAuthenticators().size(), 2);
        assertTrue("Default AuthConfig should have Basic authenticator",
                   authConfig.getAuthenticators().get(AuthConfig.BASIC) instanceof BasicAuthenticator);
        assertTrue("Default AuthConfig should have Digest authenticator",
                   authConfig.getAuthenticators().get(AuthConfig.DIGEST) instanceof DigestAuthenticator);
    }

    @Test
    public void testDisableBasic() {
        AuthConfig authConfig = AuthConfig.builder().disableProvidedBasicAuth().build();
        assertEquals("AuthConfig should have just 1 authenticators", authConfig.getAuthenticators().size(), 1);
        assertTrue("AuthConfig should have Digest authenticator",
                   authConfig.getAuthenticators().get(AuthConfig.DIGEST) instanceof DigestAuthenticator);
        assertNull("AuthConfig should remove Basic auth", authConfig.getAuthenticators().get(AuthConfig.BASIC));
    }

    @Test
    public void testDisableDigest() {
        AuthConfig authConfig = AuthConfig.builder().disableProvidedDigestAuth().build();
        assertEquals("AuthConfig should have just 1 authenticators", authConfig.getAuthenticators().size(), 1);
        assertTrue("AuthConfig should have Basic authenticator",
                   authConfig.getAuthenticators().get(AuthConfig.BASIC) instanceof BasicAuthenticator);
        assertNull("AuthConfig should remove Digest auth", authConfig.getAuthenticators().get(AuthConfig.DIGEST));
    }

    @Test
    public void testOverrideBasic() {
        AuthConfig authConfig =
                AuthConfig.builder().registerAuthProvider(AuthConfig.BASIC, new ProprietaryAuthenticator()).build();
        assertEquals("Default AuthConfig should have just 2 authenticators", authConfig.getAuthenticators().size(), 2);
        assertTrue("AuthConfig should have Proprietary authenticator mapped as Basic",
                   authConfig.getAuthenticators().get(AuthConfig.BASIC) instanceof ProprietaryAuthenticator);
    }

    @Test
    public void testAddNewAuthenticator() {
        AuthConfig authConfig =
                AuthConfig.builder().registerAuthProvider(PROPRIETARY, new ProprietaryAuthenticator()).build();
        assertEquals("AuthConfig should have just 3 authenticators", authConfig.getAuthenticators().size(), 3);
        assertTrue("AuthConfig should have Basic authenticator",
                   authConfig.getAuthenticators().get(AuthConfig.BASIC) instanceof BasicAuthenticator);
        assertTrue("AuthConfig should have Digest authenticator",
                   authConfig.getAuthenticators().get(AuthConfig.DIGEST) instanceof DigestAuthenticator);
        assertTrue("AuthConfig should have Proprietary authenticator",
                   authConfig.getAuthenticators().get(PROPRIETARY) instanceof ProprietaryAuthenticator);
    }

    class ProprietaryAuthenticator extends Authenticator {

        @Override
        public String generateAuthorizationHeader(URI uri, String wwwAuthenticateHeader, Credentials credentials) throws
                AuthenticationException {
            return "authorize me";
        }
    }
}
