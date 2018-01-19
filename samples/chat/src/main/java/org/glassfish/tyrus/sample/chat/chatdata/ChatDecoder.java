/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.chat.chatdata;

import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class ChatDecoder implements Decoder.Text<ChatMessage> {

    @Override
    public ChatMessage decode(String s) {
        return ChatMessage.parseMessage(s);
    }

    @Override
    public boolean willDecode(String s) {
        return s.startsWith(DisconnectRequestMessage.DISCONNECT_REQUEST)
                || s.startsWith(DisconnectRequestMessage.LOGIN_REQUEST)
                || s.startsWith(DisconnectRequestMessage.CHAT_MESSAGE);
    }

    @Override
    public void init(EndpointConfig config) {
        // do nothing.
    }

    @Override
    public void destroy() {
        // do nothing.
    }
}
