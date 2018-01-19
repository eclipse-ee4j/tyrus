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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import javax.websocket.DeploymentException;

import org.glassfish.tyrus.tests.qa.lifecycle.config.ProgrammaticWholeMessageStringSessionConfig;
import org.glassfish.tyrus.tests.qa.lifecycle.handlers.text.ProgrammaticWholeMessageStringSession;
import org.glassfish.tyrus.tests.qa.regression.Issue;

import org.junit.Test;

import junit.framework.Assert;

import org.glassfish.tyrus.tests.qa.tools.IssueManipulator;

/**
 * @author Michal Conos (michal.conos at oracle.com)
 */
public class URIMatchProgrammaticTest extends AbstractLifeCycleTestBase {
    @Test
    public void testURIMatchProgrammatic() throws DeploymentException, URISyntaxException, IOException {
        boolean exThrown = false;
        try {
            IssueManipulator.disableAll();
            lifeCycle(ProgrammaticWholeMessageStringSessionConfig.class, ProgrammaticWholeMessageStringSession.class,
                      Collections.EMPTY_SET, new URI("ws://localhost/aaaaa"), null);
        } catch (Exception ex) {
            exThrown = true;
        }

        Assert.assertEquals("URI don't match and Hnadshake  exception is not thrown", true, exThrown);
    }
}
