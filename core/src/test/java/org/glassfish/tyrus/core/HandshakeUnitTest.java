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

import java.net.URI;

import org.glassfish.tyrus.spi.UpgradeRequest;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link Handshake}.
 *
 * @author Roman Puchkovskiy
 */
public class HandshakeUnitTest {
    @Test
    public void originForPlainConnectionShouldStartWithHttp() throws Exception {
        UpgradeRequest upgradeRequest = new RequestContext.Builder()
                .requestURI(new URI("ws://localhost:8443/echo"))
                .secure(false)
                .build();

        Handshake.updateHostAndOrigin(upgradeRequest);

        assertThat(upgradeRequest.getHeader("Origin"), is("http://localhost:8443"));
    }

    @Test
    public void originForSecureConnectionShouldStartWithHttps() throws Exception {
        UpgradeRequest upgradeRequest = new RequestContext.Builder()
                .requestURI(new URI("wss://localhost:8443/echo"))
                .secure(true)
                .build();

        Handshake.updateHostAndOrigin(upgradeRequest);

        assertThat(upgradeRequest.getHeader("Origin"), is("https://localhost:8443"));
    }
}
