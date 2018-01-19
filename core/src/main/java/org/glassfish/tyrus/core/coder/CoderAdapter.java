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

package org.glassfish.tyrus.core.coder;

import javax.websocket.EndpointConfig;

/**
 * Adapter for {@link javax.websocket.Encoder} and {@link javax.websocket.Decoder} which implements lifecycle
 * methods.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public abstract class CoderAdapter {

    /**
     * This method does nothing.
     *
     * @param config the endpoint configuration object when being brought into service.
     */
    public void init(EndpointConfig config) {
    }

    /**
     * This method does nothing.
     */
    public void destroy() {
    }
}
