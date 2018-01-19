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


abstract class SimpleMessage extends ChatMessage {
    String dataString;

    SimpleMessage(String type, String dataString) {
        super(type);
        this.dataString = dataString;
    }

    @Override
    public String asString() {
        return type + dataString;
    }

    @Override
    public void fromString(String s) {
        dataString = s.substring(type.length());
    }

    @Override
    String getData() {
        return dataString;
    }


}
