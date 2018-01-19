/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.auction.encoders;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import org.glassfish.tyrus.sample.auction.message.AuctionMessage;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class AuctionMessageEncoder implements Encoder.Text<AuctionMessage> {

    @Override
    public String encode(AuctionMessage object) throws EncodeException {
        return object.toString();
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
