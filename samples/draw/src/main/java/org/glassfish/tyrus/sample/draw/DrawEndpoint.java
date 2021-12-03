/*
 * Copyright (c) 2011, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.draw;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.websocket.EncodeException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Endpoint which broadcasts incoming events to all connected peers.
 *
 * @author Danny Coward
 * @author Pavel Bucek
 */
@ServerEndpoint(value = "/draw")
public class DrawEndpoint {

    private static Set<Session> peers = Collections.newSetFromMap(new ConcurrentHashMap<Session, Boolean>());

    @OnOpen
    public void onOpen(Session session) {
        peers.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        peers.remove(session);
    }

    @OnMessage
    public void shapeCreated(String message, Session client) throws IOException, EncodeException {
        for (Session otherSession : peers) {
            if (!otherSession.equals(client)) {
                otherSession.getBasicRemote().sendText(message);
            }
        }
    }

}
