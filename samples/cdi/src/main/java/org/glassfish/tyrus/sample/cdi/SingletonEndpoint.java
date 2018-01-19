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

import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.interceptor.Interceptors;


/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
@ServerEndpoint(value = "/singleton")
@Singleton
@Interceptors(LoggingInterceptor.class)
public class SingletonEndpoint {

    private final AtomicInteger counter = new AtomicInteger(0);

    private volatile boolean postConstructCalled = false;
    private volatile boolean interceptorCalled = false;

    @OnMessage
    public String echo(String message) {
        return (postConstructCalled && interceptorCalled)
                ? String.format("%s:%s", message, counter.incrementAndGet())
                : "PostConstruct not called.";
    }

    @PostConstruct
    public void postConstruct() {
        postConstructCalled = true;
    }

    public void onInterceptorCalled() {
        interceptorCalled = true;
    }
}
