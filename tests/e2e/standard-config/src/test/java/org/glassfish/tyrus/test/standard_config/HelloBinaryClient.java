/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.test.standard_config;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

/**
 * @author Danny Coward (danny.coward at oracle.com)
 */
public class HelloBinaryClient extends Endpoint {
    volatile boolean echoWorked = false;
    static String MESSAGE = "hello";
    private final CountDownLatch messageLatch;

    public HelloBinaryClient(CountDownLatch messageLatch) {
        this.messageLatch = messageLatch;
    }

    @Override
    public void onOpen(Session session, EndpointConfig EndpointConfig) {
        System.out.println("HELLOBCLIENT opened !!");
        try {
            session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                @Override
                public void onMessage(ByteBuffer bb) {
                    System.out.println("HELLOBCLIENT received: " + new String(bb.array()));
                    echoWorked = (MESSAGE.equals(new String(bb.array())));
                    System.out.println(echoWorked);
                    messageLatch.countDown();
                }
            });
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(MESSAGE.getBytes()));
            System.out.println("## HELLOBCLIENT - message sent");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Session session, Throwable thr) {
        thr.printStackTrace();
    }
}
