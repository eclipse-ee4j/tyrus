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

import java.nio.ByteBuffer;
import java.util.Locale;

import org.glassfish.tyrus.core.ProtocolException;
import org.glassfish.tyrus.core.TyrusWebSocket;

/**
 * Frame representation used in Tyrus runtime.
 * <p>
 * Enriched {@link Frame} representation.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public abstract class TyrusFrame extends Frame {

    private FrameType frameType;

    /**
     * Constructor.
     *
     * @param frame     enriched frame.
     * @param frameType type of the frame.
     */
    protected TyrusFrame(Frame frame, FrameType frameType) {
        super(frame);
        this.frameType = frameType;
    }

    /**
     * Execution part of frame processing.
     *
     * @param socket socket on which the appropriate action will be performed.
     */
    public abstract void respond(TyrusWebSocket socket);

    /**
     * Returns the type of the frame. It is used for distinguishing frame types in monitoring.
     * <p>
     * TODO: consider moving this to "MonitoredFrame" or something like this;
     *
     * @return type of the frame.
     */
    public FrameType getFrameType() {
        return frameType;
    }

    /**
     * {@link TyrusFrame} factory method.
     *
     * @param frame            original plain frame.
     * @param inFragmentedType type of fragment (text or binary).
     * @param remainder        decoding remainder. Used only for partial text frames.
     * @return new TyrusFrame.
     */
    public static TyrusFrame wrap(Frame frame, byte inFragmentedType, ByteBuffer remainder) {

        switch (frame.getOpcode()) {
            case 0x00:
                if ((inFragmentedType & 0x01) == 0x01) {
                    return new TextFrame(frame, remainder, true);
                } else {
                    return new BinaryFrame(frame, true);
                }
            case 0x01:
                return new TextFrame(frame, remainder);
            case 0x02:
                return new BinaryFrame(frame);
            case 0x08:
                return new CloseFrame(frame);
            case 0x09:
                return new PingFrame(frame);
            case 0x0A:
                return new PongFrame(frame);
            default:
                throw new ProtocolException(String.format("Unknown wrappedFrame type: %s",
                                                          Integer.toHexString(frame.getOpcode())
                                                                 .toUpperCase(Locale.US)));
        }
    }

    /**
     * An Enumeration of frame types.
     */
    public static enum FrameType {
        /**
         * Text frame.
         *
         * @see org.glassfish.tyrus.core.frame.TextFrame
         */
        TEXT,
        /**
         * Continuation text frame.
         *
         * @see org.glassfish.tyrus.core.frame.TextFrame
         */
        TEXT_CONTINUATION,
        /**
         * Binary frame.
         *
         * @see org.glassfish.tyrus.core.frame.BinaryFrame
         */
        BINARY,
        /**
         * Continuation binary frame.
         *
         * @see org.glassfish.tyrus.core.frame.BinaryFrame
         */
        BINARY_CONTINUATION,
        /**
         * Ping frame.
         *
         * @see org.glassfish.tyrus.core.frame.PingFrame
         */
        PING,
        /**
         * Pong frame.
         *
         * @see org.glassfish.tyrus.core.frame.PongFrame
         */
        PONG,
        /**
         * Close frame.
         *
         * @see org.glassfish.tyrus.core.frame.CloseFrame
         */
        CLOSE
    }
}
