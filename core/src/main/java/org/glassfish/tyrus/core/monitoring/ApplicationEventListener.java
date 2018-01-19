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

package org.glassfish.tyrus.core.monitoring;

import org.glassfish.tyrus.core.Beta;

/**
 * Listens to application-level events that are interesting for monitoring. Only one listener per application can be
 * registered.
 *
 * @author Petr Janouch
 */
@Beta
public interface ApplicationEventListener {

    /**
     * A key used for registering a application event listener implementation.
     * <p>
     * For monitoring in Grizzly server an instance should be passed to the server in server properties:
     * <pre>
     *     serverProperties.put(ApplicationEventListener.APPLICATION_EVENT_LISTENER, new MyApplicationEventListener());
     * </pre>
     * For use in servlet container the class name should be passed as a context parameter in web.xml:
     * <pre>{@code
     *     <context-param>
     *         <param-name>org.glassfish.tyrus.core.monitoring.ApplicationEventListener</param-name>
     *         <param-value>com.acme.MyApplicationEventListener</param-value>
     *     </context-param>}</pre>
     */
    public static final String APPLICATION_EVENT_LISTENER =
            "org.glassfish.tyrus.core.monitoring.ApplicationEventListener";

    /**
     * Called when the application has been initialized.
     *
     * @param applicationName name of the initialized application.
     */
    void onApplicationInitialized(String applicationName);

    /**
     * Called when the application has been destroyed.
     */
    void onApplicationDestroyed();

    /**
     * Called when an endpoint has been registered.
     *
     * @param endpointPath  the path the endpoint has been registered on.
     * @param endpointClass class of the registered endpoint.
     * @return endpoint event listener for registered endpoint.
     */
    EndpointEventListener onEndpointRegistered(String endpointPath, Class<?> endpointClass);

    /**
     * Called when an endpoint has been unregistered.
     *
     * @param endpointPath the path the endpoint has been registered on.
     */
    void onEndpointUnregistered(String endpointPath);

    /**
     * An instance of @ApplicationEventListener that does not do anything.
     */
    public static final ApplicationEventListener NO_OP = new ApplicationEventListener() {

        @Override
        public void onApplicationInitialized(String applicationName) {
            // do nothing
        }

        @Override
        public void onApplicationDestroyed() {
            // do nothing
        }

        @Override
        public EndpointEventListener onEndpointRegistered(String endpointPath, Class<?> endpointClass) {
            return EndpointEventListener.NO_OP;
        }

        @Override
        public void onEndpointUnregistered(String endpointPath) {
            // do nothing
        }
    };
}
