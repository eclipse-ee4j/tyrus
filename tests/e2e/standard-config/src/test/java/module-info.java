/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Tyrus End-to-End Standard Config Tests Module
 */

module org.glassfish.tyrus.tests.e2e.stdconfig {
    requires java.logging;

    requires org.glassfish.grizzly;
    requires org.glassfish.grizzly.http;
    requires org.glassfish.grizzly.http.server;

    requires junit;

    requires jakarta.xml.bind;
    requires jakarta.json;
    requires jakarta.websocket;

    requires com.sun.xml.bind.osgi;

    requires org.glassfish.tyrus.client;
    requires org.glassfish.tyrus.core;
    requires org.glassfish.tyrus.container.grizzly.server;
    requires org.glassfish.tyrus.server;
    requires org.glassfish.tyrus.spi;

    requires org.glassfish.tyrus.test.tools;

    exports org.glassfish.tyrus.test.standard_config;
    exports org.glassfish.tyrus.test.standard_config.bean;
    exports org.glassfish.tyrus.test.standard_config.bean.stin;
    exports org.glassfish.tyrus.test.standard_config.message;
    exports org.glassfish.tyrus.test.standard_config.decoder;
    exports org.glassfish.tyrus.test.standard_config.userproperties;

    opens org.glassfish.tyrus.test.standard_config.bean to jakarta.xml.bind;
}