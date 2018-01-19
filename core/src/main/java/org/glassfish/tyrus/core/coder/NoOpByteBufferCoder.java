/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.core.coder;

import java.nio.ByteBuffer;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;

/**
 * {@link Encoder} and {@link Decoder} implementation for {@link ByteBuffer}.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class NoOpByteBufferCoder extends CoderAdapter
        implements Decoder.Binary<ByteBuffer>, Encoder.Binary<ByteBuffer> {
    @Override
    public boolean willDecode(ByteBuffer bytes) {
        return true;
    }

    @Override
    public ByteBuffer decode(ByteBuffer bytes) throws DecodeException {
        return bytes;
    }

    @Override
    public ByteBuffer encode(ByteBuffer object) throws EncodeException {
        return object;
    }
}
