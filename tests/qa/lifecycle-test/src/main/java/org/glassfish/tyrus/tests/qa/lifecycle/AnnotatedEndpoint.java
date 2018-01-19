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

package org.glassfish.tyrus.tests.qa.lifecycle;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.glassfish.tyrus.tests.qa.tools.SessionController;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
abstract public class AnnotatedEndpoint {

    protected static final Logger logger = Logger.getLogger(AnnotatedEndpoint.class.getCanonicalName());
    protected SessionLifeCycle lifeCycle;
    protected SessionController sc;

    public abstract void createLifeCycle();

    protected Session session;

    public void onOpen(Session session, EndpointConfig ec) {
        if (this.session == null) {
            this.session = session;
        }
        logger.log(Level.INFO, "AnnotatedEndpoint: onOpen");
        this.sc = new SessionController(session);
        createLifeCycle();
        lifeCycle.setSessionController(sc);
    }
}
