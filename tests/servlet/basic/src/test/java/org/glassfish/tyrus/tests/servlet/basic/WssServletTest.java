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

package org.glassfish.tyrus.tests.servlet.basic;

import static org.junit.Assume.assumeTrue;

/**
 * Secured (using SSL) run of tests in {@link org.glassfish.tyrus.tests.servlet.basic.ServletTestBase}.
 * <p/>
 * The test will be run only if system property {@code tyrus.test.port.ssl} is set.
 *
 * @author Petr Janouch
 */
public class WssServletTest extends ServletTestBase {

    @Override
    protected int getPort() {
        String sslPort = System.getProperty("tyrus.test.port.ssl");
        // if sslPort is not set the test will be skipped
        assumeTrue(sslPort != null);

        try {
            return Integer.parseInt(sslPort);
        } catch (NumberFormatException nfe) {
            // do nothing
        }

        assumeTrue(false);
        // just to make javac happy - won't be executed.
        return 0;
    }

    @Override
    protected String getScheme() {
        return "wss";
    }
}
