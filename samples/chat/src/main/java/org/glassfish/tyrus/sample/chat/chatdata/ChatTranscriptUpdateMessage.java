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

public class ChatTranscriptUpdateMessage extends ListMessage {

    public ChatTranscriptUpdateMessage(List transcript) {
        super(ChatMessage.CHATTRANSCRIPT_UPDATE, transcript);
    }

    public ChatTranscriptUpdateMessage() {
        super(ChatMessage.CHATTRANSCRIPT_UPDATE);
    }

    public String getLastLine() {
        return (String) super.getData().get(0);
    }

    @Override
    public void fromString(String s) {
        super.parseDataString(s.substring(CHATTRANSCRIPT_UPDATE.length()));
    }
}
