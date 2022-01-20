/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.servlet.embedded;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerContainer;
import org.glassfish.tyrus.server.TyrusServerContainer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebFilter(filterName = "A", value = "/*")
public class DispatchingServletFilter implements Filter {

    public static enum OP {
        UpgradeHttpToWebSocket,
        AddEndpoint
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        final HttpServletRequest servletRequest = (HttpServletRequest) request;
        final Object objectContainer = request.getServletContext().getAttribute(ServerContainer.class.getName());
        if (objectContainer == null
                || servletRequest.getQueryString() == null) {
            chain.doFilter(request, response);
            return;
        }
        if (servletRequest.getQueryString().equals(OP.UpgradeHttpToWebSocket.toString())) {
            final String path = "/test/{a}/{b}";
            final Map<String, String> parameters = new HashMap<>();
            parameters.put("a", "hello");
            parameters.put("b", "world");
            final TyrusServerContainer container = (TyrusServerContainer) objectContainer;
            try {
                container.upgradeHttpToWebSocket(
                        request,
                        response,
                        new DispatchingServletProgrammaticEndpointConfig(ProgramaticEndpoint.class, path),
                        parameters);
            } catch (DeploymentException e) {
                e.printStackTrace();
            }
        } else if (servletRequest.getQueryString().equals(OP.AddEndpoint.toString())) {
            final TyrusServerContainer container = (TyrusServerContainer) objectContainer;
            try {
                final String path = "/test/{a}/{b}";
                container.addEndpoint(new DispatchingServletProgrammaticEndpointConfig(ProgramaticEndpoint.class, path));
            } catch (DeploymentException e) {
                throw new ServletException(e);
            }
        }
    }

    public static class ProgramaticEndpoint extends Endpoint {

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            session.addMessageHandler(new ProgramaticEndpointMessageHandler(session));
        }

        @Override
        public void onError(Session session, Throwable thr) {
            thr.printStackTrace();
        }

        /*
         * Anonymous classes do not work with Arquillian archive and module-info.
         */
        class ProgramaticEndpointMessageHandler implements MessageHandler.Whole<String> {
            private final Session session;
            private final Map<String, String> parameters;

            public ProgramaticEndpointMessageHandler(Session session) {
                this.session = session;
                this.parameters = session.getPathParameters();
            }

            @Override
            public void onMessage(String message) {
                try {
                    session.getBasicRemote().sendText(parameters.get("a"));
                    session.getBasicRemote().sendText(parameters.get("b"));
                } catch (IOException e) {
                    onError(session, e);
                }
            }
        }
    }
}
