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

import java.util.logging.Logger;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * Registered only for {@link SingletonEndpoint}.
 *
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
public class LoggingInterceptor {

    @AroundInvoke
    public Object manageTransaction(InvocationContext ctx) throws Exception {
        ((SingletonEndpoint) ctx.getTarget()).onInterceptorCalled();

        Logger.getLogger(getClass().getName()).info("LOGGING.");
        return ctx.proceed();
    }
}
