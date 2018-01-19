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

package org.glassfish.tyrus.core.cluster;

import java.io.Serializable;
import java.util.Map;

import javax.websocket.Session;

import org.glassfish.tyrus.core.TyrusSession;

/**
 * Extended {@link Session} which adds distributed properties.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public interface DistributedSession extends Session {

    /**
     * Get distributed properties.
     * <p>
     * Values put into this map must be {@link Serializable} or serializable by other, implementation-dependent
     * alternative.
     * <p>
     * Content of this map is synchronized among all cluster nodes, so putting an entry on any of the nodes will be
     * visible on all other nodes which have reference to current session (in form of {@link TyrusSession} or {@link
     * RemoteSession}).
     * <p>
     * Please note that when not running in the distributed environment, this map behaves similarly to {@link
     * #getUserProperties()}, so no serialization or deserialization is performed when values are read from or stored to
     * the returned map.
     *
     * @return map of distributed properties.
     * @see TyrusSession
     * @see RemoteSession
     */
    public Map<String, Object> getDistributedProperties();
}
