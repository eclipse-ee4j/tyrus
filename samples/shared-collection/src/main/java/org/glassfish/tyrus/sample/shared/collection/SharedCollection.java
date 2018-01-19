/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.shared.collection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.JsonObject;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
class SharedCollection {

    static final Map<String, String> map = new ConcurrentHashMap<String, String>();

    static {
        map.put("Red Leader", "Garven Dreis");
        map.put("Red Two", "Wedge Antilles");
        map.put("Red Three", "Biggs Darklighter");
        map.put("Red Four", "John D. Branon");
        map.put("Red Five", "Luke Skywalker");
    }

    static void broadcast(JsonObject object) {
        SharedCollectionEndpoint.broadcast(object);
        SharedCollectionResource.broadcast(object);
    }

}
