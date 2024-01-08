/*
 * Copyright (c) 2022, 2024 Oracle and/or its affiliates. All rights reserved.
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

module tyrus.tests.e2e.nondeployable {
    requires com.sun.xml.bind.osgi;

    requires org.glassfish.grizzly;
    requires org.glassfish.grizzly.http;
    requires org.glassfish.grizzly.http.server;

    requires java.logging;

    requires jakarta.websocket;
    requires jakarta.websocket.client;

    requires junit;

    requires org.glassfish.tyrus.core;
    requires org.glassfish.tyrus.client;
    requires org.glassfish.tyrus.container.grizzly.client;
    requires org.glassfish.tyrus.container.grizzly.server;
    requires org.glassfish.tyrus.container.inmemory;
    requires org.glassfish.tyrus.container.jdk.client;
    requires org.glassfish.tyrus.server;
    requires org.glassfish.tyrus.spi;
    requires org.glassfish.tyrus.test.tools;

    exports org.glassfish.tyrus.test.e2e.non_deployable;
}