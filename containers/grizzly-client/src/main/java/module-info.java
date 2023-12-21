/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates. All rights reserved.
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
 * Tyrus Grizzly Client Container Module
 */

module org.glassfish.tyrus.container.grizzly.client {
    requires java.logging;
    requires jakarta.websocket;
    requires org.glassfish.grizzly;
    requires org.glassfish.grizzly.http;
    requires org.glassfish.tyrus.spi;
    requires org.glassfish.tyrus.client;
    requires org.glassfish.tyrus.core;

    exports org.glassfish.tyrus.container.grizzly.client;

    provides jakarta.websocket.ContainerProvider with org.glassfish.tyrus.container.grizzly.client.GrizzlyContainerProvider;
}