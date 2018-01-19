/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.qa.lifecycle.handlers;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.glassfish.tyrus.tests.qa.lifecycle.SessionConversation;
import org.glassfish.tyrus.tests.qa.lifecycle.SessionLifeCycle;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public class EmptySessionImpl extends SessionLifeCycle<String> implements SessionConversation {

    public EmptySessionImpl() {
        super(false);
    }

    @Override
    public void onServerMessageHandler(String message, Session session) throws IOException {

    }

    @Override
    public void onClientMessageHandler(String message, Session session) throws IOException {

    }

    @Override
    public void startTalk(Session s) throws IOException {

    }

    @Override
    public SessionLifeCycle getSessionConversation(boolean partial) {
        return new EmptySessionImpl();
    }

    @Override
    public void onServerError(Session s, Throwable thr) {
    }

    @Override
    public void onServerClose(Session s, CloseReason reason) {

    }

    @Override
    public void onServerOpen(Session s, EndpointConfig config) {

    }

    @Override
    public void onClientError(Session s, Throwable thr) {
    }

    @Override
    public void onClientClose(Session s, CloseReason reason) {

    }

    @Override
    public void onClientOpen(Session s, EndpointConfig config) {

    }

    @Override
    public void onServerMessageHandler(String message, Session session, boolean last) throws IOException {
    }

    @Override
    public void onClientMessageHandler(String message, Session session, boolean last) throws IOException {
    }

    @Override
    public void startTalkPartial(Session s) throws IOException {
    }
}
