/*
 * Copyright (c) 2013, 2021 Oracle and/or its affiliates. All rights reserved.
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

import java.io.Reader;
import java.io.StringReader;

import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;

/**
 * Built in {@link Decoder} for {@link Reader}.
 *
 * @author Pavel Bucek
 */
public class ReaderDecoder extends CoderAdapter implements Decoder.Text<Reader> {

    @Override
    public boolean willDecode(String s) {
        return true;
    }

    @Override
    public Reader decode(String s) throws DecodeException {
        return new StringReader(s);
    }
}
