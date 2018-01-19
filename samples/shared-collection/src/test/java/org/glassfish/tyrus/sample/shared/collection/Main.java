/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.tyrus.sample.shared.collection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.DeploymentException;

import org.glassfish.tyrus.server.Server;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class Main {

    public static void main(String[] args) throws DeploymentException, IOException {
        final Map<String, Object> serverProperties = new HashMap<String, Object>();
        serverProperties.put(Server.STATIC_CONTENT_ROOT, "./src/main/webapp");

        final Server server = new Server("localhost", 8025, "/sample-shared-collection", serverProperties,
                                         SharedCollectionEndpoint.class);
        server.start();

        System.out.println("Press any key to stop the server and quit..");
        //noinspection ResultOfMethodCallIgnored
        System.in.read();

        server.stop();
    }

}
