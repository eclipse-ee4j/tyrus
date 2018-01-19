/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.ext.monitoring.jmx;

import org.glassfish.tyrus.core.frame.TyrusFrame;
import org.glassfish.tyrus.core.monitoring.MessageEventListener;

/**
 * Determines the type of a received or sent frame.
 *
 * @author Petr Janouch
 */
class MessageEventListenerImpl implements MessageEventListener {

    private final MessageListener messageListener;

    MessageEventListenerImpl(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    @Override
    public void onFrameSent(TyrusFrame.FrameType frameType, long payloadLength) {
        if (frameType == TyrusFrame.FrameType.TEXT || frameType == TyrusFrame.FrameType.TEXT_CONTINUATION) {
            messageListener.onTextMessageSent(payloadLength);
        }
        if (frameType == TyrusFrame.FrameType.BINARY || frameType == TyrusFrame.FrameType.BINARY_CONTINUATION) {
            messageListener.onBinaryMessageSent(payloadLength);
        }
        if (frameType == TyrusFrame.FrameType.PING || frameType == TyrusFrame.FrameType.PONG) {
            messageListener.onControlMessageSent(payloadLength);
        }
    }

    @Override
    public void onFrameReceived(TyrusFrame.FrameType frameType, long payloadLength) {
        if (frameType == TyrusFrame.FrameType.TEXT || frameType == TyrusFrame.FrameType.TEXT_CONTINUATION) {
            messageListener.onTextMessageReceived(payloadLength);
        }
        if (frameType == TyrusFrame.FrameType.BINARY || frameType == TyrusFrame.FrameType.BINARY_CONTINUATION) {
            messageListener.onBinaryMessageReceived(payloadLength);
        }
        if (frameType == TyrusFrame.FrameType.PING || frameType == TyrusFrame.FrameType.PONG) {
            messageListener.onControlMessageReceived(payloadLength);
        }
    }
}
