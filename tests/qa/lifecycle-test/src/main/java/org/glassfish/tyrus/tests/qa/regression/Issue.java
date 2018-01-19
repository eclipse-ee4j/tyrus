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

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Session;

import org.glassfish.tyrus.tests.qa.lifecycle.MyException;
import org.glassfish.tyrus.tests.qa.tools.Misc;
import org.glassfish.tyrus.tests.qa.tools.SerializationToolkit;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class Issue implements java.io.Serializable {

    public enum IssueId {TYRUS_101, TYRUS_104, TYRUS_94, TYRUS_93}

    private static final Logger logger = Logger.getLogger(Issue.class.getCanonicalName());
    private IssueId id;
    private String description;
    private boolean enabled;

    private void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * is the issue enabled?
     *
     * @return true if enabled, false if the issue is disabled
     */
    public boolean isEnabled() {
        logger.log(Level.INFO, "Issue {0} enabled", this);
        return enabled;
    }

    /**
     * Disable issue
     */
    public void disable() {
        setEnabled(false);
    }

    /**
     * Enable issue
     */
    public void enable() {
        setEnabled(true);
    }

    /**
     * Issue is created with a description
     *
     * @param description issue description
     */
    public Issue(IssueId id, String description) {
        this.id = id;
        this.description = description;
        this.enabled = true;
    }

    public IssueId getIssueId() {
        return id;
    }

}
