/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.simplelife;

import java.io.IOException;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;


@ServerEndpoint(value = "/simplelife")
public class SimpleLifeEndpoint {

    @OnOpen
    public void hi(Session remote) throws IOException {
        System.out.println("Someone connected...");
        remote.getBasicRemote().sendText("onOpen");
    }

    @OnMessage
    public void handleMessage(String message, Session session) {
        System.out.println("Someone sent me this message: " + message);
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void bye(Session remote) {
        System.out.println("Someone is disconnecting...");
    }

}
