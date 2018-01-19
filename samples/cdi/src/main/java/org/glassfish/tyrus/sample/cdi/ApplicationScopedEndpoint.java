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

import javax.inject.Inject;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
@ServerEndpoint("/injectingappscoped")
public class ApplicationScopedEndpoint {

    @Inject
    InjectedApplicationScoped bean;

    @OnMessage
    public String echo(String message) {
        return message + bean.getText();
    }
}
