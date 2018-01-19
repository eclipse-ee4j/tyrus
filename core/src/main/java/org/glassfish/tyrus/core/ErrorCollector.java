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

package org.glassfish.tyrus.core;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.DeploymentException;

/**
 * Used to collect deployment errors to present these to the user together.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class ErrorCollector {

    private static final Logger LOGGER = Logger.getLogger(ErrorCollector.class.getName());

    private final List<Exception> exceptionsToPublish = new ArrayList<Exception>();

    /**
     * Add {@link Exception} to the collector.
     *
     * @param exception to be collected.
     */
    public void addException(Exception exception) {
        LOGGER.log(Level.FINE, "Adding exception", exception);
        exceptionsToPublish.add(exception);
    }

    /**
     * Create {@link DeploymentException} with message concatenated from collected exceptions.
     *
     * @return comprehensive exception.
     */
    public DeploymentException composeComprehensiveException() {
        StringBuilder sb = new StringBuilder();

        for (Exception exception : exceptionsToPublish) {
            sb.append(exception.getMessage());
            sb.append("\n");
        }

        return new DeploymentException(sb.toString());
    }

    /**
     * Checks whether any exception has been logged.
     *
     * @return {@code true} iff no exception was logged, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return exceptionsToPublish.isEmpty();
    }
}
