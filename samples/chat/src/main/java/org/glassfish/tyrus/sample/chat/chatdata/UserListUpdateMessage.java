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


import java.util.List;
import java.util.Set;

public class UserListUpdateMessage extends ListMessage {

    public UserListUpdateMessage(Set usernames) {
        super(ChatMessage.USERLIST_UPDATE, usernames);
    }

    public UserListUpdateMessage() {
        super(ChatMessage.USERLIST_UPDATE);
    }

    public UserListUpdateMessage(List usernames) {
        super(ChatMessage.USERLIST_UPDATE, usernames);
    }

    public List getUserList() {
        return super.dataList;
    }

    @Override
    public void fromString(String s) {
        super.parseDataString(s.substring(USERLIST_UPDATE.length()));
    }

}
