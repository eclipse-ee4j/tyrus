/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.chat.chatdata;


/**
 * @author Danny Coward (danny.coward at oracle.com)
 */
public class ChatUpdateMessage extends ListMessage {

    public ChatUpdateMessage(String username, String message) {
        super(ChatMessage.CHAT_MESSAGE, username, message);
    }

    public ChatUpdateMessage() {
        super(ChatMessage.CHAT_MESSAGE);
    }

    public String getUsername() {
        return (String) super.getData().get(0);
    }

    public String getMessage() {
        return (String) super.getData().get(1);
    }

    @Override
    public void fromString(String s) {
        super.parseDataString(s.substring(CHAT_MESSAGE.length()));
    }

}
