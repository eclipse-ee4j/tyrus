/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.cdi;

import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
@ServerEndpoint(value = "/stateful")
@Stateful
public class StatefulEndpoint {

    int counter = 0;
    private boolean postConstructCalled = false;


    @OnMessage
    public String echo(String message) {
        return postConstructCalled ? String.format("%s:%s", message, counter++) : "PostConstruct not called.";
    }

    @PostConstruct
    public void postConstruct() {
        postConstructCalled = true;
    }
}
