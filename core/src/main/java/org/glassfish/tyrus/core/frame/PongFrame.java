/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.core.frame;

import org.glassfish.tyrus.core.TyrusWebSocket;

/**
 * Pong frame representation.
 */
public class PongFrame extends TyrusFrame {

    /**
     * Constructor.
     *
     * @param frame original (pong) frame.
     */
    public PongFrame(Frame frame) {
        super(frame, FrameType.PONG);
    }

    /**
     * Constructor.
     *
     * @param payload pong frame payload.
     */
    public PongFrame(byte[] payload) {
        super(Frame.builder().fin(true).opcode((byte) 0x0A).payloadData(payload).build(), FrameType.PONG);
    }

    @Override
    public void respond(TyrusWebSocket socket) {
        socket.onPong(this);
    }
}
