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

package org.glassfish.tyrus.core;

import java.nio.ByteBuffer;

class Masker {
    private volatile ByteBuffer buffer;
    private volatile byte[] mask;
    private volatile int index = 0;

    public Masker(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public Masker(int mask) {
        this.mask = new byte[4];
        this.mask[0] = (byte) (mask >> 24);
        this.mask[1] = (byte) (mask >> 16);
        this.mask[2] = (byte) (mask >> 8);
        this.mask[3] = (byte) mask;
    }

    byte get() {
        return buffer.get();
    }

    byte[] get(final int size) {
        byte[] bytes = new byte[size];
        buffer.get(bytes);
        return bytes;
    }

    public byte[] unmask(int count) {
        byte[] bytes = get(count);
        if (mask != null) {
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] ^= mask[index++ % ProtocolHandler.MASK_SIZE];
            }
        }

        return bytes;
    }

    public void mask(byte[] target, int location, byte[] bytes, int length) {
        if (bytes != null && target != null) {
            for (int i = 0; i < length; i++) {
                target[location + i] = mask == null
                        ? bytes[i]
                        : (byte) (bytes[i] ^ mask[index++ % ProtocolHandler.MASK_SIZE]);
            }
        }
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public byte[] getMask() {
        return mask;
    }

    public void readMask() {
        mask = get(ProtocolHandler.MASK_SIZE);
    }
}
