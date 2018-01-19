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

package org.glassfish.tyrus.tests.qa.tools;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.DeploymentException;

import org.glassfish.tyrus.server.Server;
import org.glassfish.tyrus.tests.qa.config.AppConfig;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class TyrusToolkit implements ServerToolkit {
    private static final Logger logger = Logger.getLogger(TyrusToolkit.class.getCanonicalName());

    private AppConfig config;
    private Set<Class<?>> endpointClasses = new HashSet<Class<?>>();
    private Server server;


    public TyrusToolkit(AppConfig config) {
        this.config = config;
    }

    @Override
    public void registerEndpoint(Class<?> endpoint) {
        endpointClasses.add(endpoint);
    }

    /**
     * Start embedded server unless "tyrus.test.host" system property is specified.
     */
    @Override
    public void startServer() throws DeploymentException {
        final String host = System.getProperty("tyrus.test.host");
        if (host == null) {
            server = new Server(config.getHost(), config.getPort(), config.getContextPath(),
                                Collections.<String, Object>emptyMap(), endpointClasses);
            server.start();
            logger.log(Level.INFO, "Tyrus Server started at {0}:{1}", new Object[]{config.getHost(), config.getPort()});
        }
    }

    @Override
    public void stopServer() {
        if (server != null) {
            server.stop();
            logger.log(Level.INFO, "Tyrus Server stopped");
        }
    }
}
