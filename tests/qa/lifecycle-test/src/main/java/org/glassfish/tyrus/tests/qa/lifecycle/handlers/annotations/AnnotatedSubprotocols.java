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

package org.glassfish.tyrus.tests.qa.lifecycle.handlers.annotations;

import java.io.IOException;
import java.util.logging.Level;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.tests.qa.lifecycle.AnnotatedEndpoint;
import org.glassfish.tyrus.tests.qa.lifecycle.LifeCycleDeployment;
import org.glassfish.tyrus.tests.qa.lifecycle.handlers.StringSessionImpl;
import org.glassfish.tyrus.tests.qa.tools.SessionController;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class AnnotatedSubprotocols {

    @ServerEndpoint(value = LifeCycleDeployment.LIFECYCLE_ENDPOINT_PATH, subprotocols =
            {
                    "mikc21", "mikc22", "mikc23", "mikc24.111", "mikc25/0", "mikc26-0", "mikc27+0", "mikc28*9",
                    "mikc291", "mikc210", "mikc21", "mikc22", "mikc23", "mikc24.111", "mikc25/0", "mikc26-0",
                    "mikc27+0", "mikc28*9", "mikc291", "mikc210", "mikc21", "mikc22", "mikc23", "mikc24.111",
                    "mikc25/0", "mikc26-0", "mikc27+0", "mikc28*9", "mikc291", "mikc210", "mikc21", "mikc22", "mikc23",
                    "mikc24.111", "mikc25/0", "mikc26-0", "mikc27+0", "mikc28*9", "mikc291", "mikc210", "mikc21",
                    "mikc22", "mikc23", "mikc24.111", "mikc25/0", "mikc26-0", "mikc27+0", "mikc28*9", "mikc291",
                    "mikc10", "mikc10"
            })
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

    @ClientEndpoint(subprotocols =
            {
                    "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91",
                    "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9",
                    "mikc91", "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0",
                    "mikc8*9", "mikc91", "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0",
                    "mikc7+0", "mikc8*9", "mikc91", "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0",
                    "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111",
                    "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10", "mikc1", "mikc2", "mikc3",
                    "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10", "mikc1", "mikc2",
                    "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10", "mikc1",
                    "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
                    "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91",
                    "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9",
                    "mikc91", "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0",
                    "mikc8*9", "mikc91", "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0",
                    "mikc7+0", "mikc8*9", "mikc91", "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0",
                    "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111",
                    "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10", "mikc1", "mikc2", "mikc3",
                    "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10", "mikc1", "mikc2",
                    "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10", "mikc1",
                    "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
                    "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91",
                    "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9",
                    "mikc91", "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0",
                    "mikc8*9", "mikc91", "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0",
                    "mikc7+0", "mikc8*9", "mikc91", "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0",
                    "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10", "mikc1", "mikc2", "mikc3", "mikc4.111",
                    "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10", "mikc1", "mikc2", "mikc3",
                    "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10"
            }
    )
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
