/*
 * Copyright (c) 2013, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.cdi;

import jakarta.websocket.OnMessage;
import jakarta.websocket.server.ServerEndpoint;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * Bean where {@link InjectedSimpleBean} is injected.
 *
 * @author Stepan Kopriva
 */
@ServerEndpoint(value = "/simple")
public class SimpleEndpoint {

    private boolean postConstructCalled = false;

    @Inject
    InjectedSimpleBean bean;

    @OnMessage
    public String echo(String message) {
        return postConstructCalled ? String.format("%s%s", message, bean.getText()) : "PostConstruct was not called";
    }

    @PostConstruct
    public void postConstruct() {
        postConstructCalled = true;
    }
}
