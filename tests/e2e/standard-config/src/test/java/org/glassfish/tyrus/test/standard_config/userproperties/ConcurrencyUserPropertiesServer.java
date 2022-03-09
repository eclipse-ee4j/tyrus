/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.test.standard_config.userproperties;

import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ConcurrencyUserPropertiesServer extends Endpoint implements MessageHandler.Whole<String> {
    public static final String KEY = "CONCURRENCY_TEST_KEY";
    public static CountDownLatch session1Latch = new CountDownLatch(1);
    public static CountDownLatch session2Latch = new CountDownLatch(1);
    private Session session;

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        session.addMessageHandler(this);
        this.session = session;
    }

    @Override
    public void onMessage(String message) {
        String [] query = session.getQueryString().split("=");
        String queryId = query[1];

        String propertyId = (String) session.getUserProperties().get(KEY);
        try {
            session.getBasicRemote().sendText(queryId.equals(propertyId) ? "OK" : queryId + "=" + propertyId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
