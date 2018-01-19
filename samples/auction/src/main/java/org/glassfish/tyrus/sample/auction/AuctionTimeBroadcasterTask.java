/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.auction;

import java.io.IOException;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.Session;

import org.glassfish.tyrus.sample.auction.message.AuctionMessage;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class AuctionTimeBroadcasterTask extends TimerTask {

    private Auction owner;
    private int timeoutCounter;

    public AuctionTimeBroadcasterTask(Auction owner, int timeoutCounter) {
        this.owner = owner;
        this.timeoutCounter = timeoutCounter;
    }

    @Override
    public void run() {
        if (timeoutCounter < 0) {
            owner.switchStateToAuctionFinished();
        } else {
            if (!owner.getRemoteClients().isEmpty()) {
                AuctionMessage.AuctionTimeBroadcastMessage atbm =
                        new AuctionMessage.AuctionTimeBroadcastMessage(owner.getId(), timeoutCounter);

                for (Session arc : owner.getRemoteClients()) {
                    try {
                        arc.getBasicRemote().sendText(atbm.toString());
                    } catch (IOException ex) {
                        Logger.getLogger(AuctionTimeBroadcasterTask.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        timeoutCounter--;
    }
}
