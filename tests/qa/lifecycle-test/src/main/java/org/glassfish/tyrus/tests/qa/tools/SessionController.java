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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.Session;

import org.glassfish.tyrus.tests.qa.lifecycle.LifeCycleDeployment;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class SessionController {

    public static final Logger logger = Logger.getLogger(SessionController.class.getCanonicalName());

    public enum SessionState {

        START("null"),
        OPEN_SERVER("server.onOpen"),
        OPEN_CLIENT("client.onOpen"),
        MESSAGE("onMessage"),
        CLOSE_SERVER("server.onClose"),
        CLOSE_CLIENT("client.onClose"),
        ERROR_SERVER("server.onError"),
        ERROR_CLIENT("client.onError"),
        FINISHED_SERVER("server.finished");
        private String msg;

        SessionState(String msg) {
            this.msg = msg;
        }

        public String getMessage() {
            return msg;
        }

        @Override
        public String toString() {
            return msg;
        }
    }

    private final Session session;

    public SessionController(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    private static File getId() {
        //return session.getId();
        return new File(Misc.getTempDirectory(), "sessionState");
    }

    private static synchronized void changeState(SessionState expect, SessionState newState) {

        logger.log(Level.INFO, "changeState: {0} ---> {1}", new Object[]{expect, newState});


        String currentState = getState();
        logger.log(Level.INFO, "changeState: currState {0}", currentState);
        if (currentState.equals(expect.getMessage())) {
            logger.log(Level.INFO, "changeState: Switching to {0}", newState);
            setState(newState.getMessage());
        }


    }

    public static synchronized void resetState() {
        getId().delete();
    }

    public static synchronized void setState(String customState) {
        try {
            PrintWriter wr = new PrintWriter(getId());
            wr.println(customState);
            wr.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        logger.log(Level.INFO, "setState: {0}: {1}", new Object[]{getId(), customState});
        //client.setSessionStatus(getId(), customState);
    }

    public static synchronized String getState() {
        String state;
        try {
            BufferedReader br = new BufferedReader(new FileReader(getId()));
            state = br.readLine();

        } catch (Exception ex) {
            state = "null";
            //ex.printStackTrace();
            logger.log(Level.WARNING, "getState:", ex.getMessage());
        }
        
        /*
        try {
            state  = client.getSessionStatus(getId());
        } catch (URISyntaxException|JSONException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        logger.log(Level.INFO, "getState: {0}: {1}", new Object[] {getId(), state});
        */
        /*
        Map<String, Object> userProps = session.getUserProperties();
        String state = (String)userProps.get(getId());
        if(state==null) {
            state="null";
        }
        */
        logger.log(Level.INFO, "getState: {0}: {1}", new Object[]{getId(), state});
        return state;
    }

    public void serverOnOpen() {
        changeState(SessionState.START, SessionState.OPEN_SERVER);
        changeState(SessionState.OPEN_CLIENT, SessionState.OPEN_SERVER);
    }

    public void clientOnOpen() {
        changeState(SessionState.START, SessionState.OPEN_CLIENT);
        changeState(SessionState.OPEN_SERVER, SessionState.OPEN_CLIENT);
    }

    public void onMessage() {
        changeState(SessionState.OPEN_CLIENT, SessionState.MESSAGE);
        changeState(SessionState.OPEN_SERVER, SessionState.MESSAGE);
    }

    public void serverOnClose() {
        changeState(SessionState.MESSAGE, SessionState.CLOSE_SERVER);
    }

    public void serverOnError(Throwable t) {
        t.printStackTrace();
        changeState(SessionState.CLOSE_SERVER, SessionState.ERROR_SERVER);
    }

    public void serverOnFinish() {
        changeState(SessionState.ERROR_SERVER, SessionState.FINISHED_SERVER);
    }

    public boolean isFinished() {
        return getState().equals(SessionState.FINISHED_SERVER.getMessage());
    }
}
