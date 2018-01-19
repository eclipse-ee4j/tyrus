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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import org.glassfish.tyrus.tests.qa.regression.Issue;
import org.glassfish.tyrus.tests.qa.regression.IssueTests;
import org.glassfish.tyrus.tests.qa.tools.IssueManipulator;
import org.glassfish.tyrus.tests.qa.tools.SessionController;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
abstract public class SessionLifeCycle<T> {

    public SessionLifeCycle(boolean partial) {
        this.partialMessageHandler = partial;
    }

    private boolean partialMessageHandler;
    private SessionController sc;
    protected static final Logger logger = Logger.getLogger(SessionLifeCycle.class.getCanonicalName());

    abstract public void onServerMessageHandler(T message, Session session) throws IOException;

    abstract public void onServerMessageHandler(T message, Session session, boolean last) throws IOException;

    abstract public void onClientMessageHandler(T message, Session session) throws IOException;

    abstract public void onClientMessageHandler(T message, Session session, boolean last) throws IOException;

    abstract public void startTalk(Session s) throws IOException;

    abstract public void startTalkPartial(Session s) throws IOException;

    public void setSessionController(SessionController sc) {
        this.sc = sc;
    }

    public void setPartialMessageHandler(boolean partial) {
        partialMessageHandler = partial;
    }

    public void onServerOpen(Session s, EndpointConfig config) {
        logger.log(Level.INFO, "Someone connected:{0}", s.getRequestURI().toString());
        sc.serverOnOpen();
    }

    public void onServerClose(Session s, CloseReason reason) {
        logger.log(Level.INFO, "Closing the session: {0}", s.toString());
        logger.log(Level.INFO, "Closing the session with reason: {0}", reason);

        if (!IssueTests.checkTyrus101(reason)) {
            sc.setState("server.TYRUS101");
        }

        if (!IssueTests.checkTyrus104(s)) {
            sc.setState("server.TYRUS104");
        }

        if (reason != null && reason.getCloseCode().equals(CloseReason.CloseCodes.GOING_AWAY) &&
                reason.getReasonPhrase() != null && reason.getReasonPhrase().equals("Going away")) {
            sc.serverOnClose();
        }
        throw new MyException("going onError");
    }

    private boolean checkError(Throwable thr) {
        // Programmatic Case
        if (thr instanceof RuntimeException && thr.getMessage() != null && "going onError".equals(thr.getMessage())) {
            return true;
        }
        // Annotated case - see TYRUS-94
        if (thr instanceof InvocationTargetException) {
            logger.log(Level.INFO, "TYRUS-94: should be runtime exception!");
            Throwable cause = thr.getCause();
            boolean res = cause instanceof RuntimeException && cause.getMessage() != null &&
                    "going onError".equals(cause.getMessage());
            logger.log(Level.INFO, "At least RuntimeException", thr);
            logger.log(Level.INFO, "RuntimeException.getMessage()=={0}", cause.getMessage());
            return res;
        }
        return false;
    }

    public void onServerError(Session s, Throwable thr) {
        logger.log(Level.INFO, "onServerError:", thr);

        if (checkError(thr)) {
            sc.serverOnError(thr);
            if (!IssueTests.checkTyrus94(thr)) {
                sc.setState("server.TYRUS_94");
            }
            sc.serverOnFinish();
        }
    }

    public void onServerMessage(T message, Session session) {
        logger.log(Level.INFO, "server:message={0}", message);
        sc.onMessage();
        try {
            //throw new RuntimeException();
            onServerMessageHandler(message, session);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, null, ex);
            sc.setState("on.server.message.exception");
        }
    }

    public void onServerMessage(T message, Session session, boolean last) {
        logger.log(Level.INFO, "server:message={0}", message);
        sc.onMessage();
        try {
            //throw new RuntimeException();
            onServerMessageHandler(message, session, last);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, null, ex);
            sc.setState("on.server.message.exception");
        }
    }

    public void onClientOpen(Session s, EndpointConfig config) {

        if (!IssueTests.checkTyrus93(s)) {
            sc.setState("TYRUS_93_FAIL");
        }
        sc.clientOnOpen();

        try {
            if (partialMessageHandler) {
                startTalkPartial(s);
            } else {
                startTalk(s);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    public void onClientClose(Session s, CloseReason reason) {
        logger.log(Level.INFO, "client: Closing the session: {0}", s.toString());
        //sc.clientOnClose();
        final RemoteEndpoint remote = s.getBasicRemote();
        try {
            s.getBasicRemote().sendText("client:onClose");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            sc.setState("on.client.close.exception");
        }
    }

    public void onClientError(Session s, Throwable thr) {
        logger.log(Level.SEVERE, "client: onError: {0}", thr.getMessage());
    }

    public void onClientMessage(T message, Session session) {
        sc.onMessage();
        logger.log(Level.INFO, "client:message={0}", message);
        try {
            onClientMessageHandler(message, session);
        } catch (IOException ex) {
            sc.setState("on.client.message.exception");
            ex.printStackTrace();
            logger.log(Level.SEVERE, null, ex);
        }
    }

    public void onClientMessage(T message, Session session, boolean last) {
        sc.onMessage();
        logger.log(Level.INFO, "client:message={0}", message);
        try {
            onClientMessageHandler(message, session, last);
        } catch (IOException ex) {
            sc.setState("on.client.partial.message.exception");
            ex.printStackTrace();
            logger.log(Level.SEVERE, null, ex);
        }
    }

    protected void closeTheSessionFromClient(Session session) throws IOException {
        logger.log(Level.INFO, "closing the session from the client");
        session.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Going away"));
    }
}
