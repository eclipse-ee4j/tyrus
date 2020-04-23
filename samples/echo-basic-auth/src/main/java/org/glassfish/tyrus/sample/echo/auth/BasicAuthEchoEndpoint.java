/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.echo.auth;

import java.io.IOException;

import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
@ServerEndpoint("/basic-auth-echo")
public class BasicAuthEchoEndpoint {

    @OnOpen
    public void onOpen(Session session) throws IOException {
        session.getBasicRemote().sendText("onOpen");
    }

    @OnMessage
    public void echo(Session session, String message) throws IOException {
        session.getBasicRemote().sendText(message + " (from your server)");
        session.close();
    }

    @OnError
    public void onError(Throwable t) {
        t.printStackTrace();
    }
}
