/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.qa;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.DeploymentException;

import org.glassfish.tyrus.tests.qa.lifecycle.handlers.annotations.MaxMessageSizeOnServer;
import org.glassfish.tyrus.tests.qa.regression.Issue;
import org.glassfish.tyrus.tests.qa.tools.IssueManipulator;
import org.glassfish.tyrus.tests.qa.tools.SessionController;

import org.junit.Test;

/**
 * @author Michal Conos (michal.conos at oracle.com)
 */
public class MaxMessageSizeOnServerTest extends AbstractLifeCycleTestBase {
    @Test
    public void testMaxMessageSizeOnServer() throws DeploymentException, IOException {
        IssueManipulator.disableAll();
        Set<String> possibleEndings = new HashSet<String>();
        possibleEndings.add(SessionController.SessionState.OPEN_CLIENT.getMessage());
        possibleEndings.add(SessionController.SessionState.OPEN_SERVER.getMessage());
        lifeCycle(MaxMessageSizeOnServer.Server.class, MaxMessageSizeOnServer.Client.class, possibleEndings,
                  testConf.getURI(), null);
    }
}
