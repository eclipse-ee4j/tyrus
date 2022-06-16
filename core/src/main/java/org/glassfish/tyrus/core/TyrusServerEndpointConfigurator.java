/*
 * Copyright (c) 2013, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.core;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import jakarta.websocket.Extension;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

import org.glassfish.tyrus.core.collection.LazyValue;
import org.glassfish.tyrus.core.collection.Values;
import org.glassfish.tyrus.core.extension.ExtendedExtension;
import org.glassfish.tyrus.core.frame.Frame;

/**
 * Tyrus' implementation of {@link ServerEndpointConfig.Configurator}.
 *
 * @author Pavel Bucek
 */
public class TyrusServerEndpointConfigurator extends ServerEndpointConfig.Configurator {

    private LazyValue<ComponentProviderService> componentProviderService;

    public TyrusServerEndpointConfigurator() {
        this.componentProviderService = Values.lazy(() -> ComponentProviderService.create());
    }

    @Override
    public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
        if (requested != null) {
            for (String clientProtocol : requested) {
                if (supported.contains(clientProtocol)) {
                    return clientProtocol;
                }
            }
        }

        return "";
    }

    @Override
    public List<Extension> getNegotiatedExtensions(List<Extension> installed, List<Extension> requested) {
        installed = new ArrayList<Extension>(installed);

        List<Extension> result = new ArrayList<Extension>();

        if (requested != null) {
            for (final Extension requestedExtension : requested) {
                for (Extension extension : installed) {
                    final String name = extension.getName();

                    if (name != null && name.equals(requestedExtension.getName())) {

                        /*
                         * Per message compression - draft 19
                         * https://tools.ietf.org/html/draft-ietf-hybi-permessage-compression-19
                         *
                         * Extensions header can contain multiple declarations of the same extension with various
                         * parameters. The result have to contain only one result; depends on servers choice.
                         *
                         * {@see ExtendedExtension#onExtensionNegotiation(ExtendedExtension.ExtensionContext, List)}
                         */
                        boolean alreadyAdded = false;

                        for (Extension e : result) {
                            if (e.getName().equals(name)) {
                                alreadyAdded = true;
                            }
                        }

                        if (!alreadyAdded) {
                            if (extension instanceof ExtendedExtension) {
                                final ExtendedExtension extendedExtension = (ExtendedExtension) extension;
                                result.add(new ExtendedExtension() {
                                    @Override
                                    public Frame processIncoming(ExtensionContext context, Frame frame) {
                                        return extendedExtension.processIncoming(context, frame);
                                    }

                                    @Override
                                    public Frame processOutgoing(ExtensionContext context, Frame frame) {
                                        return extendedExtension.processOutgoing(context, frame);
                                    }

                                    /**
                                     * {@inheritDoc}
                                     * <p/>
                                     * Please note the TODO. {@link
                                     * ExtendedExtension#onExtensionNegotiation(ExtensionContext, List)}
                                     */
                                    @Override
                                    public List<Parameter> onExtensionNegotiation(ExtensionContext context,
                                                                                  List<Parameter> requestedParameters) {
                                        return extendedExtension
                                                .onExtensionNegotiation(context, requestedExtension.getParameters());
                                    }

                                    @Override
                                    public void onHandshakeResponse(ExtensionContext context,
                                                                    List<Parameter> responseParameters) {
                                        extendedExtension.onHandshakeResponse(context, responseParameters);
                                    }

                                    @Override
                                    public void destroy(ExtensionContext context) {
                                        extendedExtension.destroy(context);
                                    }

                                    @Override
                                    public String getName() {
                                        return name;
                                    }

                                    @Override
                                    public List<Parameter> getParameters() {
                                        return extendedExtension.getParameters();
                                    }
                                });
                            } else {
                                result.add(requestedExtension);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    public boolean checkOrigin(String originHeaderValue) {
        return true;
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        //noinspection unchecked
        return (T) componentProviderService.get().getEndpointInstance(endpointClass);
    }

    /**
     * Check whether the user defined {@link ServerEndpointConfig.Configurator} has overridden
     * {@link ServerEndpointConfig.Configurator#getEndpointInstance(Class)} method.
     * In that case, CDIProvider does not manage the instantiation.
     * @param configurator The user defined {@link ServerEndpointConfig.Configurator} subclass
     * @param <T> The subclass type
     * @return {@code true} iff the user creates the endpoint instance on their own.
     */
    static <T extends ServerEndpointConfig.Configurator> boolean overridesGetEndpointInstance(T configurator) {
        final String getInstanceName = "getEndpointInstance";
        if (null == configurator) {
            return false;
        }
        try {
            final Method originalMethod = TyrusServerEndpointConfigurator.class.isInstance(configurator)
                    ? TyrusServerEndpointConfigurator.class.getMethod(getInstanceName, Class.class)
                    : ServerEndpointConfig.Configurator.class.getMethod(getInstanceName, Class.class);
            final Method configMethod = configurator.getClass().getMethod(getInstanceName, Class.class);
            return !originalMethod.equals(configMethod);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
