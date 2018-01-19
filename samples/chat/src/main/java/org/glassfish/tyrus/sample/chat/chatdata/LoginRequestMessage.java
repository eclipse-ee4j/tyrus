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

public class LoginRequestMessage extends SimpleMessage {

    public LoginRequestMessage() {
        super(LOGIN_REQUEST, "");
    }

    public LoginRequestMessage(String username) {
        super(LOGIN_REQUEST, username);
    }

    public String getUsername() {
        return super.getData();
    }

}
