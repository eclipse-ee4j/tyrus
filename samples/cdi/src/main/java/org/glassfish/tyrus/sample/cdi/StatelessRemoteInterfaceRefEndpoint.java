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

import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

import javax.ejb.Remote;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
@Remote(RemoteService.class)
@ServerEndpoint("/statelessRemoteInterfaceRefEndpoint")
public class StatelessRemoteInterfaceRefEndpoint {

    @OnMessage
    public String onMessage(String message) {
        return message;
    }
}
