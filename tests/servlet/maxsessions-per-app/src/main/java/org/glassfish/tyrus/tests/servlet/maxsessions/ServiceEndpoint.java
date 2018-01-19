/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tyrus.tests.servlet.maxsessions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

/**
 * Service endpoint to reset a tested endpoint.
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
@ServerEndpoint(value = ServiceEndpoint.SERVICE_ENDPOINT_PATH)
public class ServiceEndpoint {

    public static final String SERVICE_ENDPOINT_PATH = "/service";

    protected static final String POSITIVE = "POSITIVE";
    protected static final String NEGATIVE = "NEGATIVE";

    @OnMessage
    public String onMessage(String message) {

        if (message.startsWith("/echo")) {
            try {
                if (MaxSessionPerAppApplicationConfig.openLatch.await(1, TimeUnit.SECONDS)
                        && MaxSessionPerAppApplicationConfig.closeLatch.await(1, TimeUnit.SECONDS)) {
                    if (!EchoEndpoint.forbiddenClose.get()) {
                        return POSITIVE;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return NEGATIVE;
        } else if (message.equals("reset")) {
            reset();
            return POSITIVE;
        }

        return NEGATIVE;
    }

    private void reset() {
        EchoEndpoint.forbiddenClose.set(false);
        MaxSessionPerAppApplicationConfig.openLatch =
                new CountDownLatch(MaxSessionPerAppApplicationConfig.MAX_SESSIONS_PER_APP);
        MaxSessionPerAppApplicationConfig.closeLatch =
                new CountDownLatch(MaxSessionPerAppApplicationConfig.MAX_SESSIONS_PER_APP);
    }
}

