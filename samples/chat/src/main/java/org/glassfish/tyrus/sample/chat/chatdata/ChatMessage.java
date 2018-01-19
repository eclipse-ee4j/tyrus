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
 *   loginRequest: username (no commas)
 *   loginResponse: username / blank
 *   userList: comma sep: list of usernames
 *   chat transcript update: list of data
 *   disconnect request
 *   disconnect response
 */

import java.util.ArrayList;
import java.util.List;

public abstract class ChatMessage {
    public static final String LOGIN_REQUEST = "lreq";
    public static final String LOGIN_RESPONSE = "lres";
    public static final String USERLIST_UPDATE = "ulupd";
    public static final String CHAT_MESSAGE = "ctmsg";
    public static final String CHATTRANSCRIPT_UPDATE = "ctupd";
    public static final String DISCONNECT_REQUEST = "dreq";
    public static final String DISCONNECT_RESPONSE = "dres";
    public static final String SEP = ":";

    String type;

    public static void main(String args[]) {
        ChatMessage login = new LoginRequestMessage("Danny");

        String loginData = (String) login.asString();
        System.out.println(loginData);
        LoginRequestMessage parsedLogin = new LoginRequestMessage();
        parsedLogin.fromString(loginData);
        String parsedLoginData = (String) parsedLogin.getData();
        System.out.println(loginData + " : " + parsedLoginData);

        List users = new ArrayList();
        users.add("Danny");
        users.add("Jared");
        users.add("Tyrus");

        UserListUpdateMessage userListUpdate = new UserListUpdateMessage(users);
        System.out.println(userListUpdate.asString());
        UserListUpdateMessage parsedUserListUpdate = new UserListUpdateMessage();
        parsedUserListUpdate.fromString(userListUpdate.asString());
        System.out.println(parsedUserListUpdate.asString());

        List chatNameValue = new ArrayList();
        chatNameValue.add("Danny");
        chatNameValue.add("hi there");

        ChatUpdateMessage cm = new ChatUpdateMessage("Danny", "Hi There");
        System.out.println(cm.asString());
        ChatUpdateMessage parsedCM = new ChatUpdateMessage();
        parsedCM.fromString(cm.asString());
        System.out.println(parsedCM.asString());
    }


    public static ChatMessage parseMessage(String s) {
        System.out.println("Parse: " + s);
        ChatMessage chatMessage;

        if (s.startsWith(LOGIN_REQUEST)) {
            chatMessage = new LoginRequestMessage();
        } else if (s.startsWith(LOGIN_RESPONSE)) {
            chatMessage = new LoginResponseMessage();
        } else if (s.startsWith(DISCONNECT_REQUEST)) {
            chatMessage = new DisconnectRequestMessage();
        } else if (s.startsWith(DISCONNECT_RESPONSE)) {
            chatMessage = new DisconnectResponseMessage();
        } else if (s.startsWith(CHAT_MESSAGE)) {
            chatMessage = new ChatUpdateMessage();
        } else if (s.startsWith(USERLIST_UPDATE)) {
            chatMessage = new UserListUpdateMessage();
        } else if (s.startsWith(CHATTRANSCRIPT_UPDATE)) {
            chatMessage = new ChatTranscriptUpdateMessage();
        } else {
            throw new RuntimeException("Unknown message: " + s);
        }
        chatMessage.fromString(s);
        return chatMessage;
    }


    public abstract String asString();

    public abstract void fromString(String s);


    ChatMessage(String type) {
        this.type = type;
    }

    abstract Object getData();

    public String getType() {
        return type;
    }
}






