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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.tests.qa.lifecycle.SessionLifeCycle;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class CustomConfiguratorProtocols extends ServerEndpointConfig.Configurator {

    protected static final Logger logger = Logger.getLogger(SessionLifeCycle.class.getCanonicalName());


    @Override
    public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
        return "mikc10";
    }

    @Override
    public List<Extension> getNegotiatedExtensions(List<Extension> installed, List<Extension> requested) {
        return MyExtension.initExtensions();
    }

    @Override
    public boolean checkOrigin(String originHeaderValue) {
        logger.log(Level.INFO, "Orogon:{0}", originHeaderValue);
        return true;
    }
}
