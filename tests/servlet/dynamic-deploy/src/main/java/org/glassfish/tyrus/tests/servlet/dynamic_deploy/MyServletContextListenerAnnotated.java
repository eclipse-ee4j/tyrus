/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

import jakarta.websocket.DeploymentException;
import jakarta.websocket.OnMessage;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpoint;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Calls {@link jakarta.websocket.server.ServerContainer#addEndpoint(Class)} with MyServletContextListener.class (which is
 * also an endpoint). It will be picked up by scanning mechanism but ignored because MyApplication class filters it
 * out.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@WebListener
@ServerEndpoint("/annotated")
public class MyServletContextListenerAnnotated implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        final ServerContainer serverContainer = (ServerContainer) servletContextEvent.getServletContext().getAttribute(
                "jakarta.websocket.server.ServerContainer");

        try {
            // this is the important call
            serverContainer.addEndpoint(MyServletContextListenerAnnotated.class);
        } catch (DeploymentException e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public String onMessage(String message) {
        return message;
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
