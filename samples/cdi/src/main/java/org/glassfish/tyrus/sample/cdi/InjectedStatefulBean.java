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

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;

/**
 * @author Stepan Kopriva (stepan.kopriva at oracle.com)
 */
@Stateful
public class InjectedStatefulBean {

    private int counter = 0;
    private boolean postConstructCalled = false;


    public int getCounter() {
        return postConstructCalled ? counter : -1;
    }

    public void incrementCounter() {
        counter++;
    }

    @PostConstruct
    public void postConstruct() {
        postConstructCalled = true;
    }
}
