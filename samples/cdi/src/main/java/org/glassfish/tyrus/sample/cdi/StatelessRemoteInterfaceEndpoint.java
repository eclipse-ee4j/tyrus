/*
 * Copyright (c) 2013, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.cdi;

import jakarta.websocket.OnMessage;
import jakarta.websocket.server.ServerEndpoint;

import jakarta.ejb.Stateless;

/**
 * @author Pavel Bucek
 */
@Stateless
@ServerEndpoint("/statelessRemoteInterfaceEndpoint")
public class StatelessRemoteInterfaceEndpoint implements RemoteService {

    @Override
    @OnMessage
    public String onMessage(String message) {
        return message;
    }
}
