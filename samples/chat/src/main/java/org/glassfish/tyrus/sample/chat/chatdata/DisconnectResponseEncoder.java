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

import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class DisconnectResponseEncoder implements Encoder.Text<DisconnectResponseMessage> {

    @Override
    public String encode(DisconnectResponseMessage data) {
        DisconnectResponseMessage drm = (DisconnectResponseMessage) data;
        return drm.asString();
    }

    @Override
    public void init(EndpointConfig config) {
        // do nothing.
    }

    @Override
    public void destroy() {
        // do nothing.
    }
}
