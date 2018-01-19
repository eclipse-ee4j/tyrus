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

package org.glassfish.tyrus.container.jdk.client;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.glassfish.tyrus.spi.UpgradeRequest;

/**
 * Adds getHttpMethod method to @UpgradeRequest. Wraps an upgrade request and delegates all method calls except
 * {@link #getHttpMethod()} to it.
 *
 * @author Petr Janouch
 */
abstract class JdkUpgradeRequest extends UpgradeRequest {

    private final UpgradeRequest upgradeRequest;

    /**
     * Create new {@link org.glassfish.tyrus.container.jdk.client.JdkUpgradeRequest} wrapping
     *
     * @param upgradeRequest wrapped upgrade request.
     */
    JdkUpgradeRequest(UpgradeRequest upgradeRequest) {
        this.upgradeRequest = upgradeRequest;
    }

    /**
     * Returns a HTTP method that should be used when composing HTTP request.
     *
     * @return a HTTP method.
     */
    public abstract String getHttpMethod();

    @Override
    public String getHeader(String name) {
        return upgradeRequest.getHeader(name);
    }

    @Override
    public boolean isSecure() {
        return upgradeRequest.isSecure();
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return upgradeRequest.getHeaders();
    }

    @Override
    public Principal getUserPrincipal() {
        return upgradeRequest.getUserPrincipal();
    }

    @Override
    public URI getRequestURI() {
        return upgradeRequest.getRequestURI();
    }

    @Override
    public boolean isUserInRole(String role) {
        return upgradeRequest.isUserInRole(role);
    }

    @Override
    public Object getHttpSession() {
        return upgradeRequest.getHttpSession();
    }

    @Override
    public Map<String, List<String>> getParameterMap() {
        return upgradeRequest.getParameterMap();
    }

    @Override
    public String getQueryString() {
        return upgradeRequest.getQueryString();
    }
}
