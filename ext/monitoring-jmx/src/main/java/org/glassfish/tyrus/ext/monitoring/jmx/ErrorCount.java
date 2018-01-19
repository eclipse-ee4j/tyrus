/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.ext.monitoring.jmx;

import java.beans.ConstructorProperties;
import java.io.Serializable;

import org.glassfish.tyrus.core.Beta;

/**
 * A pair of error class name and number of times the error occurred.
 *
 * @author Petr Janouch
 */
@Beta
public class ErrorCount implements Serializable {

    private static final long serialVersionUID = 7754787131158486198L;

    private final String throwableClassName;
    private final Long count;

    @ConstructorProperties({"throwableClassName", "count"})
    public ErrorCount(String throwableClassName, Long count) {
        this.throwableClassName = throwableClassName;
        this.count = count;
    }

    /**
     * Class name of the throwable that was raised.
     *
     * @return class name of the throwable that was raised.
     */
    public String getThrowableClassName() {
        return throwableClassName;
    }

    /**
     * The number of times the error has occurred.
     *
     * @return the number of times the error has occurred.
     */
    public Long getCount() {
        return count;
    }
}
