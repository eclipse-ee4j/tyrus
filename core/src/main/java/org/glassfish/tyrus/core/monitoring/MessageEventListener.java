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

package org.glassfish.tyrus.core.monitoring;

import org.glassfish.tyrus.core.Beta;
import org.glassfish.tyrus.core.frame.TyrusFrame;

/**
 * Listens for message-level events that are interesting for monitoring.
 *
 * @author Petr Janouch
 */
@Beta
public interface MessageEventListener {

    /**
     * Called when a frame has been sent.
     *
     * @param frameType     type of the frame.
     * @param payloadLength length of the frame payload.
     */
    void onFrameSent(TyrusFrame.FrameType frameType, long payloadLength);

    /**
     * Called when a frame has been received.
     *
     * @param frameType     type of the frame.
     * @param payloadLength length of the frame payload.
     */
    void onFrameReceived(TyrusFrame.FrameType frameType, long payloadLength);

    /**
     * An instance of @MessageEventListener that does not do anything.
     */
    public static final MessageEventListener NO_OP = new MessageEventListener() {


        @Override
        public void onFrameSent(TyrusFrame.FrameType frameType, long payloadLength) {
            //do nothing
        }

        @Override
        public void onFrameReceived(TyrusFrame.FrameType frameType, long payloadLength) {
            //do nothing
        }
    };
}
