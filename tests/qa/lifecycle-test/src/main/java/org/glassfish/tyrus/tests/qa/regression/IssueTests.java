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

package org.glassfish.tyrus.tests.qa.regression;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Session;

import org.glassfish.tyrus.tests.qa.lifecycle.MyException;
import org.glassfish.tyrus.tests.qa.tools.IssueManipulator;

/**
 * @author michal.conos at oracle.com
 */
public class IssueTests {

    private static final Logger logger = Logger.getLogger(IssueTests.class.getCanonicalName());

    public static boolean checkTyrus93(Session s) {
        if (!IssueManipulator.isIssueEnabled(Issue.IssueId.TYRUS_93)) {
            return true; // disabled, ignore the test
        }

        try {
            logger.log(Level.INFO, "Tyrus-93: Client connecting:{0}", s.getRequestURI().toString());
        } catch (NullPointerException npe) {
            logger.log(Level.SEVERE, "Tyrus-93: NPE!");
            return false;
        }

        return true;
    }

    public static boolean checkTyrus94(Throwable thr) {
        if (!IssueManipulator.isIssueEnabled(Issue.IssueId.TYRUS_94)) {
            return true; // disabled, ignore the test
        }

        if (thr instanceof MyException) {
            return true;
        }

        logger.log(Level.INFO, String.format("Received %s, expected MyException.class", thr.getClass().getName()));
        return false;

    }

    public static boolean checkTyrus101(CloseReason reason) {
        if (!IssueManipulator.isIssueEnabled(Issue.IssueId.TYRUS_101)) {
            return true; // disabled, ignore the test
        }

        logger.log(Level.INFO, "TYRUS-101: reason={0}", reason);
        if (reason != null) {
            logger.log(Level.INFO, "TYRUS-101: reason.getCloseCode={0}", reason.getCloseCode());
        }
        return reason != null && reason.getCloseCode().equals(CloseReason.CloseCodes.GOING_AWAY);

    }

    public static boolean checkTyrus104(Session s) {
        if (!IssueManipulator.isIssueEnabled(Issue.IssueId.TYRUS_104)) {
            return true; // disabled, ignore the test
        }

        if (s.isOpen()) {
            logger.log(Level.SEVERE, "TYRUS-104: isOpen on a closed session must return false");
            return false; // isClosed
        }
        try {
            logger.log(Level.INFO, "TYRUS-104: send string on closed connection");
            s.getBasicRemote().sendText("Raise onError now - socket is closed");
            logger.log(Level.SEVERE, "TYRUS-104: IllegalStateException expected, should never get here");
            s.close();
        } catch (IOException ex) {
            return true;
        } catch (IllegalStateException ex) {
            return true;
        }
        return false;

    }
}
