/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
@ServerEndpoint(value = "/service")
public class ServiceEndpoint {

    public static final String BINARY_FUTURE = "BINARY FUTURE";
    public static final String BINARY_HANDLER = "BINARY HANDLER";
    public static final String OBJECT_FUTURE = "OBJECT FUTURE";
    public static final String OBJECT_HANDLER = "OBJECT HANDLER";
    public static final String TEXT_FUTURE = "TEXT FUTURE";
    public static final String TEXT_HANDLER = "TEXT HANDLER";

    @OnMessage
    public int echo(String message, Session session) throws Exception {

        if (message.equals(BINARY_FUTURE)) {
            return BinaryFutureEndpoint.counter.get();
        } else if (message.equals(BINARY_HANDLER)) {
            return BinaryHandlerEndpoint.counter.get();
        } else if (message.equals(OBJECT_FUTURE)) {
            return ObjectFutureEndpoint.counter.get();
        } else if (message.equals(OBJECT_HANDLER)) {
            return ObjectHandlerEndpoint.counter.get();
        } else if (message.equals(TEXT_FUTURE)) {
            return TextFutureEndpoint.counter.get();
        } else if (message.equals(TEXT_HANDLER)) {
            return TextHandlerEndpoint.counter.get();
        } else {
            return -1;
        }
    }
}

