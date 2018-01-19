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
 * Binary frame representation.
 */
public class BinaryFrame extends TyrusFrame {

    private final boolean continuation;

    /**
     * Constructor.
     *
     * @param frame original (binary) frame.
     */
    public BinaryFrame(Frame frame) {
        super(frame, FrameType.BINARY);
        this.continuation = false;
    }

    /**
     * Constructor.
     *
     * @param frame        original (binary) frame.
     * @param continuation {@code true} when this frame is continuation frame, {@code false} otherwise.
     */
    public BinaryFrame(Frame frame, boolean continuation) {
        super(frame, continuation ? FrameType.BINARY_CONTINUATION : FrameType.BINARY);
        this.continuation = continuation;
    }

    /**
     * Constructor.
     *
     * @param payload      frame payload.
     * @param continuation {@code true} {@code true} when this frame is continuation frame, {@code false} otherwise.
     * @param fin          {@code true} when this frame is last in current partial message batch. Standard
     *                     (non-continuous) frames have this bit set to {@code true}.
     */
    public BinaryFrame(byte[] payload, boolean continuation, boolean fin) {
        super(Frame.builder().payloadData(payload).opcode(continuation ? (byte) 0x00 : (byte) 0x02).fin(fin).build(),
              continuation ? FrameType.BINARY_CONTINUATION : FrameType.BINARY);
        this.continuation = continuation;
    }

    @Override
    public void respond(TyrusWebSocket socket) {

        if (continuation) {
            socket.onFragment(this, isFin());
        } else {
            if (isFin()) {
                socket.onMessage(this);
            } else {
                socket.onFragment(this, false);
            }
        }
    }
}
