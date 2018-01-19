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

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public interface LifeCycleDeployment {
    public static final String INSTALL_ROOT = System.getenv("HOME") + "/glassfish4/glassfish";
    public static final String CONTEXT_PATH = "/websockets-lifecycle-test";
    public static final String LIFECYCLE_ENDPOINT_PATH = "/life";
    public static final int COMMCHANNEL_PORT = 9339;
    public static final String COMMCHANNEL_SCHEME = "http";
    public static final String COMMCHANNEL_HOST = "localhost";
    public static final long DEBUG_TIMEOUT = 6000;
    public static final long NORMAL_TIMEOUT = 5;
    public static final String[] serverProtoOrder = {
            "mikc21", "mikc22", "mikc23", "mikc24.111", "mikc25/0", "mikc26-0", "mikc27+0", "mikc28*9", "mikc291",
            "mikc210", "mikc21", "mikc22", "mikc23", "mikc24.111", "mikc25/0", "mikc26-0", "mikc27+0", "mikc28*9",
            "mikc291", "mikc210", "mikc21", "mikc22", "mikc23", "mikc24.111", "mikc25/0", "mikc26-0", "mikc27+0",
            "mikc28*9", "mikc291", "mikc210", "mikc21", "mikc22", "mikc23", "mikc24.111", "mikc25/0", "mikc26-0",
            "mikc27+0", "mikc28*9", "mikc291", "mikc210", "mikc21", "mikc22", "mikc23", "mikc24.111", "mikc25/0",
            "mikc26-0", "mikc27+0", "mikc28*9", "mikc291", "mikc10",
    };
    public static final String[] clientProtoOrder = {
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10",
            "mikc1", "mikc2", "mikc3", "mikc4.111", "mikc5/0", "mikc6-0", "mikc7+0", "mikc8*9", "mikc91", "mikc10"
    };
}
