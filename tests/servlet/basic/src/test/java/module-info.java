/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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

module org.glassfish.tyrus.tests.servlet.basic.test {
    requires jakarta.websocket;
    requires jakarta.websocket.client;

    requires junit;

    requires org.glassfish.tyrus.client;
    requires org.glassfish.tyrus.container.grizzly.client;
    requires org.glassfish.tyrus.container.grizzly.server;
    requires org.glassfish.tyrus.server;
    requires org.glassfish.tyrus.test.tools;
    requires org.glassfish.tyrus.tests.servlet.basic;

    exports org.glassfish.tyrus.tests.servlet.basic.test to junit;
}