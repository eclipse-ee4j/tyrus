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

package org.glassfish.tyrus.container.grizzly.server;

import org.glassfish.tyrus.spi.ServerContainer;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.NetworkListener;

/**
 * WebSockets {@link AddOn} for the {@link HttpServer}.
 *
 * @author Alexey Stashok
 */
// keep this public to allow other developers use this with their own GrizzlyServerContainer alternative
// see https://java.net/jira/browse/TYRUS-317
public class WebSocketAddOn implements AddOn {

    private final ServerContainer serverContainer;
    private final String contextPath;

    public WebSocketAddOn(ServerContainer serverContainer, String contextPath) {
        this.serverContainer = serverContainer;
        this.contextPath = contextPath;
    }

    @Override
    public void setup(NetworkListener networkListener, FilterChainBuilder builder) {
        // Get the index of HttpServerFilter in the HttpServer filter chain
        final int httpServerFilterIdx = builder.indexOfType(HttpServerFilter.class);

        if (httpServerFilterIdx >= 0) {
            // Insert the WebSocketFilter right before HttpServerFilter
            builder.add(httpServerFilterIdx, new GrizzlyServerFilter(serverContainer, contextPath));
        }
    }
}
