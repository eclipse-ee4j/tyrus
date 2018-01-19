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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import org.glassfish.tyrus.tests.qa.regression.Issue;

/**
 * @author michal.conos at oracle.com
 */
public class IssueManipulator {

    private static final Map<Issue.IssueId, Issue> knownIssues = new EnumMap<Issue.IssueId, Issue>(Issue.IssueId.class);

    static {
        knownIssues.put(
                Issue.IssueId.TYRUS_93,
                new Issue(Issue.IssueId.TYRUS_93, "ClientEndpoint session.getRequestURI()==null"));
        knownIssues.put(
                Issue.IssueId.TYRUS_94,
                new Issue(Issue.IssueId.TYRUS_94, "ServerEndPoint: onError(): throwable.getCause()==null"));
        knownIssues.put(
                Issue.IssueId.TYRUS_101, new Issue(Issue.IssueId.TYRUS_101,
                                                   "CloseReason not propagated to server side (when close() " +
                                                           "initiated from client)"));
        knownIssues.put(
                Issue.IssueId.TYRUS_104, new Issue(Issue.IssueId.TYRUS_104,
                                                   "session should raise IllegalStateException when Session" +
                                                           ".getRemote() called on a closed session"));
    }

    private static final Logger logger = Logger.getLogger(IssueManipulator.class.getCanonicalName());

    private IssueManipulator() {
    }

    public static Issue getIssueById(Issue.IssueId id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (knownIssues.containsKey(id)) {
            return knownIssues.get(id);
        }
        throw new RuntimeException(String.format("Cannot find the issue: %s!", id));

    }

    public static boolean isIssueEnabled(Issue.IssueId id) {
        Issue loadedIssue = loadIssue(getIssueById(id));
        return loadedIssue.isEnabled();
    }

    private static String getIssueHolder(Issue issue) {
        return Misc.separatorsToUnix(Misc.getTempDirectoryPath() + "/" + issue.getIssueId().toString());
    }

    public static void saveIssue(Issue issue) {
        if (issue == null) {
            throw new IllegalArgumentException("issue is null!");
        }
        logger.log(Level.FINE,
                   String.format("saveIssue(): Saving issue:%s [%s]", getIssueHolder(issue), issue.isEnabled()));
        SerializationToolkit stool = new SerializationToolkit(getIssueHolder(issue));
        stool.save(issue);
    }

    public static Issue loadIssue(Issue issue) {
        if (issue == null) {
            throw new IllegalArgumentException("issue is null!");
        }
        SerializationToolkit stool = new SerializationToolkit(getIssueHolder(issue));
        Object obj = stool.load();
        if (obj != null && obj instanceof Issue) {
            Issue retrivedIssue = (Issue) obj;
            logger.log(Level.FINE, String.format("loadIssue(): Loading issue:%s [%s]", getIssueHolder(issue),
                                                 retrivedIssue.isEnabled()));
            return retrivedIssue;
        }

        throw new RuntimeException("loaded object is not Issue");
    }

    /**
     * Disable all issue but the on requested. Handy for regression testing
     */
    public static void disableAllButThisOne(Issue issue) {
        disableAll();
        issue.enable();
        saveIssue(issue);
    }

    /**
     * Disable all issue but the on requested. Handy for regression testing
     */
    public static void disableAllButThisOne(Issue.IssueId id) {
        disableAll();
        Issue issue = getIssueById(id);
        issue.enable();
        saveIssue(issue);
    }

    /**
     * Enable All issues in the database
     */
    public static void enableAll() {
        for (Issue crno : knownIssues.values()) {
            crno.enable();
            saveIssue(crno);
        }
    }

    /**
     * Disable all issue in the database
     */
    public static void disableAll() {
        for (Issue crno : knownIssues.values()) {
            crno.disable();
            saveIssue(crno);
        }
    }

}
