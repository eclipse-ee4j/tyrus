/*
 * Copyright (c) 2013, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.servlet.async;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.websocket.OnMessage;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * @author Stepan Kopriva
 */
@ServerEndpoint(value = "/binary-handler")
public class BinaryHandlerEndpoint {

    public static final AtomicInteger counter = new AtomicInteger(100);

    @OnMessage
    public void echo(ByteBuffer buf, Session session) throws Exception {
        session.getAsyncRemote().sendBinary(buf, new SendHandler() {
            @Override
            public void onResult(SendResult result) {
                counter.decrementAndGet();
            }
        });
    }
}
