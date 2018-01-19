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

/**
 * Path and class name of an endpoint exposed by JMX.
 * <p>
 * See {@link org.glassfish.tyrus.core.monitoring.ApplicationEventListener}.
 *
 * @author Petr Janouch
 */
public class EndpointClassNamePathPair implements Serializable {

    private static final long serialVersionUID = -4603130812747865355L;

    private final String endpointPath;
    private final String endpointClassName;

    /**
     * Constructor, {@link java.beans.ConstructorProperties} is required, so that MXBean client can create an instance.
     *
     * @param endpointPath      the path the endpoint is registered on.
     * @param endpointClassName the class name of the endpoint.
     */
    @ConstructorProperties({"endpointPath", "endpointClassName"})
    public EndpointClassNamePathPair(String endpointPath, String endpointClassName) {
        this.endpointPath = endpointPath;
        this.endpointClassName = endpointClassName;
    }

    /**
     * Get class name of the endpoint.
     *
     * @return class name of the endpoint.
     */
    public String getEndpointClassName() {
        return endpointClassName;
    }

    /**
     * Get the path the endpoint is registered on.
     *
     * @return the path the endpoint is registered on.
     */
    public String getEndpointPath() {
        return endpointPath;
    }
}
