/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.auction.decoders;

import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import org.glassfish.tyrus.sample.auction.message.AuctionMessage;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class AuctionMessageDecoder implements Decoder.Text<AuctionMessage> {

    @Override
    public AuctionMessage decode(String s) {
        String[] tokens = s.split(":");

        return new AuctionMessage(tokens[0], tokens[1], tokens[2]);
    }

    @Override
    public boolean willDecode(String s) {
        return s.startsWith(AuctionMessage.BID_REQUEST)
                || s.startsWith(AuctionMessage.AUCTION_LIST_REQUEST)
                || s.startsWith(AuctionMessage.LOGIN_REQUEST)
                || s.startsWith(AuctionMessage.LOGOUT_REQUEST);
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
