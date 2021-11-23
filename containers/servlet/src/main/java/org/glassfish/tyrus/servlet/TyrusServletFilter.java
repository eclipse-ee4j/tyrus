/*
 * Copyright (c) 2012, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.servlet;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.websocket.server.HandshakeRequest;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import org.glassfish.tyrus.core.TyrusUpgradeResponse;
import org.glassfish.tyrus.core.Utils;
import org.glassfish.tyrus.spi.UpgradeResponse;

/**
 * Filter used for Servlet integration.
 * <p/>
 * Consumes only requests with {@link HandshakeRequest#SEC_WEBSOCKET_KEY} headers present, all others are passed back
 * to
 * {@link FilterChain}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
class TyrusServletFilter implements Filter, HttpSessionListener {

    private TyrusServletUpgrade tyrusServletUpgrade;

    TyrusServletFilter(TyrusServletUpgrade tyrusServletUpgrade) {
        this.tyrusServletUpgrade = tyrusServletUpgrade;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        tyrusServletUpgrade.init(servletContext);
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // do nothing.
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        tyrusServletUpgrade.destroySession(se.getSession());
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        final HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        boolean success = tyrusServletUpgrade.upgrade(httpServletRequest, httpServletResponse);

        if (!success && filterChain != null) {
            filterChain.doFilter(request, response);
        }
    }

    private static void appendTraceHeaders(HttpServletResponse httpServletResponse, TyrusUpgradeResponse
            tyrusUpgradeResponse) {
        for (Map.Entry<String, List<String>> entry : tyrusUpgradeResponse.getHeaders().entrySet()) {
            if (entry.getKey().contains(UpgradeResponse.TRACING_HEADER_PREFIX)) {
                httpServletResponse.addHeader(entry.getKey(), Utils.getHeaderFromList(entry.getValue()));
            }
        }
    }

    @Override
    public void destroy() {
        tyrusServletUpgrade.destroy();
    }
}
