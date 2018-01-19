/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.servlet.dynamic_deploy;

import java.io.IOException;

import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Calls {@link javax.websocket.server.ServerContainer#addEndpoint(javax.websocket.server.ServerEndpointConfig)}.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@WebListener
public class MyServletContextListenerProgrammatic extends Endpoint implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final ServerContainer serverContainer =
                (ServerContainer) sce.getServletContext().getAttribute("javax.websocket.server.ServerContainer");

        try {
            // this is the important call
            serverContainer.addEndpoint(ServerEndpointConfig.Builder.create(MyServletContextListenerProgrammatic.class,
                                                                            "/programmatic").build());
        } catch (DeploymentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Override
    public void onOpen(final Session session, EndpointConfig config) {
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    // do nothing.
                }
            }
        });
    }
}
