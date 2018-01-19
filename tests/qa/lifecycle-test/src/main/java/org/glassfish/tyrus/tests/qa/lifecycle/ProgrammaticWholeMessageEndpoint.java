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

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.tests.qa.tools.SessionController;

abstract public class ProgrammaticWholeMessageEndpoint<T> extends Endpoint implements MessageHandler.Whole<T> {

    protected static final Logger logger = Logger.getLogger(ProgrammaticWholeMessageEndpoint.class.getCanonicalName());
    protected SessionLifeCycle lifeCycle;
    protected MessageHandler messageHandler;
    protected SessionController sc;
    protected Session session = null;

    public abstract void createLifeCycle();

    boolean isServerContainer(Session session) {
        logger.log(Level.INFO, "websocket.container:{0}", session.getContainer().toString());
        return session.getContainer() instanceof TyrusServerContainer;
    }

    @Override
    public void onMessage(T message) {

        logger.log(Level.INFO, "Programmatic.onMessage:{0}", message.toString());
        if (isServerContainer(session)) {
            logger.log(Level.INFO, "PRGEND:server:onMessage:{0}", message.toString());
            lifeCycle.onServerMessage(message, session);
        } else {
            logger.log(Level.INFO, "PRGEND:client:onMessage:{0}", message.toString());
            lifeCycle.onClientMessage(message, session);
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig ec) {
        if (this.session == null) {
            this.session = session;
        }
        logger.log(Level.INFO, "ProgrammaticEndpoint: onOpen");
        this.sc = new SessionController(session);
        createLifeCycle();
        lifeCycle.setSessionController(sc);
        session.addMessageHandler(this);
        if (isServerContainer(session)) {
            lifeCycle.onServerOpen(session, ec);
        } else {
            lifeCycle.onClientOpen(session, ec);
        }
    }

    @Override
    public void onClose(Session s, CloseReason reason) {
        if (isServerContainer(s)) {
            lifeCycle.onServerClose(s, reason);
        } else {
            lifeCycle.onClientClose(s, reason);
        }
    }

    @Override
    public void onError(Session s, Throwable thr) {
        if (isServerContainer(s)) {
            lifeCycle.onServerError(s, thr);
        } else {
            lifeCycle.onClientError(s, thr);
        }
    }

}
