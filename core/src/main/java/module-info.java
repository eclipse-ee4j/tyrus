/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates. All rights reserved.
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
 * Tyrus Core Module
 */
module org.glassfish.tyrus.core {
    requires java.logging;

    requires static jakarta.xml.bind;
    requires transitive jakarta.websocket;
    requires transitive jakarta.websocket.client;

    requires static org.osgi.core;

    requires org.glassfish.tyrus.spi;

    exports org.glassfish.tyrus.core;
    exports org.glassfish.tyrus.core.cluster;
    exports org.glassfish.tyrus.core.coder;
    exports org.glassfish.tyrus.core.extension;
    exports org.glassfish.tyrus.core.frame;
    exports org.glassfish.tyrus.core.l10n;
    exports org.glassfish.tyrus.core.monitoring;
    exports org.glassfish.tyrus.core.uri;
    exports org.glassfish.tyrus.core.uri.internal;
    exports org.glassfish.tyrus.core.wsadl.model;

    opens org.glassfish.tyrus.core.wsadl.model to jakarta.xml.bind;

    uses org.glassfish.tyrus.core.ComponentProvider;

    provides jakarta.websocket.server.ServerEndpointConfig.Configurator with org.glassfish.tyrus.core.TyrusServerEndpointConfigurator;
}