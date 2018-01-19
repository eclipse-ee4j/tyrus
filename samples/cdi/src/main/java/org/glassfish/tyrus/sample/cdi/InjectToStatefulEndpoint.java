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
import javax.inject.Inject;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
@ServerEndpoint(value = "/injectingstateful")
public class InjectToStatefulEndpoint {

    public static final String TEXT = " Inner counter is:";
    private boolean postConstructCalled = false;

    @Inject
    InjectedStatefulBean bean;

    @OnMessage
    public String doThat(String message) {
        bean.incrementCounter();
        return postConstructCalled ? String.format("%s%s%s", message, TEXT, bean.getCounter()) : "PostConstruct not "
                + "called.";
    }

    @PostConstruct
    public void postConstruct() {
        postConstructCalled = true;
    }
}
