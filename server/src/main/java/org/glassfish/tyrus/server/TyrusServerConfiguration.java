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

package org.glassfish.tyrus.server;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.core.ErrorCollector;
import org.glassfish.tyrus.core.ReflectionHelper;

/**
 * Container for either deployed {@link ServerApplicationConfig}s, if any, or deployed classes.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class TyrusServerConfiguration implements ServerApplicationConfig {

    private static final Logger LOGGER = Logger.getLogger(TyrusServerConfiguration.class.getName());

    private final Set<ServerEndpointConfig> serverEndpointConfigs = new HashSet<ServerEndpointConfig>();
    private final Set<Class<?>> annotatedClasses = new HashSet<Class<?>>();

    /**
     * Create new {@link TyrusServerConfiguration}.
     *
     * @param classes               classes to be included in this application instance. Can contain any combination of
     *                              annotated endpoints (see {@link ServerEndpoint}). Cannot be {@code null}.
     * @param serverEndpointConfigs List of instances of {@link ServerEndpointConfig} to be deployed.
     * @throws IllegalArgumentException when any of the arguments is {@code null}.
     */
    public TyrusServerConfiguration(Set<Class<?>> classes, Set<ServerEndpointConfig> serverEndpointConfigs) {
        this(classes, Collections.<Class<?>>emptySet(), serverEndpointConfigs, new ErrorCollector());
    }

    /**
     * Create new {@link TyrusServerConfiguration}.
     *
     * @param classes                 classes to be included in this application instance. Can contain any combination
     *                                of annotated endpoints (see {@link ServerEndpoint}).
     * @param dynamicallyAddedClasses dynamically deployed classes. See {@link javax.websocket.server
     *                                .ServerContainer#addEndpoint(Class)}.
     * @param serverEndpointConfigs   List of instances of {@link ServerEndpointConfig} to be deployed.
     * @param errorCollector          model errors are reported to this instance. Cannot be {@code null}.
     * @throws IllegalArgumentException when any of the arguments is {@code null}.
     */
    public TyrusServerConfiguration(Set<Class<?>> classes, Set<Class<?>> dynamicallyAddedClasses,
                                    Set<ServerEndpointConfig> serverEndpointConfigs, ErrorCollector errorCollector) {
        if (classes == null || serverEndpointConfigs == null || errorCollector == null) {
            throw new IllegalArgumentException();
        }

        this.serverEndpointConfigs.addAll(serverEndpointConfigs);

        final Set<ServerApplicationConfig> configurations = new HashSet<ServerApplicationConfig>();
        final Set<Class<? extends Endpoint>> scannedProgramatics = new HashSet<Class<? extends Endpoint>>();
        final Set<Class<?>> scannedAnnotateds = new HashSet<Class<?>>();


        for (Iterator<Class<?>> it = classes.iterator(); it.hasNext(); ) {
            Class<?> cls = it.next();

            if (isAbstract(cls, errorCollector)) {
                it.remove();
                continue;
            }

            if (ServerApplicationConfig.class.isAssignableFrom(cls)) {
                ServerApplicationConfig config =
                        (ServerApplicationConfig) ReflectionHelper.getInstance(cls, errorCollector);
                configurations.add(config);
            }

            if (Endpoint.class.isAssignableFrom(cls)) {
                scannedProgramatics.add((Class<? extends Endpoint>) cls);
            }

            if (cls.isAnnotationPresent(ServerEndpoint.class)) {
                scannedAnnotateds.add(cls);
            }
        }

        // iterate through dynamically deployed classes
        // main difference compared to "scanning" deployment is that all classes will be deployed, no matter if
        // ServerApplicationConfig descendant is found.
        for (Class<?> c : dynamicallyAddedClasses) {

            // not ifaces nor abstract classes
            if (isAbstract(c, errorCollector)) {
                continue;
            }

            // or add any @ServerEndpoint annotated class
            if (c.isAnnotationPresent(ServerEndpoint.class)) {
                annotatedClasses.add(c);

            } else if (ServerApplicationConfig.class.isAssignableFrom(c)) {
                ServerApplicationConfig config =
                        (ServerApplicationConfig) ReflectionHelper.getInstance(c, errorCollector);
                configurations.add(config);

                // nothing else is expected/supported.
            } else {
                errorCollector.addException(new DeploymentException(String.format(
                        "Class %s is not ServerApplicationConfig descendant nor has @ServerEndpoint annotation.",
                        c.getName())));
            }
        }

        if (LOGGER.isLoggable(Level.CONFIG)) {
            StringBuilder logMessage = new StringBuilder();

            if (!configurations.isEmpty()) {
                logMessage.append("Found server application configs:\n");
            }

            for (ServerApplicationConfig serverApplicationConfig : configurations) {
                logMessage.append("\t").append(serverApplicationConfig.getClass().getName()).append("\n");
            }

            if (!scannedProgramatics.isEmpty()) {
                logMessage.append("Found programmatic endpoints:\n");
            }

            for (Class<? extends Endpoint> endpoint : scannedProgramatics) {
                logMessage.append("\t").append(endpoint.getName()).append("\n");
            }

            if (!scannedAnnotateds.isEmpty() || !annotatedClasses.isEmpty()) {
                logMessage.append("Found annotated endpoints:\n");
            }

            for (Class<?> endpoint : scannedAnnotateds) {
                logMessage.append("\t").append(endpoint.getName()).append("\n");
            }

            for (Class<?> endpoint : annotatedClasses) {
                logMessage.append("\t").append(endpoint.getName()).append("\n");
            }

            if (!logMessage.toString().equals("")) {
                LOGGER.config(logMessage.toString());
            }
        }

        if (!configurations.isEmpty()) {
            for (ServerApplicationConfig configuration : configurations) {
                Set<ServerEndpointConfig> programmatic = configuration.getEndpointConfigs(scannedProgramatics);
                programmatic = programmatic == null ? new HashSet<ServerEndpointConfig>() : programmatic;
                this.serverEndpointConfigs.addAll(programmatic);

                Set<Class<?>> annotated = configuration.getAnnotatedEndpointClasses(scannedAnnotateds);
                annotated = annotated == null ? new HashSet<Class<?>>() : annotated;
                annotatedClasses.addAll(annotated);
            }
        } else {
            annotatedClasses.addAll(scannedAnnotateds);
        }
    }

    private boolean isAbstract(Class<?> clazz, ErrorCollector errorCollector) {
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            LOGGER.log(Level.WARNING, String.format(
                    "%s: Deployed class can't be abstract nor interface. The class will not be deployed.",
                    clazz.getName()));
            return true;
        }

        return false;
    }

    /**
     * Gets all the {@link ServerEndpointConfig} classes which should be deployed.
     *
     * @param scanned is unused.
     * @return all the {@link ServerEndpointConfig} classes which should be deployed.
     */
    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> scanned) {
        return Collections.unmodifiableSet(serverEndpointConfigs);
    }

    /**
     * Gets all the classes annotated with {@link ServerEndpoint} annotation which should be deployed.
     *
     * @param scanned is unused.
     * @return all the classes annotated with {@link ServerEndpoint} annotation which should be deployed.
     */
    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
        return Collections.unmodifiableSet(annotatedClasses);
    }
}
