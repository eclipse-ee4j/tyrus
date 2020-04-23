/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.qa.lifecycle.handlers.annotations;

import java.io.IOException;
import java.util.logging.Level;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.tests.qa.lifecycle.AnnotatedEndpoint;
import org.glassfish.tyrus.tests.qa.lifecycle.LifeCycleDeployment;
import org.glassfish.tyrus.tests.qa.lifecycle.config.CustomConfiguratorProtocols;
import org.glassfish.tyrus.tests.qa.lifecycle.handlers.StringSessionImpl;
import org.glassfish.tyrus.tests.qa.tools.SessionController;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class SubprotocolsViaCustomConfigurator {

    @ServerEndpoint(value = LifeCycleDeployment.LIFECYCLE_ENDPOINT_PATH,
            configurator = CustomConfiguratorProtocols.class)
    static public class Server extends AnnotatedEndpoint {

        @Override
        public void createLifeCycle() {
            lifeCycle = new StringSessionImpl(false);
        }

        private void checkSubProtocols(Session s) {
            logger.log(Level.INFO, "checkSubProtocols:{0}", s.getNegotiatedSubprotocol());

            if (!s.getNegotiatedSubprotocol().equals("mikc10")) {
                throw new RuntimeException("checkSubProtocols: bad subprotocol! Got:" + s.getNegotiatedSubprotocol());
            }


        }

        @OnOpen
        @Override
        public void onOpen(Session session, EndpointConfig ec) {
            super.onOpen(session, ec);
            lifeCycle.onServerOpen(session, ec);
            logger.log(Level.INFO, "lifeCycle={0}", lifeCycle.toString());
            logger.log(Level.INFO, "extendsion={0}", ((ServerEndpointConfig) ec).getExtensions());
            checkSubProtocols(session);
        }

        @OnMessage(maxMessageSize = -1)
        public void onMessage(String message, Session session) throws IOException {
            checkSubProtocols(session);
            lifeCycle.onServerMessage(message, session);
        }

        @OnClose
        public void onClose(Session s, CloseReason reason) {
            checkSubProtocols(s);
            lifeCycle.onServerClose(s, reason);
        }

        @OnError
        public void onError(Session s, Throwable thr) {
            checkSubProtocols(s);
            lifeCycle.onServerError(s, thr);
        }
    }

    @ClientEndpoint
    static public class Client extends AnnotatedEndpoint {

        @Override
        public void createLifeCycle() {
            lifeCycle = new StringSessionImpl(false);
        }

        @OnOpen
        public void onOpen(Session session, EndpointConfig ec) {
            if (this.session == null) {
                this.session = session;
            }
            logger.log(Level.INFO, "AnnotatedEndpoint.Client: onOpen");
            logger.log(Level.INFO, "Client can do:{0}", ((ClientEndpointConfig) ec).getPreferredSubprotocols());
            this.sc = new SessionController(session);
            createLifeCycle();
            lifeCycle.setSessionController(sc);
            lifeCycle.onClientOpen(session, ec);

        }

        @OnMessage
        public void onMessage(String message, Session session) throws IOException {
            lifeCycle.onClientMessage(message, session);
        }

        @OnClose
        public void onClose(Session s, CloseReason reason) {
            lifeCycle.onClientClose(s, reason);
        }

        @OnError
        public void onError(Session s, Throwable thr) {
            lifeCycle.onClientError(s, thr);
        }
    }
}
