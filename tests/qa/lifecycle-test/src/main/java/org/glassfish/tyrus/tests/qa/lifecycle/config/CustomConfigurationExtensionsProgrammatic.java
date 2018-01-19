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

package org.glassfish.tyrus.tests.qa.lifecycle.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.tests.qa.lifecycle.LifeCycleDeployment;
import org.glassfish.tyrus.tests.qa.lifecycle.ProgrammaticWholeMessageEndpoint;
import org.glassfish.tyrus.tests.qa.lifecycle.handlers.StringSessionImpl;
import org.glassfish.tyrus.tests.qa.tools.SessionController;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class CustomConfigurationExtensionsProgrammatic implements ServerApplicationConfig {

    static class StringLifeCycle extends ProgrammaticWholeMessageEndpoint<String>
            implements MessageHandler.Whole<String> {

        @Override
        public void createLifeCycle() {
            lifeCycle = new StringSessionImpl(false);
        }

        @Override
        public void onOpen(Session session, EndpointConfig ec) {
            if (this.session == null) {
                this.session = session;
            }
            logger.log(Level.INFO, "ProgrammaticEndpoint: onOpen");
            this.sc = new SessionController(session);
            createLifeCycle();

            logger.log(Level.INFO, "Get Extensions: {0}", ((ServerEndpointConfig) ec).getExtensions());
            logger.log(Level.INFO, "Get negotiated extensions: {0}", session.getNegotiatedExtensions());
            lifeCycle.setSessionController(sc);
            session.addMessageHandler(this);
            lifeCycle.onServerOpen(session, ec);
        }
    }

    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> set) {
        Set<ServerEndpointConfig> configSet = new HashSet<ServerEndpointConfig>();
        List<String> protocols = Arrays.asList(LifeCycleDeployment.serverProtoOrder);
        ServerEndpointConfig config = ServerEndpointConfig.Builder.create(
                CustomConfigurationProtocolsProgrammatic.StringLifeCycle.class,
                LifeCycleDeployment.LIFECYCLE_ENDPOINT_PATH).extensions(MyExtension.initExtensions()).build();
        configSet.add(config);
        return configSet;
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> set) {
        return Collections.EMPTY_SET;
    }

}
