/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.cdi;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import javax.ejb.Stateless;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class ApplicationConfig implements ServerApplicationConfig {
    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
        return new HashSet<ServerEndpointConfig>(Arrays.asList(
                ServerEndpointConfig.Builder
                        .create(ProgrammaticStatelessRemoteInterfaceEndpoint.class,
                                "/programmaticStatelessRemoteInterfaceEndpoint")
                        .build()));
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
        return scanned;
    }

    @Stateless
    public static class ProgrammaticStatelessRemoteInterfaceEndpoint extends Endpoint implements
            RemoteServiceProgrammatic {

        @Override
        public void onOpen(final Session session, EndpointConfig config) {
            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    try {
                        session.getBasicRemote().sendText(message);
                    } catch (IOException e) {
                        // do nothing.
                    }
                }
            });
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
        }

        @Override
        public void onError(Session session, Throwable thr) {
        }
    }
}
