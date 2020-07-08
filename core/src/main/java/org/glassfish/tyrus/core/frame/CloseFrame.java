/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import jakarta.websocket.CloseReason;

import org.glassfish.tyrus.core.CloseReasons;
import org.glassfish.tyrus.core.ProtocolException;
import org.glassfish.tyrus.core.StrictUtf8;
import org.glassfish.tyrus.core.TyrusWebSocket;
import org.glassfish.tyrus.core.Utf8DecodingException;
import org.glassfish.tyrus.core.Utils;

/**
 * Close frame representation.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class CloseFrame extends TyrusFrame {

    private final CloseReason closeReason;
    private static final byte[] EMPTY_BYTES = new byte[0];

    /**
     * Constructor.
     *
     * @param frame original (close) frame.
     */
    public CloseFrame(Frame frame) {
        super(frame, FrameType.CLOSE);

        int closeCode;
        String closeReasonString;
        final byte[] data = frame.getPayloadData();
        if (data.length < 2) {
            throw new ProtocolException(
                    "Closing wrappedFrame payload, if present, must be a minimum of 2 bytes in length") {

                private static final long serialVersionUID = -5720682492584668231L;

                // autobahn test suite, test 7.3.1
                @Override
                public CloseReason getCloseReason() {
                    if (data.length == 0) {
                        return CloseReasons.NORMAL_CLOSURE.getCloseReason();
                    } else {
                        return super.getCloseReason();
                    }

                }
            };
        } else {
            closeCode = (int) Utils.toLong(data, 0, 2);
            if (closeCode < 1000 || closeCode == 1004 || closeCode == 1005 || closeCode == 1006
                    || (closeCode > 1013 && closeCode < 3000) || closeCode > 4999) {
                throw new ProtocolException("Illegal status code: " + closeCode);
            }
            if (data.length > 2) {
                closeReasonString = utf8Decode(data);
            } else {
                closeReasonString = null;
            }
        }

        closeReason = new CloseReason(CloseReason.CloseCodes.getCloseCode(closeCode), closeReasonString);
    }

    /**
     * Constructor.
     *
     * @param closeReason close reason used to construct close frame.
     */
    public CloseFrame(CloseReason closeReason) {
        super(Frame.builder().fin(true).opcode((byte) 0x08)
                   .payloadData(getPayload(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase()))
                   .build(), FrameType.CLOSE);
        this.closeReason = closeReason;
    }

    /**
     * Get close reason.
     *
     * @return close reason.
     */
    public CloseReason getCloseReason() {
        return closeReason;
    }

    @Override
    public void respond(TyrusWebSocket socket) {
        socket.onClose(this);
        socket.close();
    }

    private String utf8Decode(byte[] data) {
        String reason;
        final ByteBuffer b = ByteBuffer.wrap(data, 2, data.length - 2);
        Charset charset = new StrictUtf8();
        final CharsetDecoder decoder = charset.newDecoder();
        int n = (int) (b.remaining() * decoder.averageCharsPerByte());
        CharBuffer cb = CharBuffer.allocate(n);
        while (true) {
            CoderResult result = decoder.decode(b, cb, true);
            if (result.isUnderflow()) {
                decoder.flush(cb);
                ((Buffer) cb).flip();
                reason = cb.toString();
                break;
            }
            if (result.isOverflow()) {
                CharBuffer tmp = CharBuffer.allocate(2 * n + 1);
                ((Buffer) cb).flip();
                tmp.put(cb);
                cb = tmp;
                continue;
            }
            if (result.isError() || result.isMalformed()) {
                throw new Utf8DecodingException();
            }
        }

        return reason;
    }

    private static byte[] getPayload(int closeCode, String closeReason) {
        if (closeCode == -1) {
            return EMPTY_BYTES;
        }

        final byte[] bytes = Utils.toArray(closeCode);
        final byte[] reasonBytes = closeReason == null ? EMPTY_BYTES : closeReason.getBytes(new StrictUtf8());
        final byte[] frameBytes = new byte[2 + reasonBytes.length];
        System.arraycopy(bytes, bytes.length - 2, frameBytes, 0, 2);
        System.arraycopy(reasonBytes, 0, frameBytes, 2, reasonBytes.length);

        return frameBytes;
    }
}
