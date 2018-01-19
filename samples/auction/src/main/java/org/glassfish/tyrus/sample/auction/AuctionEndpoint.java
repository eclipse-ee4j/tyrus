/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.auction;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.sample.auction.decoders.AuctionMessageDecoder;
import org.glassfish.tyrus.sample.auction.encoders.AuctionMessageEncoder;
import org.glassfish.tyrus.sample.auction.message.AuctionMessage;

/**
 * Runs multiple auctions.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
@ServerEndpoint(
        value = "/auction",
        decoders = {AuctionMessageDecoder.class},
        encoders = {AuctionMessageEncoder.class})
public class AuctionEndpoint {

    /*
     * Set of auctions (finished, running, to be started auctions).
     */
    private static final Set<Auction> auctions = Collections.unmodifiableSet(new HashSet<Auction>() {
        {
            add(new Auction(new AuctionItem("Swatch", "Nice Swatch watches, hand made", 100, 20)));
            add(new Auction(new AuctionItem("Rolex", "Nice Rolex watches, hand made", 200, 20)));
            add(new Auction(new AuctionItem("Omega", "Nice Omega watches, hand made", 300, 20)));
        }
    });

    @OnClose
    public void handleClosedConnection(Session session) {
        for (Auction auction : auctions) {
            auction.removeArc(session);
        }
    }

    @OnMessage
    public void handleMessage(AuctionMessage message, Session session) {
        String communicationId;

        final String messageType = message.getType();

        if (messageType.equals(AuctionMessage.LOGOUT_REQUEST)) {
            handleClosedConnection(session);

        } else if (messageType.equals(AuctionMessage.AUCTION_LIST_REQUEST)) {
            StringBuilder sb = new StringBuilder("-");

            for (Auction auction : auctions) {
                sb.append(auction.getId()).append("-").append(auction.getItem().getName()).append("-");
            }

            try {
                session.getBasicRemote().sendObject(
                        new AuctionMessage.AuctionListResponseMessage("0", sb.toString()));
            } catch (Exception e) {
                Logger.getLogger(AuctionEndpoint.class.getName()).log(Level.SEVERE, null, e);
            }

        } else if (messageType.equals(AuctionMessage.LOGIN_REQUEST)) {
            communicationId = message.getCommunicationId();
            for (Auction auction : auctions) {
                if (communicationId.equals(auction.getId())) {
                    auction.handleLoginRequest(message, session);
                }
            }

        } else if (messageType.equals(AuctionMessage.BID_REQUEST)) {
            communicationId = message.getCommunicationId();
            for (Auction auction : auctions) {
                if (communicationId.equals(auction.getId())) {
                    auction.handleBidRequest(message, session);
                    break;
                }
            }
        }
    }
}
